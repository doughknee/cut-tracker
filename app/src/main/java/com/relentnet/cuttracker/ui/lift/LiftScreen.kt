package com.relentnet.cuttracker.ui.lift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.Exercise
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Repo
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.H1
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.delay

private data class Entry(val weight: Double, val reps: Int)

@Composable
fun LiftScreen(vm: AppViewModel) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    val blocks: List<Block> = when (st.mode) {
        DayMode.CLIMB -> listOf(Block.CLIMB)
        DayMode.OFF -> listOf(Block.OFF)
        DayMode.REST, null -> listOf(Block.CLIMB, Block.OFF)
    }
    val exercises = blocks.flatMap { ProgramEngine.exercisesFor(it, s) }
    val last = remember(st.liftSets, st.today) { exercises.associate { it.id to ProgramEngine.lastSession(st.liftSets, it.id, st.today) } }
    val values = remember { mutableStateMapOf<String, Entry>() }
    // Prefill from the last session; a fresh exercise starts at 0 lb × 10 reps.
    LaunchedEffect(last, st.today) {
        for (e in exercises) if (e.id !in values) {
            val l = last[e.id]
            values[e.id] = Entry(l?.first ?: 0.0, l?.second ?: 10)
        }
    }
    val liveWon = exercises.any { e -> values[e.id]?.let { v -> ProgramEngine.beat(v.weight to v.reps, last[e.id]) } == true }
    val won = liveWon || ProgramEngine.weekWon(st.liftSets, st.today)

    Screen {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(
                    when (st.mode) {
                        DayMode.CLIMB -> "Climb day block · Week ${st.weekN}"
                        DayMode.OFF -> "Off day block · Week ${st.weekN}"
                        DayMode.REST -> "Rest day · Week ${st.weekN}"
                        null -> "Both blocks · Week ${st.weekN}"
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (won) c.good else c.text2))
                    Text(if (won) "WEEK WON" else "NOT YET", color = if (won) c.good else c.text2, style = cutStyle(11, 700, 1f, 0.08f))
                }
            }
            H1("Lift")
        }
        RestTimer()
        for (b in blocks) {
            if (blocks.size > 1) Eyebrow(if (b == Block.CLIMB) "Climb day block" else "Off day block")
            for (e in ProgramEngine.exercisesFor(b, s)) {
                val v = values[e.id] ?: Entry(last[e.id]?.first ?: 0.0, last[e.id]?.second ?: 10)
                ExerciseCard(
                    e, v, last[e.id], sets = ProgramEngine.setsFor(e, s.phase),
                    onChange = { values[e.id] = it; haptics.tick() },
                )
            }
        }
        BigButton("Save session", modifier = Modifier.fillMaxWidth(), onClick = {
            haptics.pattern(0, 20)
            vm.saveLiftSession(exercises.map { e ->
                val v = values[e.id] ?: Entry(0.0, 10)
                Repo.LiftEntry(e.id, v.weight, v.reps, ProgramEngine.setsFor(e, s.phase))
            })
        })
    }
}

@Composable
private fun ExerciseCard(e: Exercise, v: Entry, last: Pair<Double, Int>?, sets: Int, onChange: (Entry) -> Unit) {
    val c = CutTheme.colors
    val bw = e.step <= 0.0
    val upW = last != null && v.weight > last.first
    val upR = last != null && v.reps > last.second
    Card(Modifier.fillMaxWidth(), border = if (upW || upR) c.good else c.hairline, padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, color = c.text, style = cutStyle(17, 700, 1.1f))
                Text(
                    when {
                        last == null -> "No session yet"
                        bw -> "Last: × ${last.second}"
                        else -> "Last: ${fmtLb(last.first)} × ${last.second}"
                    },
                    color = c.text2, style = cutStyle(11, 600, 1f, tabular = true),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.unit.uppercase(), color = c.text2, style = cutStyle(10, 600, 1f, 0.1f))
                    if (bw) Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(9.dp)).background(c.ground), contentAlignment = Alignment.Center) {
                        Text(e.bwLabel ?: "Body", color = c.text, style = cutStyle(22, 800, 1f))
                    } else Stepper(fmtLb(v.weight), upW, onMinus = { onChange(v.copy(weight = (v.weight - e.step).coerceAtLeast(0.0))) }, onPlus = { onChange(v.copy(weight = v.weight + e.step)) })
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("REPS × $sets", color = c.text2, style = cutStyle(10, 600, 1f, 0.1f))
                    Stepper("${v.reps}", upR, onMinus = { onChange(v.copy(reps = (v.reps - 1).coerceAtLeast(1))) }, onPlus = { onChange(v.copy(reps = v.reps + 1)) })
                }
            }
        }
    }
}

private fun fmtLb(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else ProgramEngine.fmt1(v)

@Composable
private fun Stepper(value: String, up: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = CutTheme.colors
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton("-", onMinus)
        Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(9.dp)).background(c.ground), contentAlignment = Alignment.Center) {
            Text(value, color = if (up) c.good else c.text, style = cutStyle(22, 800, 1f, tabular = true))
        }
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier.width(44.dp).height(48.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, style = cutStyle(20, 700, 1f)) }
}

/** 90-second rest timer. Accent border while running, green at zero, triple haptic when it ends. */
@Composable
fun RestTimer() {
    val c = CutTheme.colors
    val haptics = LocalHaptics.current
    var left by remember { mutableIntStateOf(ProgramSpec.REST_SECONDS) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        while (running && left > 0) {
            delay(1000)
            left--
            if (left == 0) { running = false; haptics.pattern(0, 60, 60, 60, 60, 60) }
        }
    }
    val border = when { running -> c.accent; left == 0 -> c.good; else -> c.hairline }
    Card(Modifier.fillMaxWidth(), border = border, padding = PaddingValues(14.dp, 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("REST", color = c.text2, style = cutStyle(10, 600, 1f, 0.1f))
                Text("%d:%02d".format(left / 60, left % 60), color = c.text, style = cutStyle(28, 800, 1f, tabular = true))
            }
            Pill(
                when { running -> "Stop"; left == 0 -> "Reset"; else -> "Start" }, on = false,
                onClick = { if (running) running = false else if (left == 0) left = ProgramSpec.REST_SECONDS else running = true },
            )
        }
    }
}
