package com.relentnet.cuttracker.ui.beta

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.Exercise
import com.relentnet.cuttracker.data.LibMeal
import com.relentnet.cuttracker.data.Meal
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.H1
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/**
 * Beta: the plan for the route. The meal slots Today runs, the library they log from, and the lifts in each block.
 * Every row expands in place and saves when you tap Done; adds and removes save at once. No Save button.
 */
@Composable
fun BetaScreen(vm: AppViewModel) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    fun open(id: String?) { editing = if (editing == id) null else id }

    Screen {
        Column {
            Eyebrow("Your plan for the route")
            H1("Beta")
            Sub("What you eat and what you lift. Change it here and Today follows from the next day you open.", Modifier.padding(top = 8.dp))
        }

        // ---- Meals: the slots
        SectionHead("Meals", "${s.meals.size} slots on the script")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in s.meals) {
                val def = ProgramEngine.defaultChoice(m, s)
                val picks = st.logs.count { it.picks.containsKey(m.id) || m.id in it.doneItemIds }
                ExpandRow(
                    open = editing == m.id, onToggle = { open(m.id) },
                    lead = { TimeChip(m.time) },
                    title = m.name, sub = def.name + "  ·  ${def.protein}g · ${def.calories} cal",
                    trail = if (picks > 0) "$picks logged" else null,
                ) {
                    SlotEditor(m, s.library, onSave = { nm -> vm.saveSettings(s.copy(meals = s.meals.map { if (it.id == m.id) nm else it })); editing = null },
                        onRemove = { haptics.tap(); vm.saveSettings(s.copy(meals = s.meals.filter { it.id != m.id })); editing = null })
                }
            }
            AddRow("Add a slot") {
                val id = "m${System.currentTimeMillis()}"
                vm.saveSettings(s.copy(meals = s.meals + Meal(id, "New slot", "", "", 0, 0, 10, true, s.library.firstOrNull()?.id)))
                editing = id
            }
        }

        // ---- Library
        SectionHead("Library", "what a slot can log")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (l in s.library) {
                val uses = st.logs.count { log -> log.picks.values.any { it == "lib:" + l.id } }
                ExpandRow(
                    open = editing == l.id, onToggle = { open(l.id) },
                    lead = { Hold(HoldShape.JUG, c.goodDim, 22.dp) },
                    title = l.name, sub = "${l.protein}g · ${l.calories} cal" + (if (l.recipe.isNotBlank()) "  ·  recipe" else ""),
                    trail = if (uses > 0) "$uses×" else null,
                ) {
                    LibEditor(l,
                        onSave = { nl -> vm.saveSettings(s.copy(library = s.library.map { if (it.id == l.id) nl else it })); editing = null },
                        onRemove = {
                            haptics.tap()
                            vm.saveSettings(s.copy(library = s.library.filter { it.id != l.id }, meals = s.meals.map { if (it.defaultLib == l.id) it.copy(defaultLib = null) else it }))
                            editing = null
                        })
                }
            }
            AddRow("Add a meal") {
                val id = "lib_${System.currentTimeMillis()}"
                vm.saveSettings(s.copy(library = s.library + LibMeal(id, "New meal", 0, 0)))
                editing = id
            }
        }

        // ---- Lifts: one list per block
        for (block in listOf(Block.CLIMB, Block.OFF)) {
            val lifts = s.exercises.filter { it.block == block && !it.phase2Only }
            SectionHead(
                if (block == Block.CLIMB) "Climb day block" else "Off day block",
                if (block == Block.CLIMB) "after the session, at the gym" else "15 minutes at home",
                shape = if (block == Block.CLIMB) HoldShape.CRIMP else HoldShape.JUG,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (e in lifts) {
                    val bw = e.step <= 0.0
                    val history = ProgramEngine.sessions(st.liftSets, e.id, st.today.plusDays(1))
                    ExpandRow(
                        open = editing == e.id, onToggle = { open(e.id) },
                        lead = { SetsChip(e.sets) },
                        title = e.name, sub = (if (bw) "bodyweight" else "+${fmtStep(e.step)} lb steps") + (if (e.repTarget.isNotBlank()) "  ·  ${e.repTarget} reps" else ""),
                        trail = null,
                        under = if (history.isNotEmpty()) ({ Trend(history, bw) }) else null,
                    ) {
                        LiftEditor(e,
                            onSave = { ne -> vm.saveSettings(s.copy(exercises = s.exercises.map { if (it.id == e.id) ne else it })); editing = null },
                            onRemove = { haptics.tap(); vm.saveSettings(s.copy(exercises = s.exercises.filter { it.id != e.id })); editing = null })
                    }
                }
                AddRow("Add a lift") {
                    val id = "x${System.currentTimeMillis()}"
                    vm.saveSettings(s.copy(exercises = s.exercises + Exercise(id, "New lift", block, 3, "8 to 12", 5.0, "Lb")))
                    editing = id
                }
            }
        }
        Sub("Lifts you log on Today show their last sessions here. Beat the last number and the chip goes green.", size = 12)
    }
}

