package com.relentnet.cuttracker.ui.route

import com.relentnet.cuttracker.ui.components.rememberPhoto
import com.relentnet.cuttracker.ui.components.num
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import android.app.DatePickerDialog
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Fab
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.NumRow
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetField
import com.relentnet.cuttracker.ui.components.SheetLabel
import com.relentnet.cuttracker.ui.components.pressable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val cpFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CheckpointsScreen(vm: AppViewModel, onBack: () -> Unit, onShoot: () -> Unit, onRoute: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    val due = ProgramEngine.photoDue(st.photos.map { it.date }, st.today, s.startDate)
    val current = currentCheckpointDate(st)
    var confirmDelete by remember { mutableStateOf<LocalDate?>(null) }
    // Right-hand photo of the compare pair; the left is always the first one.
    var compare by remember { mutableStateOf<LocalDate?>(null) }

    var editing by remember { mutableStateOf<String?>(null) }
    val haptics2 = LocalHaptics.current
    Box(Modifier.fillMaxSize()) {
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Back", title = "Checkpoints", onBack = onBack)
        Sub("Where the route checks in.", size = 12)
        if (due) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.accent).padding(16.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(CutIcons.Camera, null, Modifier.size(26.dp), tint = c.onAccent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Progress photo due today", color = c.onAccent, style = cutStyle(15, 800, 1.1f))
                    Text("Same mirror, same light. Stored on device only. +${ProgramSpec.XP_PHOTO} xp", color = c.onAccent.copy(alpha = 0.8f), style = cutStyle(12, 600, 1.2f))
                }
                Box(
                    Modifier.height(36.dp).clip(RoundedCornerShape(8.dp)).background(c.ground).clickable(onClick = onShoot).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("SHOOT", color = c.text, style = cutStyle(12, 800, 1f, 0.06f)) }
            }
        } else if (st.photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val sorted = st.photos.sortedBy { it.date }
                if (sorted.size >= 2) {
                    val first = sorted.first()
                    val right = sorted.firstOrNull { it.date == compare } ?: sorted.last()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((p, label) in listOf(first to "Day 1", right to "Day ${ProgramEngine.dayNumber(right.date, s.startDate)}")) {
                            val bmp = rememberPhoto(p.path, 4)
                            Box(Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp)).background(c.surface)) {
                                if (bmp != null) Image(bmp, null, Modifier.fillMaxWidth().aspectRatio(3f / 4f), contentScale = ContentScale.Crop)
                                Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                                    Text(label.uppercase(), color = c.text, style = cutStyle(11, 800, 1f, 0.08f))
                                    Text(p.date.format(cpFmt), color = c.text2, style = cutStyle(11, 600, 1f))
                                }
                            }
                        }
                    }
                    Sub("First photo against the latest. Tap any photo below to put it on the right.", size = 12)
                }
                Sub("Next one in ${ProgramSpec.PHOTO_INTERVAL_DAYS - ChronoUnit.DAYS.between(st.photos.maxOf { it.date }, st.today)} days. Hold one to delete.", size = 12)
                for (row in st.photos.sortedByDescending { it.date }.chunked(4)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (p in row) {
                            val bmp = rememberPhoto(p.path, 8)
                            Box(
                                Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp)).background(c.surface)
                                    .combinedClickable(onClick = { compare = p.date }, onLongClick = { haptics.tap(); confirmDelete = p.date }),
                            ) {
                                if (bmp != null) Image(bmp, null, Modifier.fillMaxWidth().aspectRatio(3f / 4f), contentScale = ContentScale.Crop)
                                Text(p.date.format(cpFmt), Modifier.align(Alignment.BottomStart).padding(6.dp), color = c.text, style = cutStyle(10, 700, 1f))
                            }
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
                confirmDelete?.let { d ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Sub("Delete the ${d.format(cpFmt)} photo?", Modifier.weight(1f), size = 12)
                        Pill("Delete", on = true, onClick = { vm.deletePhoto(d); confirmDelete = null })
                        Pill("Keep", on = false, onClick = { confirmDelete = null })
                    }
                }
            }
        }
        val cps = s.checkpoints.sortedBy { it.date }
        KindCard("Flags", "${cps.count { !it.free }}") {
            for (cp in cps) {
                val week = ProgramEngine.checkpointWeek(cp, s.startDate, s.lengthWeeks)
                val (state, avg) = checkpointState(st, cp.localDate, cp.target)
                val isCurrent = !cp.free && cp.localDate == current
                val dateLabel = if (cp.days > 1) "${cp.localDate.format(cpFmt)} to ${cp.lastDate.dayOfMonth}" else cp.localDate.format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.US))
                val (right, rc) = when {
                    cp.free -> (if (cp.days > 1) "skip days" else "skip day") to c.text2
                    state == CpState.HIT -> "hit at ${ProgramEngine.fmt1(avg!!)}" to c.good
                    state == CpState.PASSED -> (if (avg != null) "was ${ProgramEngine.fmt1(avg)}" else "passed") to c.text2
                    isCurrent -> "in ${ChronoUnit.DAYS.between(st.today, cp.localDate)} days" to c.accent
                    else -> "week $week" to c.text2
                }
                NumRow(
                    if (cp.free) "–" else ProgramEngine.fmt1(cp.target ?: 0.0), dateLabel, unit = if (cp.free) null else "lb",
                    sub = cp.note.ifBlank { null }, right = right, rightColor = rc,
                    onClick = { editing = cp.date },
                )
            }
        }
        Sub("Tap one to change the date, the weight or the note.", size = 12)
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).pressable(haptic = false, onClick = onRoute).padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sub("Days off are planned from the calendar on Route.", size = 12)
            Text("ROUTE ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
        }
    }
    Fab(Modifier.align(Alignment.BottomEnd)) {
        val last = s.checkpoints.filter { !it.free }.maxOfOrNull { it.localDate } ?: st.today
        val d = last.plusDays(14).let { if (it.isAfter(s.endDate.minusDays(1))) s.endDate.minusDays(1) else it }
        // The date is a checkpoint's identity, so a day that already has a flag opens that flag instead of stacking a second one.
        val taken = s.checkpoints.firstOrNull { !it.free && it.localDate == d }
        if (taken != null) { editing = taken.date; return@Fab }
        val ncp = Checkpoint(d.toString(), ProgramEngine.paceWeight(ProgramEngine.dayNumber(d, s.startDate), s, ProgramEngine.paceAnchor(st.weighIns, s)).let { kotlin.math.round(it * 2) / 2 }, note = "Pace check.")
        vm.updateSettings { s -> s.copy(checkpoints = (s.checkpoints + ncp).sortedBy { it.date }) }
        editing = ncp.date
    }
    }
    editing?.let { id ->
        val cp = s.checkpoints.firstOrNull { it.date == id }
        if (cp == null) editing = null
        else CheckpointSheet(
            cp, onDismiss = { editing = null },
            onSave = { ncp ->
                // A date is a flag's identity: moving onto a day that has one is refused, not stacked.
                if (ncp.date != cp.date && s.checkpoints.any { it.date == ncp.date }) vm.showToast("${ncp.localDate.format(cpFmt)} already has a flag")
                else { vm.updateSettings { s -> s.copy(checkpoints = s.checkpoints.map { if (it.date == cp.date) ncp else it }.sortedBy { it.date }) }; editing = null }
            },
            onRemove = { haptics2.tap(); vm.updateWithUndo("Flag removed") { s -> s.copy(checkpoints = s.checkpoints.filter { it.date != cp.date }) }; editing = null },
        )
    }
}

