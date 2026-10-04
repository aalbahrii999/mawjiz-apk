package app.mawjiz

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val Paper = Color(0xFF10141B)
private val Sheet = Color(0xFF1C2422)
private val Ink = Color(0xFFF4EFE6)
private val Muted = Color(0xFF9AA39E)
private val Line = Color(0xFF2C3532)
private val Teal = Color(0xFF1B5E56)

@Composable
fun MawjizApp(model: MawjizModel = viewModel()) {
    if (model.showSettings) SettingsScreen(model) else TimelineScreen(model)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimelineScreen(model: MawjizModel) {
    val visible = remember(model.stories, model.selected, model.enabled) {
        model.stories.filter { story ->
            story.desks.any { model.enabled[it] == true } &&
                (model.selected.isEmpty() || story.desks.any { it in model.selected })
        }
    }
    val chips = model.order.mapNotNull { id ->
        Catalog.byId(id)?.takeIf { model.enabled[id] == true }
    }
    Scaffold(containerColor = Paper) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("موجز", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { model.showSettings = true }) {
                    Text("الإعدادات", color = Muted)
                }
            }
            ChipRow(chips, model.selected, model::toggleDesk, model::showAll, model::moveDesk)
            if (model.note.isNotBlank()) {
                Text(model.note, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            PullToRefreshBox(
                isRefreshing = model.loading,
                onRefresh = model::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (visible.isEmpty() && !model.loading) {
                    Text("لا مادة هنا.", color = Ink, modifier = Modifier.padding(24.dp))
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                        items(visible, key = { it.id }) { story ->
                            StoryRow(story)
                            HorizontalDivider(color = Line)
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
    onToggle: (String) -> Unit,
    onAll: () -> Unit,
    onMove: (String, String) -> Unit,
) {
    val scroll = rememberScrollState()
    val centers = remember { mutableMapOf<String, Float>() }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip("الكل", selected.isEmpty(), null, null, onAll)
        chips.forEach { desk ->
            Chip(
                label = desk.label,
                on = desk.id in selected,
                color = Color(desk.color.toInt()),
                onToggle = { onToggle(desk.id) },
                onDrag = { x ->
                    val target = centers.entries.filter { it.key != desk.id }.minByOrNull { abs(it.value - x) }?.key
                    if (target != null) onMove(desk.id, target)
                },
                onPlace = { center -> centers[desk.id] = center },
            )
        }
    }
}

@Composable
private fun Chip(
    label: String,
    on: Boolean,
    color: Color?,
    onToggle: (() -> Unit)?,
    onClick: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onPlace: ((Float) -> Unit)? = null,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val background = if (on) Ink else Sheet
    val foreground = if (on) Paper else Ink
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .onGloballyPositioned {
                coords = it
                onPlace?.invoke(it.boundsInWindow().center.x)
            }
            .pointerInput(label) {
                if (onToggle != null) detectTapGestures(onTap = { onToggle() })
                if (onClick != null) detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(label) {
                if (onDrag == null) return@pointerInput
                detectDragGesturesAfterLongPress(onDrag = { change, _ ->
                    val window = coords?.localToWindow(change.position) ?: return@detectDragGesturesAfterLongPress
                    onDrag(window.x)
                })
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = foreground, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun StoryRow(story: Story) {
    val context = LocalContext.current
    val stamp = remember(story.at) { formatStamp(story.at) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            story.desks.take(3).forEach { id ->
                val desk = Catalog.byId(id) ?: return@forEach
                Text(desk.label, color = Color(desk.color.toInt()), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(stamp, color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(story.text, color = Ink, fontSize = 16.sp, lineHeight = 24.sp)
        if (story.sources.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                story.sources.filter { it.outlet.isNotBlank() && it.outlet != "اخبار" }.forEach { source ->
                    Text(
                        source.outlet,
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                            runCatching { context.startActivity(intent) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: MawjizModel) {
    var draftKey by remember { mutableStateOf(model.key) }
    var draftModel by remember { mutableStateOf(model.model) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
        focusedBorderColor = Teal,
        unfocusedBorderColor = Line,
        cursorColor = Ink,
        focusedLabelColor = Muted,
        unfocusedLabelColor = Muted,
    )
    Scaffold(containerColor = Paper) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                TextButton(onClick = { model.showSettings = false }) { Text("رجوع", color = Muted) }
                Text("الإعدادات", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("التصنيفات", color = Muted, fontSize = 13.sp)
            }
            items(model.order) { id ->
                val desk = Catalog.byId(id) ?: return@items
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(desk.label, color = Color(desk.color.toInt()), fontSize = 16.sp)
                    Switch(
                        checked = model.enabled[id] == true,
                        onCheckedChange = { model.setEnabled(id, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Teal, checkedThumbColor = Ink),
                    )
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draftKey,
                    onValueChange = { draftKey = it },
                    label = { Text("مفتاح Gemini") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draftModel,
                    onValueChange = { draftModel = it },
                    label = { Text("النموذج") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            model.saveKey(draftKey, draftModel)
                            model.note = "حُفظ المفتاح على الجهاز."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Ink),
                    ) { Text("حفظ") }
                    TextButton(onClick = { model.testKey(draftKey, draftModel) }) {
                        Text("اختبار المفتاح", color = Ink)
                    }
                }
                if (model.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(model.note, color = Muted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("اسحب التايملاين للتحديث. اضغط مطولًا على التصنيف ثم اسحبه لتغيير ترتيبه. اضغطه مرة ليُظهر أخباره مع غيره.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}

private fun formatStamp(at: Long): String {
    val format = SimpleDateFormat("d MMM · h:mm a", Locale("ar"))
    format.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
    return format.format(Date(at))
}
