package com.relentnet.cuttracker.ui.route

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.relentnet.cuttracker.ui.components.Field
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
fun CheckpointsScreen(vm: AppViewModel, onBack: () -> Unit, onShoot: () -> Unit) {
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
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Back", title = "Checkpoints", onBack = onBack)
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
                            val bmp = remember(p.path) { runCatching { BitmapFactory.decodeFile(p.path, BitmapFactory.Options().apply { inSampleSize = 4 }) }.getOrNull() }
                            Box(Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp)).background(c.surface)) {
                                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(3f / 4f), contentScale = ContentScale.Crop)
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
                            val bmp = remember(p.path) {
                                runCatching { BitmapFactory.decodeFile(p.path, BitmapFactory.Options().apply { inSampleSize = 8 }) }.getOrNull()
                            }
                            Box(
                                Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp)).background(c.surface)
                                    .combinedClickable(onClick = { compare = p.date }, onLongClick = { haptics.tap(); confirmDelete = p.date }),
                            ) {
                                if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxWidth().aspectRatio(3f / 4f), contentScale = ContentScale.Crop)
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
        Sub("Targets on the pace line. Tap one to change the date, the weight or the note. Days off are planned from the calendar on Route.", size = 12)
        Column {
            val cps = s.checkpoints.sortedBy { it.date }
            cps.forEachIndexed { i, cp ->
                Column(Modifier.animateContentSize(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))) {
                    Box(Modifier.pressable(haptic = false) { editing = if (editing == cp.date) null else cp.date }) {
                        CheckpointRow(cp, st, current, last = i == cps.lastIndex && editing != cp.date)
                    }
                    if (editing == cp.date) CheckpointEditor(
                        cp, last = i == cps.lastIndex,
                        onSave = { ncp -> vm.saveSettings(s.copy(checkpoints = s.checkpoints.map { if (it.date == cp.date) ncp else it }.sortedBy { it.date })); editing = null },
                        onRemove = { haptics2.tap(); vm.saveSettings(s.copy(checkpoints = s.checkpoints.filter { it.date != cp.date })); editing = null },
                    )
                }
            }
        }
        Text(
            "+ ADD A CHECKPOINT", color = c.text2, style = cutStyle(11, 800, 1f, 0.1f),
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) {
                val last = s.checkpoints.filter { !it.free }.maxOfOrNull { it.localDate } ?: st.today
                val d = last.plusDays(14).let { if (it.isAfter(s.endDate.minusDays(1))) s.endDate.minusDays(1) else it }
                val ncp = Checkpoint(d.toString(), ProgramEngine.paceWeight(ProgramEngine.dayNumber(d, s.startDate), s, ProgramEngine.paceAnchor(st.weighIns, s)).let { kotlin.math.round(it * 2) / 2 }, note = "Pace check.")
                vm.saveSettings(s.copy(checkpoints = (s.checkpoints + ncp).sortedBy { it.date }))
                editing = ncp.date
            }.padding(horizontal = 4.dp, vertical = 8.dp),
        )
    }
}

