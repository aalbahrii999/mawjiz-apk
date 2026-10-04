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
    private val prefs = app.getSharedPreferences("mawjiz", Context.MODE_PRIVATE)
    private val net = Net()

    var stories by mutableStateOf<List<Story>>(emptyList())
    var order by mutableStateOf(Catalog.defaultOrder)
    var enabled by mutableStateOf(Catalog.defaultEnabled())
    var selected by mutableStateOf(setOf<String>())
    var loading by mutableStateOf(false)
    var key by mutableStateOf("")
    var model by mutableStateOf("gemini-3.5-flash-lite")
    var note by mutableStateOf("")
    var showSettings by mutableStateOf(false)

    init {
        load()
        refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        note = ""
        viewModelScope.launch {
            val incoming = withContext(Dispatchers.IO) {
                val raw = net.gather(enabled)
                val fresh = toStories(raw, enabled, order)
                net.rewrite(key, model, fresh)
            }
            stories = mergeStories(stories, incoming, System.currentTimeMillis())
            save()
            loading = false
            if (incoming.isEmpty() && stories.isEmpty()) note = "ما وصلت مادة جديدة."
        }
    }

    fun toggleDesk(id: String) {
        selected = if (id in selected) selected - id else selected + id
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

    fun setEnabled(id: String, on: Boolean) {
        enabled = enabled + (id to on)
        if (!on) selected = selected - id
        save()
    }

    fun saveKey(nextKey: String, nextModel: String) {
        key = nextKey.trim()
        model = nextModel.trim().ifBlank { "gemini-3.5-flash-lite" }
        save()
    }

    fun testKey(nextKey: String, nextModel: String) {
        viewModelScope.launch {
            note = "يختبر المفتاح…"
            val message = withContext(Dispatchers.IO) {
                net.testKey(nextKey.trim(), nextModel.trim().ifBlank { "gemini-3.5-flash-lite" })
            }
            note = message
        }
    }

    private fun load() {
        val raw = prefs.getString("state", null) ?: return
        try {
            val json = JSONObject(raw)
            key = json.optString("key")
            model = json.optString("model").ifBlank { model }
            val enabledJson = json.optJSONObject("enabled")
            if (enabledJson != null) {
                enabled = Catalog.desks.associate { it.id to enabledJson.optBoolean(it.id, enabled[it.id] == true) }
            }
            val storedOrder = json.optJSONArray("order")?.toList().orEmpty().filter { Catalog.byId(it) != null }
            order = (storedOrder + Catalog.defaultOrder).distinct()
            val selectedJson = json.optJSONArray("selected")?.toList().orEmpty()
            selected = selectedJson.filter { enabled[it] == true }.toSet()
            val items = json.optJSONArray("stories") ?: return
            val loaded = mutableListOf<Story>()
            for (i in 0 until items.length()) {
                val row = items.optJSONObject(i) ?: continue
                val sources = mutableListOf<Source>()
                val sourceRows = row.optJSONArray("sources")
                if (sourceRows != null) {
                    for (s in 0 until sourceRows.length()) {
                        val source = sourceRows.optJSONObject(s) ?: continue
                        sources += Source(source.optString("outlet"), source.optString("url"))
                    }
                }
                loaded += Story(
                    id = row.optString("id"),
                    text = row.optString("text"),
                    desks = row.optJSONArray("desks")?.toList().orEmpty(),
                    at = row.optLong("at"),
                    sources = sources,
                )
            }
            stories = mergeStories(emptyList(), loaded, System.currentTimeMillis())
        } catch (_: Exception) {
            note = ""
        }
    }

    private fun save() {
        val json = JSONObject()
            .put("key", key)
            .put("model", model)
            .put("order", JSONArray(order))
            .put("selected", JSONArray(selected.toList()))
        val enabledJson = JSONObject()
        enabled.forEach { (id, on) -> enabledJson.put(id, on) }
        json.put("enabled", enabledJson)
        val items = JSONArray()
        stories.forEach { story ->
            val sources = JSONArray()
            story.sources.forEach { source ->
                sources.put(JSONObject().put("outlet", source.outlet).put("url", source.url))
            }
            items.put(
                JSONObject()
                    .put("id", story.id)
                    .put("text", story.text)
                    .put("desks", JSONArray(story.desks))
                    .put("at", story.at)
                    .put("sources", sources),
            )
        }
        json.put("stories", items)
        prefs.edit().putString("state", json.toString()).apply()
    }
}

private fun JSONArray.toList(): List<String> = List(length()) { optString(it) }.filter { it.isNotBlank() }
