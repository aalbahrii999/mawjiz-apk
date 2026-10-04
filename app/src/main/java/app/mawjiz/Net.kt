package app.mawjiz

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.min

data class RawItem(val title: String, val url: String, val outlet: String, val body: String, val at: Long)

class Net {
    val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    fun gather(enabled: Map<String, Boolean>): List<RawItem> {
        val jobs = mutableListOf<Pair<String, String>>()
        Catalog.direct.forEach { jobs.add(it) }
        Catalog.queries.forEach { (id, query) ->
            if (enabled[id] == true) {
                val url = "https://news.google.com/rss/search?q=" +
                    URLEncoder.encode(query, "UTF-8") + "&hl=ar&gl=SA&ceid=SA:ar"
                jobs.add(url to Catalog.byId(id)?.label.orEmpty())
            }
        }
        val found = mutableListOf<RawItem>()
        for (job in jobs.take(14)) {
            val xml = get(job.first) ?: continue
            found += regexParse(xml, job.second)
        }
        return found
    }

    fun rewrite(key: String, model: String, items: List<Story>): List<Story> {
        if (key.isBlank() || items.isEmpty()) return items
        val batch = items.take(6)
        val payload = JSONArray()
        batch.forEach { story ->
            payload.put(JSONObject().put("id", story.id).put("text", story.text.take(700)))
        }
        val prompt = listOf(
            "اكتب كل خبر بالعربية الواضحة بنبرة سعودية هادئة. جملتان: من فعل، ماذا حدث، أين، والنتيجة.",
            "إذا ذكر النص أثرًا مباشرًا على بلد آخر، أدخله كما هو ولا تخترعه.",
            "جولة صحف، مقال رأي، معرض، زيارة، أو خبر مشاهير: keep=false.",
            "لا تلصق اسم الوكالة. لا سؤال. لا رقم غير موجود في النص.",
            """أعد JSON فقط: {"items":[{"id":"...","text":"...","keep":true}]}""",
            payload.toString(),
        ).joinToString("\n")
        val used = callModel(key, model, prompt) ?: fallbackModels.firstNotNullOfOrNull { callModel(key, it, prompt) } ?: return items
        val rows = parseRewrite(used) ?: return items
        val byId = rows.associateBy { it.first }
        return items.map { story ->
            val row = byId[story.id] ?: return@map story
            if (!row.second) return@map story
            val text = cleanCopy(row.third)
            if (!isNewsworthy(text)) story else story.copy(text = text)
        }.filter { story -> byId[story.id]?.second != false }
    }

    fun testKey(key: String, model: String): String {
        val prompt = "أجب بكلمة واحدة: تمام"
        val ok = callModel(key, model, prompt) != null || fallbackModels.any { callModel(key, it, prompt) != null }
        return if (ok) "المفتاح يعمل." else "المفتاح غير مقبول."
    }

    private val fallbackModels = listOf("gemini-2.5-flash-lite", "gemini-2.0-flash")

    private fun callModel(key: String, model: String, prompt: String): String? {
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            .put("generationConfig", JSONObject().put("temperature", 0.2).put("maxOutputTokens", 2048))
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", key)
            .post(body)
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else readGemini(response.body?.string().orEmpty())
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readGemini(raw: String): String? {
        val parts = JSONObject(raw).optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts") ?: return null
        val text = buildString {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought")) continue
                append(part.optString("text"))
            }
        }.trim()
        return text.ifBlank { null }
    }

    private fun parseRewrite(raw: String): List<Triple<String, Boolean, String>>? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val items = JSONObject(raw.substring(start, end + 1)).optJSONArray("items") ?: return null
        val rows = mutableListOf<Triple<String, Boolean, String>>()
        for (i in 0 until items.length()) {
            val row = items.optJSONObject(i) ?: continue
            rows += Triple(row.optString("id"), row.optBoolean("keep", true), row.optString("text"))
        }
        return rows
    }

    private fun get(url: String): String? {
        if (!url.startsWith("https://")) return null
        val request = Request.Builder().url(url).header("user-agent", "Mawjiz/2.0").build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun regexParse(xml: String, fallback: String): List<RawItem> {
        val items = mutableListOf<RawItem>()
        for (chunk in xml.split(Regex("<item\\b", RegexOption.IGNORE_CASE)).drop(1)) {
            val block = chunk.substringBefore("</item>")
            val title = tag(block, "title")
            val link = tag(block, "link").ifBlank { attr(block, "link", "href") }
            if (title.isBlank() || !link.startsWith("http")) continue
            val split = splitOutlet(cleanCopy(title), fallback)
            val outlet = tag(block, "source").ifBlank { split.second }.let { cleanCopy(it).ifBlank { fallback } }
            val body = cleanCopy(tag(block, "description"))
            val text = joinText(split.first, body)
            if (!isNewsworthy(text)) continue
            val at = parseDate(tag(block, "pubDate").ifBlank { tag(block, "dc:date") })
            if (at > 0 && System.currentTimeMillis() - at > 48L * 60 * 60 * 1000) continue
            items += RawItem(split.first, link, outlet.take(42), body, if (at > 0) at else System.currentTimeMillis())
            if (items.size == 18) break
        }
        return items
    }

    private fun tag(block: String, name: String): String {
        val match = Regex("<$name(?:\\s[^>]*)?>([\\s\\S]*?)</$name>", RegexOption.IGNORE_CASE).find(block) ?: return ""
        return match.groupValues[1].replace(Regex("<!\\[CDATA\\[|]]>"), "").trim()
    }

    private fun attr(block: String, tag: String, name: String): String {
        val match = Regex("<$tag[^>]*$name=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(block) ?: return ""
        return match.groupValues[1]
    }
}

fun splitOutlet(title: String, fallback: String): Pair<String, String> {
    val parts = title.split(Regex("\\s[-–—|]\\s"))
    if (parts.size < 2) return title to fallback
    val outlet = parts.last().trim()
    if (outlet.length !in 2..42) return title to fallback
    return parts.dropLast(1).joinToString(" - ").trim() to outlet
}

fun joinText(title: String, body: String): String {
    val head = cleanCopy(title)
    val rest = cleanCopy(body).take(320)
    if (rest.length > head.length + 20 && !rest.startsWith(head.take(min(12, head.length)))) {
        return "$head. $rest".replace(Regex("\\s+"), " ").trim()
    }
    return head
}

fun parseDate(raw: String): Long {
    if (raw.isBlank()) return 0
    val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    for (pattern in patterns) {
        try {
            val format = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            format.isLenient = true
            return format.parse(raw)?.time ?: continue
        } catch (_: Exception) {
            continue
        }
    }
    return 0
}

fun toStories(raw: List<RawItem>, enabled: Map<String, Boolean>, order: List<String>): List<Story> {
    val rank = order.withIndex().associate { it.value to it.index }
    val stories = mutableListOf<Story>()
    for (item in raw) {
        val text = joinText(item.title, item.body)
        val desks = matchDesks("$text ${item.outlet}", enabled)
            .sortedBy { rank[it] ?: 99 }
        if (desks.isEmpty() || !isNewsworthy(text)) continue
        val story = Story(
            id = storyId(item.url),
            text = text,
            desks = desks,
            at = item.at,
            sources = listOf(Source(item.outlet.ifBlank { "مصدر" }, item.url)),
        )
        if (stories.any { sameStory(it, story) }) continue
        stories += story
    }
    return stories.sortedByDescending { it.at }
}
