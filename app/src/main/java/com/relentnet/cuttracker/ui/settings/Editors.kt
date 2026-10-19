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
import com.relentnet.cuttracker.data.LibMeal
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

// Meals: the slots on Today and the library they log from

private data class SlotDraft(val id: String, var name: String, var time: String, var defaultLib: String?, val xp: Int, val sub: String, val protein: Int, val calories: Int)
private data class LibDraft(val id: String, var name: String, var protein: String, var calories: String, var recipe: String)

@Composable
fun MealsEditor(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val slots = remember(s.meals) { s.meals.map { SlotDraft(it.id, it.name, it.time, it.defaultLib, it.xp, it.sub, it.protein, it.calories) }.toMutableStateList() }
    val lib = remember(s.library) { s.library.map { LibDraft(it.id, it.name, it.protein.toString(), it.calories.toString(), it.recipe) }.toMutableStateList() }
    var tick by remember { mutableStateOf(0) }
    Screen(bottomInset = true) {
        BackHeader("Settings", "Meals", onBack)
        Sub("Slots are the rows on Today. Each logs its default meal with a swipe; tapping the row picks any library meal or a one-off.")
        Sub("SLOTS", size = 11)
        Card(Modifier.fillMaxWidth()) {
            key(tick) {
                slots.forEachIndexed { i, m ->
                    Column(Modifier.padding(14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextField(m.name, { slots[i] = m.copy(name = it) }, Modifier.weight(1f), "Name")
                            Field(m.time, { slots[i] = m.copy(time = it) }, Modifier.width(72.dp), height = 40.dp, fontSize = 13, weight = 700, center = true, keyboard = KeyboardType.Text, imeAction = ImeAction.Next, borderWidth = 1.dp, radius = 8.dp, padding = 6.dp, placeholder = "Time")
                            RemoveButton { slots.removeAt(i); tick++ }
                        }
                        // Tapping cycles the default through the library.
                        val cur = lib.firstOrNull { it.id == m.defaultLib }
                        BlockPill("Default: " + (cur?.name ?: "slot numbers") + "  ›", on = false) {
                            if (lib.isNotEmpty()) {
                                val idx = lib.indexOfFirst { it.id == m.defaultLib }
                                slots[i] = m.copy(defaultLib = lib[(idx + 1) % lib.size].id); tick++
                            }
                        }
                    }
                    RowDivider(i == slots.lastIndex)
                }
            }
        }
        BigButton("Add a slot", secondary = true, modifier = Modifier.fillMaxWidth(), height = 44.dp, fontSize = 13, onClick = {
            slots += SlotDraft("m${System.currentTimeMillis()}", "New slot", "", lib.firstOrNull()?.id, 10, "", 0, 0); tick++
        })
        Sub("LIBRARY", size = 11)
        Card(Modifier.fillMaxWidth()) {
            key(tick) {
                lib.forEachIndexed { i, m ->
                    Column(Modifier.padding(14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextField(m.name, { lib[i] = m.copy(name = it) }, Modifier.weight(1f), "Name")
                            RemoveButton { lib.removeAt(i); tick++ }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            SmallNum(m.protein, { lib[i] = m.copy(protein = it) }); Sub("g", size = 11)
                            SmallNum(m.calories, { lib[i] = m.copy(calories = it) }, 64); Sub("cal", size = 11)
                        }
                        Field(m.recipe, { lib[i] = m.copy(recipe = it) }, Modifier.fillMaxWidth(), height = 84.dp, fontSize = 13, weight = 500, keyboard = KeyboardType.Text, imeAction = ImeAction.Default, borderWidth = 1.dp, radius = 8.dp, padding = 10.dp, placeholder = "Recipe or notes", singleLine = false)
                    }
                    RowDivider(i == lib.lastIndex)
                }
            }
        }
        BigButton("Add a meal", secondary = true, modifier = Modifier.fillMaxWidth(), height = 44.dp, fontSize = 13, onClick = { lib += LibDraft("lib_${System.currentTimeMillis()}", "New meal", "0", "0", ""); tick++ })
        BigButton("Save", modifier = Modifier.fillMaxWidth(), onClick = {
            val library = lib.map { LibMeal(it.id, it.name, it.protein.toIntOrNull() ?: 0, it.calories.toIntOrNull() ?: 0, it.recipe) }
            val meals = slots.map { Meal(it.id, it.name, it.sub, it.time, it.protein, it.calories, it.xp, true, it.defaultLib?.takeIf { d -> library.any { l -> l.id == d } }) }
            vm.saveSettings(s.copy(meals = meals, library = library)) { vm.showToast("Meals saved"); onBack() }
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
        Sub("Target weights along the route. Days off are planned from the calendar on Route, by tapping the day.")
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
                            if (cp.free) Sub("skip", size = 11)
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