// ---- building blocks

@Composable
private fun SectionHead(title: String, sub: String, shape: HoldShape? = null) {
    val c = CutTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (shape != null) Hold(shape, c.accent, 16.dp)
        Text(title.uppercase(), color = c.text, style = cutStyle(12, 800, 1f, 0.1f))
        Text(sub, color = c.text2, style = cutStyle(12, 500, 1f))
    }
}

/** A Today-style row: lead chip, title and sub, optional trailing note, chevron; the editor unfolds beneath it. */
@Composable
private fun ExpandRow(
    open: Boolean, onToggle: () -> Unit,
    lead: @Composable () -> Unit, title: String, sub: String, trail: String?,
    under: (@Composable () -> Unit)? = null,
    editor: @Composable () -> Unit,
) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth().animateContentSize(), border = if (open) c.mutedBorder else c.hairline, radius = 12.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle, role = Role.Button).padding(14.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                lead()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = c.text, style = cutStyle(16, 700, 1.1f))
                    Text(sub, color = c.text2, style = cutStyle(12, 500, 1.2f, tabular = true))
                    if (under != null) Box(Modifier.padding(top = 4.dp)) { under() }
                }
                if (trail != null) Text(trail, color = c.text2, style = cutStyle(11, 700, 1f, tabular = true))
                Icon(CutIcons.Forward, null, Modifier.size(12.dp).rotate(if (open) 90f else 0f), tint = c.text2)
            }
            if (open) Column(Modifier.background(c.ground).padding(14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { editor() }
        }
    }
}

@Composable
private fun AddRow(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    Text(
        "+ ${label.uppercase()}", color = c.text2, style = cutStyle(11, 800, 1f, 0.1f),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick, role = Role.Button).padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
private fun TimeChip(time: String) {
    val c = CutTheme.colors
    Box(Modifier.width(46.dp).height(28.dp).clip(RoundedCornerShape(7.dp)).background(c.ground), contentAlignment = Alignment.Center) {
        Text(time.ifBlank { "any" }, color = c.text, style = cutStyle(11, 800, 1f, tabular = true))
    }
}

@Composable
private fun SetsChip(sets: Int) {
    val c = CutTheme.colors
    Box(Modifier.width(46.dp).height(28.dp).clip(RoundedCornerShape(7.dp)).background(c.ground), contentAlignment = Alignment.Center) {
        Text("$sets×", color = c.text, style = cutStyle(12, 800, 1f, tabular = true))
    }
}

@Composable
private fun Trend(history: List<Pair<Double, Int>>, bw: Boolean) {
    val c = CutTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        history.forEachIndexed { i, (w, r) ->
            val beat = i > 0 && ProgramEngine.beat(w to r, history[i - 1])
            Text(
                if (bw) "×$r" else "${fmtStep(w)}×$r",
                color = if (beat) c.good else c.text2, style = cutStyle(11, 700, 1f, tabular = true),
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.ground).padding(6.dp, 4.dp),
            )
            if (i < history.lastIndex) Text("›", color = c.dim, style = cutStyle(11, 700, 1f))
        }
    }
}

@Composable
private fun Label(text: String) = Text(text.uppercase(), color = CutTheme.colors.text2, style = cutStyle(10, 800, 1f, 0.1f))

@Composable
private fun Txt(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String? = null, multiline: Boolean = false) {
    Field(
        value, onChange, modifier, height = if (multiline) 84.dp else 40.dp, fontSize = 14, weight = if (multiline) 500 else 700,
        keyboard = KeyboardType.Text, imeAction = if (multiline) ImeAction.Default else ImeAction.Next, borderWidth = 1.dp, radius = 8.dp, padding = 10.dp,
        placeholder = placeholder, singleLine = !multiline,
    )
}

@Composable
private fun Num(value: String, onChange: (String) -> Unit, width: Int = 64) {
    Field(value, onChange, Modifier.width(width.dp), height = 40.dp, fontSize = 14, weight = 700, center = true, borderWidth = 1.dp, radius = 8.dp, padding = 6.dp, imeAction = ImeAction.Next)
}

