package app.mawjiz

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MawjizModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("mawjiz4", Context.MODE_PRIVATE)
    private val net = Net()

    var stories by mutableStateOf<List<Story>>(emptyList())
    var order by mutableStateOf(Catalog.defaultOrder)
    var enabled by mutableStateOf(Catalog.defaultEnabled())
    var sources by mutableStateOf(Catalog.defaultSources)
    var scopeSources by mutableStateOf(Catalog.defaultScopeSources())
    var selected by mutableStateOf(setOf<String>())
    var loading by mutableStateOf(false)
    var key by mutableStateOf("")
    var modelName by mutableStateOf("gemini-3.5-flash-lite")
    var morning by mutableStateOf("07:00")
    var evening by mutableStateOf("19:00")
    var font by mutableStateOf("cairo")
    var size by mutableStateOf("md")
    var theme by mutableStateOf("dark")
    var note by mutableStateOf("")
    var page by mutableStateOf("")
    var tab by mutableStateOf("feed")
    var reading by mutableStateOf<Story?>(null)
    var readingMore by mutableStateOf("")
    var readingBusy by mutableStateOf(false)
    var interests by mutableStateOf(setOf<String>())
    var taste by mutableStateOf<Map<String, Int>>(emptyMap())
    var tasteDay by mutableStateOf("")

    init {
        load()
        refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        note = ""
        decayTaste()
        val desks = enabled
        val chosen = scopeSources
        val liked = taste
        viewModelScope.launch {
            val incoming = withContext(Dispatchers.IO) {
                val fresh = net.gather(desks, chosen, liked)
                val written = net.rewrite(key, modelName, fresh, desks)
                written.mapNotNull { retag(it, desks) }
            }
            stories = mergeStories(stories, incoming, System.currentTimeMillis()).mapNotNull { retag(it, desks) }
            save()
            loading = false
            if (incoming.isEmpty() && stories.isEmpty()) note = "ما وصلت مادة جديدة."
        }
    }

    fun openSettings() {
        tab = "settings"
        page = "home"
        reading = null
    }

    fun showFeed() {
        tab = "feed"
        page = ""
        reading = null
    }

    fun showSaved() {
        tab = "saved"
        page = ""
        reading = null
    }

    fun backSettings() {
        page = if (page == "home" || page.isEmpty()) "" else "home"
        if (page.isEmpty()) tab = "feed"
    }

    fun openStory(story: Story) {
        reading = story
        readingMore = story.detail
        val next = taste.toMutableMap()
        story.desks.forEach { id -> next[id] = ((next[id] ?: 0) + 1).coerceAtMost(20) }
        taste = next
        save()
        val url = story.sources.firstOrNull()?.url.orEmpty()
        if (url.isBlank() || story.detail.length > 180) return
        readingBusy = true
        viewModelScope.launch {
            val more = withContext(Dispatchers.IO) { net.article(url) }
            readingBusy = false
            if (more.isNullOrBlank() || reading?.id != story.id) return@launch
            readingMore = more
            stories = stories.map { if (it.id == story.id) it.copy(detail = more) else it }
            reading = stories.find { it.id == story.id }
            save()
        }
    }

    fun closeStory() {
        reading = null
        readingMore = ""
        readingBusy = false
    }

    fun toggleSaved(story: Story) {
        stories = stories.map { if (it.id == story.id) it.copy(saved = !it.saved) else it }
        reading = stories.find { it.id == story.id } ?: reading
        save()
    }

    fun shareText(story: Story): String {
        val outlet = story.sources.firstOrNull { it.outlet.isNotBlank() }?.outlet
        return story.text + (if (outlet.isNullOrBlank()) "" else "\n$outlet") + "\nموجز"
    }

    fun openPage(id: String) {
        page = id
    }

    fun interest(story: Story) {
        val desk = primaryDesk(story.desks)
        if (story.id in interests) {
            interests = interests - story.id
            if (desk != null) {
                val left = (taste[desk] ?: 0) - 4
                taste = if (left <= 0) taste - desk else taste + (desk to left)
            }
        } else {
            interests = interests + story.id
            if (desk != null) taste = taste + (desk to ((taste[desk] ?: 0) + 4).coerceAtMost(12))
        }
        save()
    }

    private fun decayTaste() {
        val day = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Riyadh")).toString()
        if (tasteDay == day) return
        tasteDay = day
        taste = taste.mapValues { (_, count) -> count / 2 }.filterValues { it > 0 }
    }

    fun showScope(id: String) {
        selected = if (id.isEmpty()) emptySet() else setOf(id)
        tab = "feed"
        page = ""
        save()
    }

    fun showAll() {
        selected = emptySet()
    }

    fun moveDesk(id: String, before: String) {
        if (id == before) return
        val next = order.toMutableList()
        next.remove(id)
        val index = next.indexOf(before)
        if (index < 0) return
        next.add(index, id)
        order = next
        save()
    }

    fun setDesk(id: String, on: Boolean) {
        if (!on && enabled.values.count { it } <= 1) {
            note = "اختر تصنيفًا واحدًا على الأقل."
            return
        }
        enabled = enabled + (id to on)
        if (!on) selected = selected - id
        save()
    }

    fun setScopeSource(scope: String, id: String, on: Boolean) {
        val current = scopeSources[scope].orEmpty()
        val next = if (on) (current + id).distinct() else current.filter { it != id }
        if (next.isEmpty()) {
            note = "أبقِ مصدرًا واحدًا في هذا النطاق."
            return
        }
        scopeSources = scopeSources + (scope to next)
        sources = scopeSources.values.flatten().distinct()
        save()
    }

    fun savePrefs(nextKey: String, nextModel: String, nextMorning: String, nextEvening: String) {
        if (!clockOk(nextMorning) || !clockOk(nextEvening) || nextEvening <= nextMorning) {
            note = "وقت المساء يكون بعد وقت الصباح."
            return
        }
        key = nextKey.trim()
        modelName = nextModel.trim().ifBlank { "gemini-3.5-flash-lite" }
        morning = nextMorning
        evening = nextEvening
        note = "حُفظت الإعدادات على الجهاز."
        save()
    }

    fun setLook(nextFont: String? = null, nextSize: String? = null, nextTheme: String? = null) {
        if (nextFont != null) font = nextFont
        if (nextSize != null) size = nextSize
        if (nextTheme != null) theme = nextTheme
        save()
    }

    fun testKey(nextKey: String, nextModel: String) {
        viewModelScope.launch {
            note = "يختبر المفتاح…"
            note = withContext(Dispatchers.IO) {
                net.testKey(nextKey.trim(), nextModel.trim().ifBlank { "gemini-3.5-flash-lite" })
            }
        }
    }

    private fun clockOk(value: String) = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)

    private fun load() {
        val raw = prefs.getString("state", null) ?: return
        try {
            val json = JSONObject(raw)
            key = json.optString("key")
            modelName = json.optString("model").ifBlank { modelName }
            morning = json.optString("morning").ifBlank { morning }
            evening = json.optString("evening").ifBlank { evening }
            font = json.optString("font").ifBlank { font }
            size = json.optString("size").ifBlank { size }
            theme = json.optString("theme").ifBlank { theme }
            val enabledJson = json.optJSONObject("enabled")
            if (enabledJson != null) {
                enabled = Catalog.desks.associate { it.id to enabledJson.optBoolean(it.id, enabled[it.id] == true) }
            }
            val storedOrder = json.optJSONArray("order").strings().filter { Catalog.desk(it) != null }
            if (storedOrder.isNotEmpty()) order = (storedOrder + Catalog.defaultOrder).distinct()
            val scopeJson = json.optJSONObject("scopeSources")
            if (scopeJson != null) {
                val map = mutableMapOf<String, List<String>>()
                for (scope in listOf("local", "region", "world")) {
                    map[scope] = scopeJson.optJSONArray(scope).strings().ifEmpty { Catalog.defaultScopeSources()[scope].orEmpty() }
                }
                scopeSources = map
            }
            sources = scopeSources.values.flatten().distinct()
            selected = json.optJSONArray("selected").strings().filter { it in setOf("local", "region", "world") }.toSet()
            val loaded = mutableListOf<Story>()
            val items = json.optJSONArray("stories") ?: JSONArray()
            for (i in 0 until items.length()) {
                val row = items.optJSONObject(i) ?: continue
                val links = mutableListOf<Source>()
                val rows = row.optJSONArray("sources")
                if (rows != null) {
                    for (s in 0 until rows.length()) {
                        val source = rows.optJSONObject(s) ?: continue
                        links += Source(source.optString("outlet"), source.optString("url"))
                    }
                }
                loaded += Story(
                    row.optString("id"),
                    row.optString("text"),
                    row.optJSONArray("desks").strings(),
                    row.optLong("at"),
                    links,
                    row.optString("detail"),
                    row.optBoolean("saved"),
                    row.optString("image"),
                    row.optString("why"),
                )
            }
            stories = mergeStories(emptyList(), loaded, System.currentTimeMillis()).mapNotNull { retag(it, enabled) }
            val tasteJson = json.optJSONObject("taste")
            if (tasteJson != null) {
                val map = mutableMapOf<String, Int>()
                val keys = tasteJson.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    map[id] = tasteJson.optInt(id)
                }
                taste = map
            }
            tasteDay = json.optString("tasteDay")
            interests = json.optJSONArray("interests").strings().toSet()
        } catch (_: Exception) {
            note = ""
        }
    }

    private fun save() {
        val json = JSONObject()
            .put("key", key)
            .put("model", modelName)
            .put("morning", morning)
            .put("evening", evening)
            .put("font", font)
            .put("size", size)
            .put("theme", theme)
            .put("order", JSONArray(order))
        val scopeJson = JSONObject()
        scopeSources.forEach { (scope, ids) -> scopeJson.put(scope, JSONArray(ids)) }
        json.put("scopeSources", scopeJson)
            .put("selected", JSONArray(selected.toList()))
        val enabledJson = JSONObject()
        enabled.forEach { (id, on) -> enabledJson.put(id, on) }
        json.put("enabled", enabledJson)
        val tasteJson = JSONObject()
        taste.forEach { (id, count) -> tasteJson.put(id, count) }
        json.put("taste", tasteJson)
        json.put("tasteDay", tasteDay)
        json.put("interests", JSONArray(interests.toList()))
        val items = JSONArray()
        stories.forEach { story ->
            val links = JSONArray()
            story.sources.forEach { links.put(JSONObject().put("outlet", it.outlet).put("url", it.url)) }
            items.put(
                JSONObject()
                    .put("id", story.id)
                    .put("text", story.text)
                    .put("detail", story.detail)
                    .put("saved", story.saved)
                    .put("desks", JSONArray(story.desks))
                    .put("at", story.at)
                    .put("image", story.image)
                    .put("why", story.why)
                    .put("sources", links),
            )
        }
        json.put("stories", items)
        prefs.edit().putString("state", json.toString()).apply()
    }
}

private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { optString(it) }.filter { it.isNotBlank() }
}
