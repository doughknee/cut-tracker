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

    Screen(bottomInset = true) {
        BackHeader(backLabel = "Route", title = "Checkpoints", onBack = onBack)
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
                Sub("Photos. Next one in ${ProgramSpec.PHOTO_INTERVAL_DAYS - ChronoUnit.DAYS.between(st.photos.maxOf { it.date }, st.today)} days. Hold one to delete.", size = 12)
                for (row in st.photos.sortedByDescending { it.date }.chunked(4)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (p in row) {
                            val bmp = remember(p.path) {
                                runCatching { BitmapFactory.decodeFile(p.path, BitmapFactory.Options().apply { inSampleSize = 8 }) }.getOrNull()
                            }
                            Box(
                                Modifier.weight(1f).aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp)).background(c.surface)
                                    .combinedClickable(onClick = {}, onLongClick = { haptics.tap(); confirmDelete = p.date }),
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
        if (s.phase == 2) Sub("Phase 2: build calories and the extra lifts until the route ends. Photos keep their 14-day cadence.")
        Column {
            val cps = s.checkpoints.sortedBy { it.date }
            cps.forEachIndexed { i, cp -> CheckpointRow(cp, st, current, last = i == cps.lastIndex, onStartPhase2 = { vm.setPhase(2) }) }
        }
    }
}

@Composable
private fun CheckpointRow(cp: Checkpoint, st: com.relentnet.cuttracker.data.AppState, current: LocalDate?, last: Boolean, onStartPhase2: () -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val week = ProgramEngine.checkpointWeek(cp, s.startDate, s.lengthWeeks)
    val (state, avg) = checkpointState(st, cp.localDate, cp.target)
    val isCurrent = !cp.free && cp.localDate == current
    val reached = !st.today.isBefore(cp.localDate)
    val dateLabel = if (cp.days > 1) "${cp.localDate.format(cpFmt)} to ${cp.lastDate.dayOfMonth}" else cp.localDate.format(cpFmt)
    val title = (cp.label ?: "Week $week") + " · " + dateLabel
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
                    cp.free -> (if (cp.days > 1) "Free days" else "Free day") to c.text2
                    state == CpState.HIT -> "${ProgramEngine.fmt1(avg!!)} hit" to c.good
                    state == CpState.PASSED -> (if (avg != null) "${ProgramEngine.fmt1(avg)} · aimed ${ProgramEngine.fmt1(cp.target ?: 0.0)}" else ProgramEngine.fmt1(cp.target ?: 0.0)) to c.text2
                    isCurrent -> "${ProgramEngine.fmt1(cp.target ?: 0.0)} · ${ChronoUnit.DAYS.between(st.today, cp.localDate)} days" to c.accent
                    else -> ProgramEngine.fmt1(cp.target ?: 0.0) to c.text2
                }
                Text(right, color = rc, style = cutStyle(12, 700, 1f, tabular = true))
            }
            Sub(cp.note, size = 13)
            val isFinal = cp.localDate == s.checkpoints.filter { !it.free }.maxOfOrNull { it.localDate }
            if (isFinal && reached && s.phase == 1) {
                BigButton("Start Phase 2", onClick = onStartPhase2, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), height = 48.dp, fontSize = 13)
            }
        }
    }
}