/** Unfolds under a checkpoint row: date, target, note. A skip only offers Unskip. */
@Composable
private fun CheckpointEditor(cp: Checkpoint, last: Boolean, onSave: (Checkpoint) -> Unit, onRemove: () -> Unit) {
    val c = CutTheme.colors
    val context = LocalContext.current
    var date by remember(cp.date) { mutableStateOf(cp.localDate) }
    var target by remember(cp.date) { mutableStateOf(cp.target?.let { ProgramEngine.fmt1(it) } ?: "") }
    var note by remember(cp.date) { mutableStateOf(cp.note) }
    Column(
        Modifier.fillMaxWidth().padding(start = 40.dp, bottom = if (last) 0.dp else 18.dp).clip(RoundedCornerShape(10.dp)).background(c.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (cp.free) {
            Sub("A planned skip. Chain kept, no freeze spent, not a Send.", size = 12)
            BigButton("Unskip", onClick = onRemove, modifier = Modifier.fillMaxWidth(), secondary = true, height = 40.dp, fontSize = 12)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier.weight(1f).height(40.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).clickable(role = Role.Button) {
                    DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(date.format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)), color = c.text, style = cutStyle(14, 700, 1f, tabular = true)) }
            Field(target, { target = it }, Modifier.width(76.dp), height = 40.dp, fontSize = 14, weight = 700, center = true, borderWidth = 1.dp, radius = 8.dp, padding = 6.dp, imeAction = ImeAction.Next)
            Sub("lb", size = 11)
        }
        Field(note, { note = it }, Modifier.fillMaxWidth(), height = 40.dp, fontSize = 14, weight = 700, keyboard = KeyboardType.Text, imeAction = ImeAction.Done, borderWidth = 1.dp, radius = 8.dp, padding = 10.dp, placeholder = "Note")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton("Done", onClick = { onSave(cp.copy(date = date.toString(), target = target.toDoubleOrNull() ?: cp.target, note = note.trim())) }, modifier = Modifier.weight(1f), height = 40.dp, fontSize = 12, confirm = true)
            BigButton("Remove", onClick = onRemove, modifier = Modifier.width(110.dp), secondary = true, height = 40.dp, fontSize = 12)
        }
    }
}

@Composable
private fun CheckpointRow(cp: Checkpoint, st: com.relentnet.cuttracker.data.AppState, current: LocalDate?, last: Boolean) {
    val c = CutTheme.colors
    val s = st.settings
    val week = ProgramEngine.checkpointWeek(cp, s.startDate, s.lengthWeeks)
    val (state, avg) = checkpointState(st, cp.localDate, cp.target)
    val isCurrent = !cp.free && cp.localDate == current
    val reached = !st.today.isBefore(cp.localDate)
    val dateLabel = if (cp.days > 1) "${cp.localDate.format(cpFmt)} to ${cp.lastDate.dayOfMonth}" else cp.localDate.format(cpFmt)
    val title = (cp.label ?: if (cp.free) "Skip" else "Week $week") + " · " + dateLabel
    val titleColor = if (state == CpState.HIT || isCurrent) c.text else c.text2
    Row(Modifier.fillMaxWidth().padding(bottom = if (last) 0.dp else 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.width(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(13.dp))
                    .background(if (state == CpState.HIT) c.good else c.ground)
                    .border(if (isCurrent) 3.dp else 2.dp, when { state == CpState.HIT -> c.good; isCurrent -> c.accent; else -> c.mutedBorder }, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { if (state == CpState.HIT) Icon(CutIcons.Check, null, Modifier.size(14.dp), tint = c.ground) }
            if (!last) Box(Modifier.width(2.dp).height(40.dp).padding(top = 4.dp).background(c.hairline))
        }
        Column(Modifier.weight(1f).padding(top = 3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = titleColor, style = cutStyle(16, 700, 1.1f))
                val (right, rc) = when {
                    cp.free -> (if (cp.days > 1) "Skip days" else "Skip day") to c.text2
                    state == CpState.HIT -> "${ProgramEngine.fmt1(avg!!)} hit" to c.good
                    state == CpState.PASSED -> (if (avg != null) "${ProgramEngine.fmt1(avg)} · aimed ${ProgramEngine.fmt1(cp.target ?: 0.0)}" else ProgramEngine.fmt1(cp.target ?: 0.0)) to c.text2
                    isCurrent -> "${ProgramEngine.fmt1(cp.target ?: 0.0)} · ${ChronoUnit.DAYS.between(st.today, cp.localDate)} days" to c.accent
                    else -> ProgramEngine.fmt1(cp.target ?: 0.0) to c.text2
                }
                Text(right, color = rc, style = cutStyle(12, 700, 1f, tabular = true))
            }
            Sub(cp.note, size = 13)
        }
    }
}
