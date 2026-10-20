package com.relentnet.cuttracker.ui.today

import androidx.compose.ui.focus.onFocusChanged
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.repeatingPress
import com.relentnet.cuttracker.ui.components.num
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
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.delay

private data class Entry(val weight: Double, val reps: Int)

/** The exercise cards, rest timer and Save session, rendered inside Today under the workout row. */
@Composable
fun LiftInline(vm: AppViewModel, mode: DayMode, onSaved: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    val exercises = ProgramEngine.exercisesFor(mode, s)
    val last = remember(st.liftSets, st.today) { exercises.associate { it.id to ProgramEngine.lastSession(st.liftSets, it.id, st.today) } }
    // The last four sessions per exercise, oldest first, so the card shows where the number has been going.
    val history = remember(st.liftSets, st.today) { exercises.associate { e -> e.id to ProgramEngine.sessions(st.liftSets, e.id, st.today) } }
    // Drafts live on the view model, keyed by day, so closing the panel or opening a meal never loses typed numbers.
    val draft = vm.liftDraft
    fun key(id: String) = "${st.today}:$id"
    // Prefill from the last session; a fresh exercise starts at 0 lb × 10 reps.
    LaunchedEffect(last, st.today) {
        for (e in exercises) if (key(e.id) !in draft) {
            val l = last[e.id]
            draft[key(e.id)] = (l?.first ?: 0.0) to (l?.second ?: 10)
        }
    }

    Column(Modifier.fillMaxWidth().padding(start = 40.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow("Lift · Week ${st.weekN} · beat the last number")
        for (e in exercises) {
            val v = draft[key(e.id)]?.let { Entry(it.first, it.second) } ?: Entry(last[e.id]?.first ?: 0.0, last[e.id]?.second ?: 10)
            ExerciseCard(
                e, v, last[e.id], history[e.id] ?: emptyList(), sets = e.sets,
                onChange = { draft[key(e.id)] = it.weight to it.reps; haptics.tick() },
            )
        }
        BigButton("Save session · checks the block", modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 13, onClick = {
            haptics.pattern(0, 20)
            vm.saveLiftSession(exercises.map { e ->
                val v = draft[key(e.id)] ?: (0.0 to 10)
                Repo.LiftEntry(e.id, v.first, v.second, e.sets)
            })
            onSaved()
        })
    }
}

@Composable
private fun ExerciseCard(e: Exercise, v: Entry, last: Pair<Double, Int>?, history: List<Pair<Double, Int>>, sets: Int, onChange: (Entry) -> Unit) {
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
            if (history.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                history.forEachIndexed { i, (w, r) ->
                    val beat = i > 0 && ProgramEngine.beat(w to r, history[i - 1])
                    Text(
                        if (bw) "×$r" else "${fmtLb(w)}×$r",
                        color = if (beat) c.good else c.text2, style = cutStyle(11, 700, 1f, tabular = true),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.ground).padding(6.dp, 4.dp),
                    )
                    if (i < history.lastIndex) Text("›", color = c.dim, style = cutStyle(11, 700, 1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.unit.uppercase(), color = c.text2, style = cutStyle(10, 600, 1f, 0.1f))
                    if (bw) Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(9.dp)).background(c.ground), contentAlignment = Alignment.Center) {
                        Text(e.bwLabel ?: "Body", color = c.text, style = cutStyle(22, 800, 1f))
                    } else Stepper(
                        fmtLb(v.weight), upW, onMinus = { onChange(v.copy(weight = (v.weight - e.step).coerceAtLeast(0.0))) }, onPlus = { onChange(v.copy(weight = v.weight + e.step)) },
                        onType = { onChange(v.copy(weight = it.coerceIn(0.0, 2000.0))) },
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("REPS × $sets", color = c.text2, style = cutStyle(10, 600, 1f, 0.1f))
                    Stepper(
                        "${v.reps}", upR, onMinus = { onChange(v.copy(reps = (v.reps - 1).coerceAtLeast(1))) }, onPlus = { onChange(v.copy(reps = v.reps + 1)) },
                        onType = { onChange(v.copy(reps = it.toInt().coerceIn(1, 500))) },
                    )
                }
            }
        }
    }
}

private fun fmtLb(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else ProgramEngine.fmt1(v)

/** [−][value][+]: the buttons repeat while held, and a tap on the value opens the keyboard to type it. */
@Composable
private fun Stepper(value: String, up: Boolean, onMinus: () -> Unit, onPlus: () -> Unit, onType: (Double) -> Unit) {
    val c = CutTheme.colors
    var typing by remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton("-", onMinus)
        if (typing) {
            // Starts empty with the current value as the hint, so typing replaces it.
            var text by remember { androidx.compose.runtime.mutableStateOf("") }
            val focus = remember { androidx.compose.ui.focus.FocusRequester() }
            var had by remember { androidx.compose.runtime.mutableStateOf(false) }
            LaunchedEffect(Unit) { focus.requestFocus() }
            fun commit() { if (typing) { text.num()?.let(onType); typing = false } }
            Field(
                text, { text = it },
                Modifier.weight(1f).onFocusChanged { if (it.isFocused) had = true else if (had) commit() },
                height = 48.dp, fontSize = 22, center = true, focusRequester = focus, onDone = ::commit, borderWidth = 1.dp, placeholder = value,
            )
        } else Box(
            Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(9.dp)).background(c.ground)
                .clickable(role = Role.Button, onClickLabel = "Type a number") { typing = true },
            contentAlignment = Alignment.Center,
        ) {
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
        Modifier.width(44.dp).height(48.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).repeatingPress(if (label == "+") "More" else "Less", onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, style = cutStyle(20, 700, 1f)) }
}
