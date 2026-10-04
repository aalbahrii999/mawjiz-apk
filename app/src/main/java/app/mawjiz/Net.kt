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
            if (outlet.id !in sources) continue
            if (outlet.feed != null) jobs += outlet.feed to outlet.label
            val url = "https://news.google.com/rss/search?q=" +
                URLEncoder.encode(outlet.query, "UTF-8") + "&hl=ar&gl=SA&ceid=SA:ar"
            jobs += url to outlet.label
        }
        for ((id, query) in Catalog.queries) {
            if (enabled[id] != true) continue
            val url = "https://news.google.com/rss/search?q=" +
                URLEncoder.encode(query, "UTF-8") + "&hl=ar&gl=SA&ceid=SA:ar"
            jobs += url to (Catalog.desk(id)?.label ?: "")
        }
        val stories = mutableListOf<Story>()
        for (job in jobs.distinctBy { it.first }.take(16)) {
            val xml = get(job.first) ?: continue
            for (item in parse(xml, job.second)) {
                val text = readable(item.title, item.body)
                if (!isNewsworthy(text)) continue
                val desks = matchDesks(text, enabled)
                if (desks.isEmpty()) continue
                val story = Story(storyId(item.url), text, desks, item.at, listOf(Source(item.outlet, item.url)))
                if (stories.none { sameStory(it, story) }) stories += story
            }
        }
        return stories.sortedByDescending { it.at }
    }

    fun rewrite(key: String, model: String, items: List<Story>): List<Story> {
        if (key.isBlank() || items.isEmpty()) return items
        val batch = items.take(6)
        val payload = JSONArray()
        batch.forEach { payload.put(JSONObject().put("id", it.id).put("text", it.text.take(700))) }
        val prompt = listOf(
            "أنت محرر نشرة. لا يمر إلا خبر وقع فعلًا وله فاعل ومكان ونتيجة.",
            "إن كان العنوان سؤالًا أو رأيًا أو جولة صحف أو زيارة أو معرضًا أو بلا حدث واضح: keep=false.",
            "إن لم تفهم ماذا تغير على الأرض: keep=false. لا تلخص الغامض.",
            "جملتان فقط. من فعل، ماذا حدث، أين، وما النتيجة.",
            "ممنوع إدخال اسم الوكالة أو رموز أو أكواد أو أرقام مكررة.",
            "لا رقم إلا إذا كان في النص الأصلي عددًا أو سعرًا أو نسبة.",
            "أعد JSON فقط: {\"items\":[{\"id\":\"\",\"text\":\"\",\"keep\":true}]}",
            payload.toString(),
        ).joinToString("\n")
        val raw = call(key, model, prompt) ?: fallbacks.firstNotNullOfOrNull { call(key, it, prompt) } ?: return items
        val rows = parseRows(raw) ?: return items
        val byId = rows.associateBy { it.first }
        return items.mapNotNull { story ->
            val row = byId[story.id] ?: return@mapNotNull story
            if (!row.second) return@mapNotNull null
            val text = arabicProse(row.third)
            if (!isNewsworthy(text)) return@mapNotNull null
            story.copy(text = text)
        }
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
        val request = Request.Builder().url(url).header("user-agent", "Mawjiz/4.0").build()
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
