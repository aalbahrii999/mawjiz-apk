package app.mawjiz

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private data class Paint(val paper: Color, val sheet: Color, val ink: Color, val muted: Color, val line: Color)

private fun paint(theme: String) = if (theme == "light") {
    Paint(Color(0xFFF3EEE6), Color(0xFFFFFBF5), Color(0xFF1A211F), Color(0xFF5E6A66), Color(0xFFE4DDD2))
} else {
    Paint(Color(0xFF10141B), Color(0xFF1C2422), Color(0xFFF4EFE6), Color(0xFF9AA39E), Color(0xFF2C3532))
}

private val Teal = Color(0xFF1B5E56)
private val CairoFont = FontFamily(Font(R.font.cairo))
private val AmiriFont = FontFamily(Font(R.font.amiri))
private val PlexFont = FontFamily(Font(R.font.plex))

private fun face(id: String) = when (id) {
    "naskh" -> AmiriFont
    "plex" -> PlexFont
    else -> CairoFont
}

@Composable
fun MawjizApp(model: MawjizModel = viewModel()) {
    val colors = paint(model.theme)
    val font = face(model.font)
    val body = when (model.size) { "sm" -> 15.sp; "lg" -> 20.sp; else -> 17.sp }
    val context = LocalContext.current
    BackHandler(enabled = model.reading != null || model.page.isNotEmpty()) {
        if (model.reading != null) model.closeStory() else model.backSettings()
    }
    if (model.reading != null) {
        ReaderScreen(model, colors, font, body, context)
        return
    }
    Scaffold(containerColor = colors.paper, bottomBar = { BottomBar(model, colors, font) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (model.tab) {
                "saved" -> SavedScreen(model, colors, font, body, context)
                "settings" -> when (model.page) {
                    "desks" -> DeskSettings(model, colors, font)
                    "sources" -> SourceSettings(model, colors, font)
                    "edition" -> EditionSettings(model, colors, font)
                    "look" -> LookSettings(model, colors, font)
                    "summary" -> SummarySettings(model, colors, font)
                    else -> SettingsHome(model, colors, font)
                }
                else -> TimelineScreen(model, colors, font, body, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineScreen(model: MawjizModel, colors: Paint, font: FontFamily, body: TextUnit, context: android.content.Context) {
    val now = System.currentTimeMillis()
    val visible = model.stories.filter { story ->
        story.desks.any { model.enabled[it] == true } &&
            (model.selected.isEmpty() || story.desks.any { it in model.selected })
    }.sortedWith(compareByDescending<Story> { rankScore(it, now, model.taste) }.thenByDescending { it.at })
    val chips = model.order.mapNotNull { id -> Catalog.desk(id)?.takeIf { model.enabled[id] == true } }
    Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("موجز", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
            }
            ChipRow(chips, model.selected, colors, font, model::toggleFilter, model::showAll, model::moveDesk)
            if (model.note.isNotBlank()) {
                Text(model.note, color = colors.muted, fontSize = 13.sp, fontFamily = font, modifier = Modifier.padding(horizontal = 16.dp))
            }
            PullToRefreshBox(
                isRefreshing = model.loading,
                onRefresh = {
                    playRefresh(context)
                    model.refresh()
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (visible.isEmpty() && !model.loading) {
                    Text("لا مادة هنا.", color = colors.ink, fontFamily = font, modifier = Modifier.padding(24.dp))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                        items(visible, key = { it.id }) { story ->
                            StoryRow(story, colors, font, body, { model.openStory(story) }, { model.toggleSaved(story) }, { shareStory(context, model.shareText(story)) })
                            HorizontalDivider(color = colors.line)
                        }
                    }
                }
            }
        }
}

@Composable
private fun ChipRow(
    chips: List<Desk>,
    selected: Set<String>,
    colors: Paint,
    font: FontFamily,
    onToggle: (String) -> Unit,
    onAll: () -> Unit,
    onMove: (String, String) -> Unit,
) {
    val centers = remember { mutableMapOf<String, Float>() }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip("الكل", selected.isEmpty(), null, colors, font, onAll)
        chips.forEach { desk ->
            FilterChip(
                desk.label,
                desk.id in selected,
                Color(desk.color.toInt()),
                colors,
                font,
                { onToggle(desk.id) },
                { x ->
                    val target = centers.entries.filter { it.key != desk.id }.minByOrNull { abs(it.value - x) }?.key
                    if (target != null) onMove(desk.id, target)
                },
                { centers[desk.id] = it },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    on: Boolean,
    mark: Color?,
    colors: Paint,
    font: FontFamily,
    onClick: () -> Unit,
    onDrag: ((Float) -> Unit)? = null,
    onPlace: ((Float) -> Unit)? = null,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) colors.ink else colors.sheet)
            .onGloballyPositioned {
                coords = it
                onPlace?.invoke(it.boundsInWindow().center.x)
            }
            .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
            .pointerInput(label, onDrag) {
                if (onDrag == null) return@pointerInput
                detectDragGesturesAfterLongPress(onDrag = { change, _ ->
                    val window = coords?.localToWindow(change.position) ?: return@detectDragGesturesAfterLongPress
                    onDrag(window.x)
                })
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mark != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(mark))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = if (on) colors.paper else colors.ink, fontFamily = font, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun StoryRow(
    story: Story,
    colors: Paint,
    font: FontFamily,
    body: TextUnit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val stamp = remember(story.at) {
        val format = SimpleDateFormat("d MMM · h:mm a", Locale("ar"))
        format.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        format.format(Date(story.at))
    }
    val lead = Catalog.desk(story.desks.firstOrNull().orEmpty())
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color((lead?.color ?: 0xFF1B5E56).toInt())),
                contentAlignment = Alignment.Center,
            ) {
                Text(lead?.label?.take(1) ?: "م", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = font)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    story.desks.take(2).forEach { id ->
                        val desk = Catalog.desk(id) ?: return@forEach
                        Text(desk.label, color = Color(desk.color.toInt()), fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = font, modifier = Modifier.padding(end = 8.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(stamp, color = colors.muted, fontSize = 12.sp, fontFamily = font)
                }
                Spacer(Modifier.height(4.dp))
                Text(story.text, color = colors.ink, fontSize = body, lineHeight = body * 1.45, fontFamily = font)
                story.sources.firstOrNull { it.outlet.isNotBlank() }?.let { source ->
                    Spacer(Modifier.height(6.dp))
                    Text(source.outlet, color = colors.muted, fontSize = 12.sp, fontFamily = font)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (story.saved) "محفوظ" else "حفظ", color = colors.ink, fontSize = 13.sp, fontFamily = font, modifier = Modifier.clickable(onClick = onSave))
                    Text("مشاركة", color = colors.ink, fontSize = 13.sp, fontFamily = font, modifier = Modifier.clickable(onClick = onShare))
                    Text("توسيع", color = colors.muted, fontSize = 13.sp, fontFamily = font, modifier = Modifier.clickable(onClick = onOpen))
                }
            }
        }
    }
}

@Composable
private fun SettingsHome(model: MawjizModel, colors: Paint, font: FontFamily) {
    val desksOn = model.enabled.values.count { it }
    val look = when (model.theme) { "light" -> "فاتح"; else -> "داكن" }
    val key = if (model.key.isBlank()) "بلا مفتاح" else "المفتاح محفوظ"
    Box(Modifier.fillMaxSize()) { val padding = PaddingValues(0.dp)
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            TextButton(onClick = { model.backSettings() }) { Text("رجوع", color = colors.muted, fontFamily = font) }
            Text("الإعدادات", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
            Spacer(Modifier.height(8.dp))
            MenuRow("التصنيفات", "$desksOn ظاهرة", colors, font) { model.openPage("desks") }
            MenuRow("المصادر", "${model.sources.size} وكالة", colors, font) { model.openPage("sources") }
            MenuRow("النشرة", "${model.morning} · ${model.evening}", colors, font) { model.openPage("edition") }
            MenuRow("المظهر", look, colors, font) { model.openPage("look") }
            MenuRow("التلخيص", key, colors, font) { model.openPage("summary") }
        }
    }
}

@Composable
private fun DeskSettings(model: MawjizModel, colors: Paint, font: FontFamily) {
    var open by remember { mutableStateOf(setOf(Catalog.groups.first().title)) }
    Box(Modifier.fillMaxSize()) { val padding = PaddingValues(0.dp)
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item { SubHead("التصنيفات", colors, font) { model.backSettings() } }
            Catalog.groups.forEach { group ->
                item {
                    MenuRow(group.title, "${group.ids.count { model.enabled[it] == true }} / ${group.ids.size}", colors, font) {
                        open = if (group.title in open) open - group.title else open + group.title
                    }
                }
                if (group.title in open) {
                    items(group.ids, key = { "desk-$it" }) { id ->
                        val desk = Catalog.desk(id) ?: return@items
                        Check(desk.label, model.enabled[id] == true, colors, font, Color(desk.color.toInt())) { model.setDesk(id, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSettings(model: MawjizModel, colors: Paint, font: FontFamily) {
    var open by remember { mutableStateOf(setOf(Catalog.outletGroups.first().first)) }
    Box(Modifier.fillMaxSize()) { val padding = PaddingValues(0.dp)
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item { SubHead("المصادر", colors, font) { model.backSettings() } }
            Catalog.outletGroups.forEach { (group, title) ->
                val outlets = Catalog.outlets.filter { it.group == group }
                item {
                    MenuRow(title, "${outlets.count { it.id in model.sources }} / ${outlets.size}", colors, font) {
                        open = if (group in open) open - group else open + group
                    }
                }
                if (group in open) {
                    items(outlets, key = { it.id }) { outlet ->
                        Check(outlet.label, outlet.id in model.sources, colors, font) { model.setSource(outlet.id, it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditionSettings(model: MawjizModel, colors: Paint, font: FontFamily) {
    var morning by remember { mutableStateOf(model.morning) }
    var evening by remember { mutableStateOf(model.evening) }
    val fields = fieldColors(colors)
    Box(Modifier.fillMaxSize()) { val padding = PaddingValues(0.dp)
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SubHead("النشرة", colors, font) { model.backSettings() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(morning, { morning = it }, label = { Text("الصباح") }, modifier = Modifier.weight(1f), colors = fields, singleLine = true)
                OutlinedTextField(evening, { evening = it }, label = { Text("المساء") }, modifier = Modifier.weight(1f), colors = fields, singleLine = true)
            }
            Spacer(Modifier.height(16.dp))
            SaveButton(colors, font) { model.savePrefs(model.key, model.modelName, morning, evening) }
            Note(model, colors, font)
        }
    }
}

@Composable
private fun LookSettings(model: MawjizModel, colors: Paint, font: FontFamily) {
    Box(Modifier.fillMaxSize()) { val padding = PaddingValues(0.dp)
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SubHead("المظهر", colors, font) { model.backSettings() }
            Text("الخط", color = colors.muted, fontSize = 13.sp, fontFamily = font)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("cairo" to "القاهرة", "naskh" to "نسخ", "plex" to "بليكس").forEach { (id, label) ->
                    val on = model.font == id
                    Text(
                        label,
                        color = if (on) colors.paper else colors.ink,
                        fontFamily = face(id),
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) colors.ink else colors.sheet).clickable { model.setLook(nextFont = id) }.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            Choice("الحجم", listOf("sm" to "صغير", "md" to "وسط", "lg" to "كبير"), model.size, colors, font) { model.setLook(nextSize = it) }
            Choice("اللون", listOf("dark" to "داكن", "light" to "فاتح"), model.theme, colors, font) { model.setLook(nextTheme = it) }
        }
    }
}

@Composable
private fun SummarySettings(model: MawjizModel, colors: Paint, font: FontFamily) {
    var draftKey by remember { mutableStateOf(model.key) }
    var draftModel by remember { mutableStateOf(model.modelName) }
    val fields = fieldColors(colors)
    Box(Modifier.fillMaxSize()) { val padding = PaddingValues(0.dp)
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SubHead("التلخيص", colors, font) { model.backSettings() }
            OutlinedTextField(draftKey, { draftKey = it }, label = { Text("مفتاح Gemini") }, modifier = Modifier.fillMaxWidth(), colors = fields, singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(draftModel, { draftModel = it }, label = { Text("النموذج") }, modifier = Modifier.fillMaxWidth(), colors = fields, singleLine = true)
            TextButton(onClick = { model.testKey(draftKey, draftModel) }) { Text("اختبار المفتاح", color = colors.ink, fontFamily = font) }
            SaveButton(colors, font) { model.savePrefs(draftKey, draftModel, model.morning, model.evening) }
            Note(model, colors, font)
        }
    }
}

@Composable
private fun SubHead(title: String, colors: Paint, font: FontFamily, onBack: () -> Unit) {
    TextButton(onClick = onBack) { Text("رجوع", color = colors.muted, fontFamily = font) }
    Text(title, color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun MenuRow(title: String, meta: String, colors: Paint, font: FontFamily, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = colors.ink, fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 17.sp)
                Text(meta, color = colors.muted, fontSize = 13.sp, fontFamily = font)
            }
            Text("‹", color = colors.muted, fontFamily = font, fontSize = 22.sp)
        }
    }
    HorizontalDivider(color = colors.line)
}

@Composable
private fun SaveButton(colors: Paint, font: FontFamily, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color(0xFFF4EFE6))) {
        Text("حفظ", fontFamily = font)
    }
}

@Composable
private fun Note(model: MawjizModel, colors: Paint, font: FontFamily) {
    if (model.note.isBlank()) return
    Spacer(Modifier.height(8.dp))
    Text(model.note, color = colors.muted, fontSize = 13.sp, fontFamily = font)
}

@Composable
private fun fieldColors(colors: Paint) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = colors.ink, unfocusedTextColor = colors.ink,
    focusedBorderColor = Teal, unfocusedBorderColor = colors.line,
    cursorColor = colors.ink, focusedLabelColor = colors.muted, unfocusedLabelColor = colors.muted,
)

@Composable
private fun Check(label: String, checked: Boolean, colors: Paint, font: FontFamily, mark: Color? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mark != null) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(mark))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, color = colors.ink, fontFamily = font)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = colors.ink))
    }
}

@Composable
private fun Choice(title: String, options: List<Pair<String, String>>, value: String, colors: Paint, font: FontFamily, onPick: (String) -> Unit) {
    Text(title, color = colors.muted, fontSize = 13.sp, fontFamily = font, modifier = Modifier.padding(top = 8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            val on = id == value
            Text(
                label,
                color = if (on) colors.paper else colors.ink,
                fontFamily = font,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) colors.ink else colors.sheet).clickable { onPick(id) }.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}


private fun playRefresh(context: Context) {
    val player = MediaPlayer.create(context, R.raw.refresh_chirp) ?: return
    player.setOnCompletionListener { it.release() }
    player.start()
}

@Composable
private fun BottomBar(model: MawjizModel, colors: Paint, font: FontFamily) {
    Row(Modifier.fillMaxWidth().background(colors.sheet).padding(vertical = 8.dp)) {
        BarItem("الموجز", model.tab == "feed", colors, font, Modifier.weight(1f)) { model.showFeed() }
        BarItem("محفوظ", model.tab == "saved", colors, font, Modifier.weight(1f)) { model.showSaved() }
        BarItem("الإعدادات", model.tab == "settings", colors, font, Modifier.weight(1f)) { model.openSettings() }
    }
}

@Composable
private fun BarItem(label: String, on: Boolean, colors: Paint, font: FontFamily, modifier: Modifier, click: () -> Unit) {
    Text(
        label,
        color = if (on) colors.ink else colors.muted,
        fontFamily = font,
        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
        fontSize = 14.sp,
        modifier = modifier.clickable(onClick = click).padding(vertical = 10.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun SavedScreen(model: MawjizModel, colors: Paint, font: FontFamily, body: TextUnit, context: Context) {
    val items = model.stories.filter { it.saved }.sortedByDescending { it.at }
    Column(Modifier.fillMaxSize()) {
        Text("محفوظ", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font, modifier = Modifier.padding(16.dp))
        if (items.isEmpty()) {
            Text("لا محفوظات بعد.", color = colors.muted, fontFamily = font, modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            LazyColumn {
                items(items, key = { it.id }) { story ->
                    StoryRow(story, colors, font, body, { model.openStory(story) }, { model.toggleSaved(story) }, { shareStory(context, model.shareText(story)) })
                    HorizontalDivider(color = colors.line)
                }
            }
        }
    }
}

@Composable
private fun ReaderScreen(model: MawjizModel, colors: Paint, font: FontFamily, body: TextUnit, context: Context) {
    val story = model.reading ?: return
    val shown = model.readingMore.ifBlank { story.detail.ifBlank { story.text } }
    Column(Modifier.fillMaxSize().background(colors.paper).verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = { model.closeStory() }) { Text("رجوع", color = colors.muted, fontFamily = font) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            story.desks.take(2).forEach { id ->
                val desk = Catalog.desk(id) ?: return@forEach
                Text(desk.label, color = Color(desk.color.toInt()), fontWeight = FontWeight.Bold, fontFamily = font, modifier = Modifier.padding(end = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(shown, color = colors.ink, fontSize = body, lineHeight = body * 1.55, fontFamily = font)
        if (model.readingBusy) {
            Spacer(Modifier.height(12.dp))
            Text("يجلب بقية النص…", color = colors.muted, fontSize = 13.sp, fontFamily = font)
        }
        story.sources.firstOrNull { it.outlet.isNotBlank() }?.let { source ->
            Spacer(Modifier.height(16.dp))
            Text(source.outlet, color = colors.muted, fontSize = 12.sp, fontFamily = font)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (story.saved) "محفوظ" else "حفظ", color = colors.ink, fontFamily = font, modifier = Modifier.clickable { model.toggleSaved(story) })
            Text("مشاركة", color = colors.ink, fontFamily = font, modifier = Modifier.clickable { shareStory(context, model.shareText(story)) })
        }
    }
}

private fun shareStory(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(send, "مشاركة")) }
}
