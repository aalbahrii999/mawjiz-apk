package app.mawjiz

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
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
    if (model.settingsOpen) SettingsScreen(model, colors, font) else TimelineScreen(model, colors, font, body)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineScreen(model: MawjizModel, colors: Paint, font: FontFamily, body: TextUnit) {
    val visible = model.stories.filter { story ->
        story.desks.any { model.enabled[it] == true } &&
            (model.selected.isEmpty() || story.desks.any { it in model.selected })
    }
    val chips = model.order.mapNotNull { id -> Catalog.desk(id)?.takeIf { model.enabled[id] == true } }
    val context = LocalContext.current
    Scaffold(containerColor = colors.paper) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("موجز", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
                TextButton(onClick = { model.settingsOpen = true }) { Text("الإعدادات", color = colors.muted, fontFamily = font) }
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
                            StoryRow(story, colors, font, body)
                            HorizontalDivider(color = colors.line)
                        }
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
private fun StoryRow(story: Story, colors: Paint, font: FontFamily, body: TextUnit) {
    val context = LocalContext.current
    val stamp = remember(story.at) {
        val format = SimpleDateFormat("d MMM · h:mm a", Locale("ar"))
        format.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        format.format(Date(story.at))
    }
    val lead = Catalog.desk(story.desks.firstOrNull().orEmpty())
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                Text(
                    source.outlet,
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontFamily = font,
                    modifier = Modifier.clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: MawjizModel, colors: Paint, font: FontFamily) {
    var draftKey by remember { mutableStateOf(model.key) }
    var draftModel by remember { mutableStateOf(model.modelName) }
    var draftMorning by remember { mutableStateOf(model.morning) }
    var draftEvening by remember { mutableStateOf(model.evening) }
    val fields = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.ink, unfocusedTextColor = colors.ink,
        focusedBorderColor = Teal, unfocusedBorderColor = colors.line,
        cursorColor = colors.ink, focusedLabelColor = colors.muted, unfocusedLabelColor = colors.muted,
    )
    Scaffold(containerColor = colors.paper) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                TextButton(onClick = { model.settingsOpen = false }) { Text("رجوع", color = colors.muted, fontFamily = font) }
                Text("الإعدادات", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = font)
                Section("التصنيفات", colors, font)
            }
            Catalog.groups.forEach { group ->
                item { Text(group.title, color = colors.muted, fontSize = 12.sp, fontFamily = font, modifier = Modifier.padding(top = 8.dp)) }
                items(group.ids, key = { "desk-$it" }) { id ->
                    val desk = Catalog.desk(id) ?: return@items
                    Check(desk.label, model.enabled[id] == true, colors, font, Color(desk.color.toInt())) { model.setDesk(id, it) }
                }
            }
            item { Section("المصادر", colors, font) }
            Catalog.outletGroups.forEach { (group, title) ->
                val outlets = Catalog.outlets.filter { it.group == group }
                item { Text(title, color = colors.muted, fontSize = 12.sp, fontFamily = font, modifier = Modifier.padding(top = 8.dp)) }
                items(outlets, key = { it.id }) { outlet ->
                    Check(outlet.label, outlet.id in model.sources, colors, font) { model.setSource(outlet.id, it) }
                }
            }
            item {
                Section("النشرة", colors, font)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draftMorning, { draftMorning = it }, label = { Text("الصباح") }, modifier = Modifier.weight(1f), colors = fields, singleLine = true)
                    OutlinedTextField(draftEvening, { draftEvening = it }, label = { Text("المساء") }, modifier = Modifier.weight(1f), colors = fields, singleLine = true)
                }
                Section("المظهر", colors, font)
                Text("الخط", color = colors.muted, fontSize = 12.sp, fontFamily = font)
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cairo" to "القاهرة", "naskh" to "نسخ", "plex" to "بليكس").forEach { (id, label) ->
                        val on = model.font == id
                        Text(
                            label,
                            color = if (on) colors.paper else colors.ink,
                            fontFamily = face(id),
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) colors.ink else colors.sheet).clickable { model.setLook(nextFont = id) }.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
                Choice("الحجم", listOf("sm" to "صغير", "md" to "وسط", "lg" to "كبير"), model.size, colors, font) { model.setLook(nextSize = it) }
                Choice("اللون", listOf("dark" to "داكن", "light" to "فاتح"), model.theme, colors, font) { model.setLook(nextTheme = it) }
                Section("التلخيص", colors, font)
                OutlinedTextField(draftKey, { draftKey = it }, label = { Text("مفتاح Gemini") }, modifier = Modifier.fillMaxWidth(), colors = fields, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(draftModel, { draftModel = it }, label = { Text("النموذج") }, modifier = Modifier.fillMaxWidth(), colors = fields, singleLine = true)
                TextButton(onClick = { model.testKey(draftKey, draftModel) }) { Text("اختبار المفتاح", color = colors.ink, fontFamily = font) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { model.savePrefs(draftKey, draftModel, draftMorning, draftEvening) },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color(0xFFF4EFE6)),
                ) { Text("حفظ", fontFamily = font) }
                if (model.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(model.note, color = colors.muted, fontSize = 13.sp, fontFamily = font)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, colors: Paint, font: FontFamily) {
    Text(title, color = colors.ink, fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
}

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