@Composable
private fun EditorButtons(onSave: () -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BigButton("Done", onClick = onSave, modifier = Modifier.weight(1f), height = 40.dp, fontSize = 12)
        BigButton("Remove", onClick = onRemove, modifier = Modifier.width(110.dp), secondary = true, height = 40.dp, fontSize = 12)
    }
}

@Composable
private fun SlotEditor(m: Meal, library: List<LibMeal>, onSave: (Meal) -> Unit, onRemove: () -> Unit) {
    val c = CutTheme.colors
    var name by remember(m.id) { mutableStateOf(m.name) }
    var time by remember(m.id) { mutableStateOf(m.time) }
    var def by remember(m.id) { mutableStateOf(m.defaultLib) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(name, { name = it }, Modifier.weight(1f), "Name")
        Num(time, { time = it }, 72)
    }
    Label("Default · what a swipe logs")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in library.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (l in row) {
                    val on = def == l.id
                    val shape = RoundedCornerShape(8.dp)
                    Text(
                        l.name, color = if (on) c.onAccent else c.text, style = cutStyle(12, 700, 1.1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clip(shape).background(if (on) c.accent else c.surface).border(1.dp, if (on) c.accent else c.mutedBorder, shape)
                            .clickable(role = Role.Button) { def = l.id }.padding(10.dp, 9.dp),
                    )
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
    EditorButtons(onSave = { onSave(m.copy(name = name.ifBlank { m.name }, time = time.trim(), defaultLib = def)) }, onRemove = onRemove)
}

@Composable
private fun LibEditor(l: LibMeal, onSave: (LibMeal) -> Unit, onRemove: () -> Unit) {
    var name by remember(l.id) { mutableStateOf(l.name) }
    var pro by remember(l.id) { mutableStateOf(l.protein.toString()) }
    var cal by remember(l.id) { mutableStateOf(l.calories.toString()) }
    var recipe by remember(l.id) { mutableStateOf(l.recipe) }
    Txt(name, { name = it }, Modifier.fillMaxWidth(), "Name")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Num(pro, { pro = it }); Sub("g protein", size = 11)
        Num(cal, { cal = it }, 72); Sub("cal", size = 11)
    }
    Txt(recipe, { recipe = it }, Modifier.fillMaxWidth(), "Recipe or notes", multiline = true)
    EditorButtons(onSave = { onSave(LibMeal(l.id, name.ifBlank { l.name }, pro.toIntOrNull() ?: 0, cal.toIntOrNull() ?: 0, recipe)) }, onRemove = onRemove)
}

@Composable
private fun LiftEditor(e: Exercise, onSave: (Exercise) -> Unit, onRemove: () -> Unit) {
    val c = CutTheme.colors
    var name by remember(e.id) { mutableStateOf(e.name) }
    var sets by remember(e.id) { mutableStateOf(e.sets.toString()) }
    var step by remember(e.id) { mutableStateOf(fmtStep(e.step)) }
    var reps by remember(e.id) { mutableStateOf(e.repTarget) }
    var block by remember(e.id) { mutableStateOf(e.block) }
    Txt(name, { name = it }, Modifier.fillMaxWidth(), "Name")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Num(sets, { sets = it }, 52); Sub("sets", size = 11)
        Num(step, { step = it }, 56); Sub("lb per tap, 0 for bodyweight", size = 11)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(reps, { reps = it }, Modifier.weight(1f), "Rep target, like 8 to 12")
        for (b in listOf(Block.CLIMB, Block.OFF)) {
            val on = block == b
            val shape = RoundedCornerShape(8.dp)
            Text(
                if (b == Block.CLIMB) "CLIMB" else "OFF", color = if (on) c.onAccent else c.text, style = cutStyle(11, 800, 1f, 0.06f),
                modifier = Modifier.clip(shape).background(if (on) c.accent else c.surface).border(1.dp, if (on) c.accent else c.mutedBorder, shape)
                    .clickable(role = Role.Button) { block = b }.padding(10.dp, 11.dp),
            )
        }
    }
    EditorButtons(
        onSave = {
            val st = step.toDoubleOrNull() ?: 0.0
            onSave(e.copy(name = name.ifBlank { e.name }, sets = sets.toIntOrNull()?.coerceIn(1, 10) ?: e.sets, step = st, repTarget = reps.trim(), block = block,
                unit = if (st > 0) e.unit.ifEmpty { "Lb" } else e.unit, bwLabel = if (st > 0) null else (e.bwLabel ?: "Body")))
        },
        onRemove = onRemove,
    )
}

private fun fmtStep(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else ProgramEngine.fmt1(v)
