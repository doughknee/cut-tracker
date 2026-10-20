package com.relentnet.cuttracker.ui.beta

import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.matchesQuery
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.DayPlan
import com.relentnet.cuttracker.data.PlanItem
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.AddRow
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.ChipRow
import com.relentnet.cuttracker.ui.components.Fab
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.MiniStepper
import com.relentnet.cuttracker.ui.components.NumRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetField
import com.relentnet.cuttracker.ui.components.SheetLabel
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.holdShape
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** What one kind of day asks for: which meals, which lifts make its block, and the rest of the script. */
@Composable
fun DayPlanPage(vm: AppViewModel, mode: DayMode, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    val plan = ProgramEngine.plan(mode, s)
    fun save(p: DayPlan) = vm.updateSettings { s -> s.copy(plans = s.plans + (mode to p)) }
    var sheet by rememberSaveable { mutableStateOf("") }   // "", "pick", "new", "lift:<id>", "item:<id>"
    val items = ProgramEngine.script(mode, s)
    val where = dayWhere(mode, s)
    val blockName = if (mode == DayMode.CLIMB) "Push block" else "Core block"
    val blockWhere = if (mode == DayMode.CLIMB) "at the gym, after the session" else "at home, 15 minutes"
    val lifts = ProgramEngine.exercisesFor(mode, s)

    Box(Modifier.fillMaxSize()) {
        Screen(bottomInset = true, fab = true) {
            BackHeader("Beta", dayTitle(mode), onBack)
            Card(Modifier.fillMaxWidth(), radius = 14.dp, padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Hold(holdShape(mode), c.accent, 44.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                            Text("${ProgramEngine.totalXp(items)}", color = c.accent, style = cutStyle(22, 800, 1f, -0.02f, display = true, tabular = true))
                            Text("xp for a full Send", color = c.text, style = cutStyle(15, 800, 1f, display = true), modifier = Modifier.padding(bottom = 1.dp))
                        }
                        Text(where, color = c.text2, style = cutStyle(12, 500, 1.35f))
                    }
                }
            }
            KindCard("Meals", "${plan.slots.count { id -> s.meals.any { it.id == id } }} of ${s.meals.size}") {
                for (m in s.meals) {
                    val on = m.id in plan.slots
                    NumRow(
                        "${m.xp}", m.name, unit = "xp", sub = "any of ${s.library.count { it.kind == m.id }}",
                        on = on, onSwitch = { haptics.tick(); save(plan.copy(slots = if (on) plan.slots - m.id else plan.slots + m.id)) },
                    )
                }
            }
            KindCard(blockName, if (lifts.isEmpty()) "none" else "${lifts.size} lifts", sub = if (mode == DayMode.REST && lifts.isEmpty()) "A rest day asks for no block. Add a lift and it gets one." else blockWhere) {
                for (e in lifts) {
                    val best = ProgramEngine.sessions(st.liftSets, e.id, st.today.plusDays(1), n = 1).firstOrNull()
                    NumRow(
                        "${e.sets}×", e.name, sub = best?.let { (w, r) -> "best " + (if (e.step <= 0.0) "×$r" else "${fmtStep(w)}×$r") } ?: "no best yet",
                        right = if (lifts.isNotEmpty()) "${if (mode == DayMode.CLIMB) ProgramSpec.XP_PUSH else ProgramSpec.XP_CORE} xp for the block" else null,
                        onClick = { sheet = "lift:" + e.id },
                    )
                }
                AddRow("Add a lift to the block") { sheet = "pick" }
            }
            KindCard("The rest of the day", "${plan.items.count { it.on } + 1} of ${plan.items.size + 1}") {
                NumRow("${ProgramSpec.XP_WEIGH}", "Weigh in", unit = "xp", sub = "always on", on = true, onSwitch = { haptics.tap() })
                for (it in plan.items) NumRow(
                    if (it.xp > 0) "${it.xp}" else "–", it.label, unit = if (it.xp > 0) "xp" else null, sub = it.sub.ifBlank { null },
                    on = it.on, onSwitch = { haptics.tick(); save(plan.copy(items = plan.items.map { x -> if (x.id == it.id) x.copy(on = !x.on) else x })) },
                    onClick = { sheet = "item:" + it.id },
                )
            }
            Sub("Switch a line off and Today stops asking for it on a ${dayTitle(mode).lowercase()}. The xp adds up to the Send.", size = 12)
        }
        Fab(Modifier.align(Alignment.BottomEnd)) { sheet = "new" }
    }

    when {
        sheet == "pick" -> LiftPicker(st.settings.liftKinds, s.exercises.filter { !it.phase2Only || it.id in plan.lifts }, plan.lifts, onDismiss = { sheet = "" },
            onDone = { ids -> save(plan.copy(lifts = ids)); sheet = "" })
        sheet.startsWith("lift:") -> s.exercises.firstOrNull { it.id == sheet.removePrefix("lift:") }?.let { e ->
            var sets by remember(e) { mutableStateOf(e.sets) }
            Sheet(e.name, onDismiss = { sheet = "" }) {
                SheetLabel("Sets on a ${dayTitle(mode).lowercase()}")
                MiniStepper(sets.toString(), onMinus = { sets = (sets - 1).coerceAtLeast(1) }, onPlus = { sets = (sets + 1).coerceAtMost(10) }, unit = "× ${e.repTarget.ifBlank { "reps" }}")
                SheetNote("Sets change the lift itself, on every day that uses it. Reps and the weight step are on the Lifts page.")
                SheetButtons("Done", onPrimary = { vm.updateSettings { s -> s.copy(exercises = s.exercises.map { if (it.id == e.id) it.copy(sets = sets) else it }) }; sheet = "" },
                    secondary = "Take out", onSecondary = { haptics.tap(); save(plan.copy(lifts = plan.lifts - e.id)); sheet = "" })
            }
        } ?: run { sheet = "" }
        sheet == "new" || sheet.startsWith("item:") -> {
            val it = plan.items.firstOrNull { x -> x.id == sheet.removePrefix("item:") }
            if (sheet != "new" && it == null) { sheet = "" } else ItemSheet(it, dayTitle(mode), onDismiss = { sheet = "" },
                onSave = { ni -> save(plan.copy(items = if (it == null) plan.items + ni else plan.items.map { x -> if (x.id == ni.id) ni else x })); sheet = "" },
                onRemove = if (it == null) null else ({ haptics.tap(); save(plan.copy(items = plan.items.filter { x -> x.id != it.id })); sheet = "" }))
        }
    }
}

