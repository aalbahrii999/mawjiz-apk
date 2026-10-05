package app.mawjiz

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RawItem(val title: String, val url: String, val outlet: String, val body: String, val at: Long)

class Net {
    private val http = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()
    private val fallbacks = listOf("gemini-2.5-flash-lite", "gemini-2.0-flash")

    fun gather(enabled: Map<String, Boolean>, sources: List<String>): List<Story> {
        val jobs = mutableListOf<Pair<String, String>>()
        for (outlet in Catalog.outlets) {
            if (outlet.id !in sources || outlet.feed == null) continue
            jobs += outlet.feed to outlet.label
        }
        for ((id, query) in Catalog.queries) {
            if (enabled[id] != true) continue
            jobs += google(query) to ""
        }
        for (outlet in Catalog.outlets) {
            if (outlet.id !in sources || outlet.feed != null) continue
            if (outlet.group != "global" && outlet.id != "spa") continue
            jobs += google(outlet.query) to outlet.label
        }
        for (outlet in Catalog.outlets) {
            if (outlet.id !in sources || outlet.feed != null) continue
            if (outlet.group == "global" || outlet.id == "spa") continue
            jobs += google(outlet.query) to outlet.label
        }
        val stories = mutableListOf<Story>()
        for (job in jobs.distinctBy { it.first }.take(12)) {
            val xml = get(job.first) ?: continue
            for (item in parse(xml, job.second)) {
                val classed = classify(item.title, item.body, item.outlet, enabled) ?: continue
                val story = Story(
                    storyId(item.url),
                    classed.first,
                    classed.second,
                    item.at,
                    listOf(Source(item.outlet, item.url)),
                    detailFor(item.title, item.body, item.outlet, classed.first),
                )
                if (stories.none { sameStory(it, story) }) stories += story
            }
        }
        return stories.sortedByDescending { it.at }
    }

    fun rewrite(key: String, model: String, items: List<Story>, enabled: Map<String, Boolean>): List<Story> {
        if (key.isBlank() || items.isEmpty()) return items
        val batch = items.take(6)
        val payload = JSONArray()
        batch.forEach { payload.put(JSONObject().put("id", it.id).put("text", it.text.take(700))) }
        val prompt = listOf(
            "أنت محرر. اختصر كل خبر في جملتين: من فعل، ماذا حدث، وأين.",
            "لا تغيّر البلد، ولا تضف بلدًا لم يرد في النص، ولا تعامل اسم الجريدة كأنه مدينة.",
            "لا تخترع رقمًا. لا تكتب اسم الوكالة داخل الخبر. لا رموز ولا أكواد.",
            "سؤال أو رأي أو جولة صحف أو خبر بلا فاعل وفعل ونتيجة: keep=false.",
            "أعد JSON فقط: {\"items\":[{\"id\":\"\",\"text\":\"\",\"keep\":true}]}",
            payload.toString(),
        ).joinToString("\n")
        val raw = call(key, model, prompt) ?: fallbacks.firstNotNullOfOrNull { call(key, it, prompt) } ?: return items
        val rows = parseRows(raw) ?: return items
        val byId = rows.associateBy { it.first }
        return items.mapNotNull { story ->
            val row = byId[story.id] ?: return@mapNotNull story
            if (!row.second) return@mapNotNull null
            val outlet = story.sources.firstOrNull()?.outlet.orEmpty()
            val text = stripMasthead(arabicProse(row.third), outlet)
            if (!isNewsworthy(text)) return@mapNotNull story
            val desks = matchDesks(text, enabled, outlet)
            if (desks.isEmpty() || story.desks.none { it in desks }) return@mapNotNull story
            story.copy(text = text, desks = desks)
        }
    }

    fun article(url: String): String? {
        if (!url.startsWith("https://") || url.contains("news.google.")) return null
        val html = get(url) ?: return null
        val plain = html.replace(Regex("(?is)<(script|style|noscript)[^>]*>.*?</\\1>"), " ")
        val parts = Regex("(?is)<p[^>]*>(.*?)</p>")
            .findAll(plain)
            .map { arabicProse(it.groupValues[1]) }
            .filter { bit -> bit.length >= 40 && bit.count { it.code in 0x0600..0x06FF } >= 20 }
            .distinct()
            .take(4)
            .toList()
        val text = parts.joinToString(" ")
        return text.take(900).ifBlank { null }
    }

