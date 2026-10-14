package com.relentnet.cuttracker.ui.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.Exercise
import com.relentnet.cuttracker.data.Meal
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate

@Composable
private fun TextField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null) {
    Field(value, onChange, modifier, height = 40.dp, fontSize = 14, weight = 700, keyboard = KeyboardType.Text, imeAction = ImeAction.Next, borderWidth = 1.dp, radius = 8.dp, padding = 10.dp, placeholder = placeholder)
}

@Composable
private fun SmallNum(value: String, onChange: (String) -> Unit, width: Int = 56) {
    Field(value, onChange, Modifier.width(width.dp), height = 36.dp, fontSize = 13, weight = 700, center = true, borderWidth = 1.dp, radius = 8.dp, padding = 6.dp, imeAction = ImeAction.Next)
}

@Composable
private fun RemoveButton(onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(40.dp).clip(shape).border(1.dp, c.mutedBorder, shape).clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) { Icon(CutIcons.Close, "Remove", Modifier.size(14.dp), tint = c.text) }
}

@Composable
private fun BlockPill(label: String, on: Boolean, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.height(32.dp).clip(shape).background(if (on) c.accent else c.surface).border(1.dp, if (on) c.accent else c.mutedBorder, shape).clickable(onClick = onClick, role = Role.Button).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label.uppercase(), color = if (on) c.onAccent else c.text, style = cutStyle(11, 800, 1f, 0.06f)) }
}

@Composable
private fun RowDivider(last: Boolean) {
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(CutTheme.colors.hairline))
}

// Meals

private data class MealDraft(val id: String, var name: String, var sub: String, var time: String, var protein: String, var calories: String, val xp: Int, val pick: Boolean)

@Composable
fun MealsEditor(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val rows = remember(s.meals) { s.meals.map { MealDraft(it.id, it.name, it.sub, it.time, it.protein.toString(), it.calories.toString(), it.xp, it.pick) }.toMutableStateList() }
    var tick by remember { mutableStateOf(0) }
    Screen(bottomInset = true) {
        BackHeader("Settings", "Meals", onBack)
        Sub("These are the fixed rows on Today. Protein and calories feed the tiles. Dinner options are the three picks.")
        Card(Modifier.fillMaxWidth()) {
            key(tick) {
                rows.forEachIndexed { i, m ->
                    Column(Modifier.padding(14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextField(m.name, { rows[i] = m.copy(name = it) }, Modifier.weight(1f), "Name")
                            RemoveButton { rows.removeAt(i); tick++ }
                        }
                        TextField(m.sub, { rows[i] = m.copy(sub = it) }, Modifier.fillMaxWidth(), "Description")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Field(m.time, { rows[i] = m.copy(time = it) }, Modifier.weight(1f), height = 36.dp, fontSize = 13, weight = 700, center = true, keyboard = KeyboardType.Text, imeAction = ImeAction.Next, borderWidth = 1.dp, radius = 8.dp, padding = 8.dp, placeholder = "Time")
                            SmallNum(m.protein, { rows[i] = m.copy(protein = it) }); Sub("g", size = 11)
                            SmallNum(m.calories, { rows[i] = m.copy(calories = it) }, 64); Sub("cal", size = 11)
                        }
                    }
                    RowDivider(i == rows.lastIndex)
                }
            }
        }
        BigButton("Add a meal", secondary = true, modifier = Modifier.fillMaxWidth(), onClick = { rows += MealDraft("m${System.currentTimeMillis()}", "New meal", "", "", "0", "0", 10, false); tick++ })
        BigButton("Save", modifier = Modifier.fillMaxWidth(), onClick = {
            val meals = rows.map { Meal(it.id, it.name, it.sub, it.time, it.protein.toIntOrNull() ?: 0, it.calories.toIntOrNull() ?: 0, it.xp, it.pick) }
            vm.saveSettings(s.copy(meals = meals)) { vm.showToast("Meals saved"); onBack() }
        })
    }
}

@Composable
private inline fun key(k: Any, content: @Composable () -> Unit) = androidx.compose.runtime.key(k) { content() }

// Exercises

private data class ExDraft(val id: String, var name: String, var block: Block, var sets: String, var step: String, val repTarget: String, val unit: String, val bwLabel: String?, val setsPhase2: Int?, val phase2Only: Boolean)