@Composable
private fun ItemSheet(it: PlanItem?, day: String, onDismiss: () -> Unit, onSave: (PlanItem) -> Unit, onRemove: (() -> Unit)?) {
    var label by remember(it) { mutableStateOf(it?.label ?: "") }
    var sub by remember(it) { mutableStateOf(it?.sub ?: "") }
    var xp by remember(it) { mutableStateOf(it?.xp ?: 10) }
    Sheet(if (it == null) "New line" else "Edit line", onDismiss) {
        SheetField("Name", label, { label = it }, text = true, big = true, placeholder = "What Today asks")
        SheetField("Note", sub, { sub = it }, text = true, placeholder = "Optional, shows under the name", last = true)
        SheetLabel("xp")
        MiniStepper(xp.toString(), onMinus = { xp = (xp - 5).coerceAtLeast(0) }, onPlus = { xp = (xp + 5).coerceAtMost(100) })
        SheetNote("Shows on Today as a line to tick on a ${day.lowercase()}.")
        SheetButtons(
            if (it == null) "Add it" else "Done", enabled = label.isNotBlank(),
            onPrimary = { onSave(PlanItem(it?.id ?: "i${System.currentTimeMillis()}", label.trim(), sub.trim(), it?.time ?: "", xp, it?.on ?: true)) },
            secondary = if (it == null) "Cancel" else "Remove", onSecondary = onRemove ?: onDismiss,
        )
    }
}

/** Every lift with a check; the kind chips narrow the list. */
@Composable
private fun LiftPicker(kinds: List<String>, all: List<com.relentnet.cuttracker.data.Exercise>, chosen0: List<String>, onDismiss: () -> Unit, onDone: (List<String>) -> Unit) {
    val c = CutTheme.colors
    var chosen by remember { mutableStateOf(chosen0) }
    var filter by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val other = "\u0000other"
    val hasOther = all.any { it.kind.isBlank() || it.kind !in kinds }
    Sheet("Add to the block", onDismiss, dirty = chosen != chosen0) {
        if (all.size > 6) Field(query, { query = it }, Modifier.fillMaxWidth(), height = 46.dp, fontSize = 15, weight = 600, keyboard = androidx.compose.ui.text.input.KeyboardType.Text, placeholder = "Search lifts")
        ChipRow(listOf("" to "All") + kinds.map { it to it } + (if (hasOther) listOf(other to "Other") else emptyList()), filter, onSelect = { filter = it })
        val shown = all.filter { e ->
            (filter.isEmpty() || (filter == other && (e.kind.isBlank() || e.kind !in kinds)) || e.kind == filter) && matchesQuery(query, e.name, e.kind)
        }
        if (shown.isEmpty()) SheetNote("No lift matches. Add it from Lifts on Beta.")
        else Card(Modifier.fillMaxWidth(), radius = 14.dp, background = c.ground, border = c.hairline) {
            for (e in shown) {
                val on = e.id in chosen
                NumRow("${e.sets}×", e.name, sub = e.kind.ifBlank { "Other" }, right = if (on) "in the block" else "add", rightColor = if (on) c.good else c.text2, chevron = false,
                    onClick = { chosen = if (on) chosen - e.id else chosen + e.id })
            }
        }
        SheetNote("${chosen.size} in the block. Tap a lift to put it in or take it out.")
        SheetButtons("Done", onPrimary = { onDone(chosen) }, secondary = "Cancel", onSecondary = onDismiss)
    }
}