    fun testKey(key: String, model: String): String {
        val ok = call(key, model, "أجب بكلمة واحدة: تمام") != null ||
            fallbacks.any { call(key, it, "أجب بكلمة واحدة: تمام") != null }
        return if (ok) "المفتاح يعمل." else "المفتاح غير مقبول."
    }

    private fun call(key: String, model: String, prompt: String): String? {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.2).put("maxOutputTokens", 1800))
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", key)
            .post(body)
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else readText(response.body?.string().orEmpty())
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readText(raw: String): String? {
        val parts = JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: return null
        val text = buildString {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (!part.optBoolean("thought")) append(part.optString("text"))
            }
        }.trim()
        return text.ifBlank { null }
    }

    private fun parseRows(raw: String): List<Triple<String, Boolean, String>>? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val items = JSONObject(raw.substring(start, end + 1)).optJSONArray("items") ?: return null
        return List(items.length()) { index ->
            val row = items.optJSONObject(index) ?: return@List Triple("", false, "")
            Triple(row.optString("id"), row.optBoolean("keep", true), row.optString("text"))
        }.filter { it.first.isNotBlank() }
    }

    private fun get(url: String): String? {
        if (!url.startsWith("https://")) return null
        val request = Request.Builder().url(url).header("user-agent", "Mawjiz/5.0").build()
        return try {
            http.newCall(request).execute().use { if (it.isSuccessful) it.body?.string() else null }
        } catch (_: Exception) {
            null
        }
    }

    private fun parse(xml: String, fallback: String): List<RawItem> {
        val items = mutableListOf<RawItem>()
        for (chunk in xml.split(Regex("<item\\b", RegexOption.IGNORE_CASE)).drop(1)) {
            val block = chunk.substringBefore("</item>")
            val rawTitle = tag(block, "title")
            val link = tag(block, "link").ifBlank { attr(block) }
            if (rawTitle.isBlank() || !link.startsWith("http")) continue
            val pair = splitOutlet(arabicProse(rawTitle), fallback)
            val outlet = arabicProse(tag(block, "source")).ifBlank { pair.second }.ifBlank { fallback }.take(42)
            val at = parseDate(tag(block, "pubDate").ifBlank { tag(block, "dc:date") })
            if (at > 0 && System.currentTimeMillis() - at > 48L * 60 * 60 * 1000) continue
            items += RawItem(pair.first, link, outlet, arabicProse(tag(block, "description")).take(360), if (at > 0) at else System.currentTimeMillis())
            if (items.size == 14) break
        }
        return items
    }

    private fun tag(block: String, name: String): String {
        val match = Regex("<$name(?:\\s[^>]*)?>([\\s\\S]*?)</$name>", RegexOption.IGNORE_CASE).find(block) ?: return ""
        return match.groupValues[1].replace("<![CDATA[", "").replace("]]>", "").trim()
    }

    private fun attr(block: String): String {
        return Regex("<link[^>]*href=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1).orEmpty()
    }
}

private fun google(query: String): String {
    return "https://news.google.com/rss/search?q=" +
        URLEncoder.encode(query, "UTF-8") + "&hl=ar&gl=SA&ceid=SA:ar"
}

private fun splitOutlet(title: String, fallback: String): Pair<String, String> {
    val parts = title.split(Regex("\\s[-–—|]\\s"))
    if (parts.size < 2) return title to fallback
    val outlet = parts.last().trim()
    if (outlet.length !in 2..42) return title to fallback
    return parts.dropLast(1).joinToString(" ").trim() to outlet
}

private fun parseDate(raw: String): Long {
    if (raw.isBlank()) return 0
    for (pattern in listOf("EEE, dd MMM yyyy HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ssX")) {
        try {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.isLenient = true
            return format.parse(raw)?.time ?: continue
        } catch (_: Exception) {
            continue
        }
    }
    return 0
}