/** Date, target, note. A skip only offers Unskip. */
@Composable
private fun CheckpointSheet(cp: Checkpoint, onDismiss: () -> Unit, onSave: (Checkpoint) -> Unit, onRemove: () -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    var date by remember(cp.date) { mutableStateOf(cp.localDate) }
    var target by remember(cp.date) { mutableStateOf(cp.target?.let { ProgramEngine.fmt1(it) } ?: "") }
    var note by remember(cp.date) { mutableStateOf(cp.note) }
    Sheet(if (cp.free) "Planned skip" else "Edit checkpoint", onDismiss) {
        if (cp.free) {
            Sub("A planned skip. Chain kept, no freeze spent, not a Send.", size = 12)
            SheetButtons("Unskip", onPrimary = onRemove, secondary = "Keep", onSecondary = onDismiss)
            return@Sheet
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SheetLabel("Date")
                val shape = RoundedCornerShape(12.dp)
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).clickable(role = Role.Button) {
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                    }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { Text(date.format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)), color = c.text, style = cutStyle(16, 700, 1f, tabular = true)) }
            }
            SheetField("Target", target, { target = it }, Modifier.width(130.dp), unit = "lb")
        }
        SheetField("Note", note, { note = it }, text = true, last = true, placeholder = "Optional")
        SheetButtons("Done", onPrimary = { onSave(cp.copy(date = date.toString(), target = target.num() ?: cp.target, note = note.trim())) }, secondary = "Remove", onSecondary = onRemove)
    }
}