@Composable
fun ExercisesEditor(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val rows = remember(s.exercises) { s.exercises.map { ExDraft(it.id, it.name, it.block, it.sets.toString(), fmtStep(it.step), it.repTarget, it.unit, it.bwLabel, it.setsPhase2, it.phase2Only) }.toMutableStateList() }
    var tick by remember { mutableStateOf(0) }
    Screen(bottomInset = true) {
        BackHeader("Settings", "Exercises", onBack)
        Sub("Which block each lift belongs to, sets, and how much the + button adds. Bodyweight lifts have a 0 step.")
        Card(Modifier.fillMaxWidth()) {
            key(tick) {
                rows.forEachIndexed { i, e ->
                    Column(Modifier.padding(14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextField(e.name, { rows[i] = e.copy(name = it) }, Modifier.weight(1f), "Name")
                            RemoveButton { rows.removeAt(i); tick++ }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                BlockPill("Climb", e.block == Block.CLIMB) { rows[i] = e.copy(block = Block.CLIMB) }
                                BlockPill("Off", e.block == Block.OFF) { rows[i] = e.copy(block = Block.OFF) }
                            }
                            Sub("sets", size = 11); SmallNum(e.sets, { rows[i] = e.copy(sets = it) })
                            Sub("step", size = 11); SmallNum(e.step, { rows[i] = e.copy(step = it) }); Sub("lb", size = 11)
                        }
                    }
                    RowDivider(i == rows.lastIndex)
                }
            }
        }
        BigButton("Add an exercise", secondary = true, modifier = Modifier.fillMaxWidth(), onClick = { rows += ExDraft("x${System.currentTimeMillis()}", "New lift", Block.OFF, "3", "5", "", "Lb", null, null, false); tick++ })
        BigButton("Save", modifier = Modifier.fillMaxWidth(), onClick = {
            val ex = rows.map {
                val step = it.step.toDoubleOrNull() ?: 0.0
                Exercise(it.id, it.name, it.block, it.sets.toIntOrNull() ?: 3, it.repTarget, step, if (step > 0) it.unit.ifEmpty { "Lb" } else it.unit, if (step > 0) null else (it.bwLabel ?: "Body"), it.setsPhase2, it.phase2Only)
            }
            vm.saveSettings(s.copy(exercises = ex)) { vm.showToast("Exercises saved"); onBack() }
        })
    }
}

private fun fmtStep(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

// Checkpoints

private data class CpDraft(var date: String, var free: Boolean, var target: String, var note: String, val days: Int, val label: String?)

@Composable
fun CheckpointsEditor(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val context = LocalContext.current
    val rows = remember(s.checkpoints) { s.checkpoints.map { CpDraft(it.date, it.free, it.target?.let(::fmt) ?: "", it.note, it.days, it.label) }.toMutableStateList() }
    var tick by remember { mutableStateOf(0) }
    val c = CutTheme.colors
    Screen(bottomInset = true) {
        BackHeader("Settings", "Checkpoints", onBack)
        Sub("Anchors on the route. A free day has no target weight and pauses the calorie tile for that date.")
        Card(Modifier.fillMaxWidth()) {
            key(tick) {
                rows.forEachIndexed { i, cp ->
                    Column(Modifier.padding(14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            val shape = RoundedCornerShape(8.dp)
                            Box(
                                Modifier.weight(1f).height(40.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).clickable(role = Role.Button) {
                                    val d = runCatching { LocalDate.parse(cp.date) }.getOrDefault(s.startDate)
                                    DatePickerDialog(context, { _, y, m, day -> rows[i] = cp.copy(date = LocalDate.of(y, m + 1, day).toString()); tick++ }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                                }.padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) { Text(cp.date, color = c.text, style = cutStyle(14, 700, 1f, tabular = true)) }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                BlockPill("Target", !cp.free) { rows[i] = cp.copy(free = false); tick++ }
                                BlockPill("Free", cp.free) { rows[i] = cp.copy(free = true); tick++ }
                            }
                            RemoveButton { rows.removeAt(i); tick++ }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!cp.free) { SmallNum(cp.target, { rows[i] = cp.copy(target = it) }, 76); Sub("lb", size = 11) }
                            TextField(cp.note, { rows[i] = cp.copy(note = it) }, Modifier.weight(1f), "Note")
                        }
                    }
                    RowDivider(i == rows.lastIndex)
                }
            }
        }
        BigButton("Add a checkpoint", secondary = true, modifier = Modifier.fillMaxWidth(), onClick = {
            rows += CpDraft(s.startDate.plusDays(7).toString(), false, "", "", 1, null); rows.sortBy { it.date }; tick++
        })
        BigButton("Save", modifier = Modifier.fillMaxWidth(), onClick = {
            val cps = rows.filter { runCatching { LocalDate.parse(it.date) }.isSuccess }.sortedBy { it.date }
                .map { Checkpoint(it.date, if (it.free) null else it.target.toDoubleOrNull(), it.free, it.days, it.label, it.note) }
            vm.saveSettings(s.copy(checkpoints = cps)) { vm.showToast("Checkpoints saved"); onBack() }
        })
    }
}
