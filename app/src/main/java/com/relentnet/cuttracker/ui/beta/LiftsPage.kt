package com.relentnet.cuttracker.ui.beta

import com.relentnet.cuttracker.ui.components.num
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.Exercise
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.MiniStepper
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetField
import com.relentnet.cuttracker.ui.components.SheetLabel

/** Lifts: everything you can put in a block, grouped by kind. Which lifts a day uses is on that day's page. */
@Composable
fun LiftsPage(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    var sheet by rememberSaveable { mutableStateOf("") }   // "", "new", "lift:<id>", "kind:<name>"
    val kinds = s.liftKinds.map { LibKind(it, it) }
    val rows = s.exercises.map { e ->
        val best = ProgramEngine.sessions(st.liftSets, e.id, st.today.plusDays(1), n = 1).firstOrNull()
        val days = DayMode.entries.filter { e.id in ProgramEngine.plan(it, s).lifts }
        val sub = listOfNotNull(
            e.repTarget.takeIf { it.isNotBlank() }?.let { "$it reps" },
            best?.let { (w, r) -> "best " + (if (e.step <= 0.0) "×$r" else "${fmtStep(w)}×$r") },
            days.takeIf { it.isNotEmpty() }?.joinToString(", ") { dayTitle(it).lowercase() + "s" },
        ).joinToString(", ")
        LibRow(e.id, if (e.kind in s.liftKinds) e.kind else "", "${e.sets}×", null, e.name, sub)
    }
    LibraryScreen(
        "Lifts", "${s.exercises.size} lifts you can put in a block. The number is sets.", onBack,
        kinds, rows, searchHint = "Search lifts",
        onRow = { sheet = "lift:" + it.key }, onAdd = { sheet = "new" }, onEditKind = { sheet = "kind:" + it.key },
        onNewKind = { name -> vm.updateSettings { s -> if (name.isBlank() || s.liftKinds.any { it.equals(name.trim(), ignoreCase = true) }) s else s.copy(liftKinds = s.liftKinds + name.trim()) } },
        empty = "No lifts yet. Tap + to add one.",
    )
    when {
        sheet == "new" -> LiftSheet(null, kinds, kinds.firstOrNull()?.key ?: "", onDismiss = { sheet = "" },
            onSave = { e -> vm.updateSettings { s -> s.copy(exercises = s.exercises + e) }; sheet = "" }, onDelete = null)
        sheet.startsWith("lift:") -> s.exercises.firstOrNull { it.id == sheet.removePrefix("lift:") }?.let { e ->
            LiftSheet(e, kinds, e.kind, onDismiss = { sheet = "" },
                onSave = { ne -> vm.updateSettings { s -> s.copy(exercises = s.exercises.map { if (it.id == e.id) ne else it }) }; sheet = "" },
                onDelete = {
                    haptics.tap()
                    vm.updateWithUndo("${e.name} deleted") { s -> s.copy(exercises = s.exercises.filter { it.id != e.id }, plans = s.plans.mapValues { (_, p) -> p.copy(lifts = p.lifts - e.id) }) }
                    sheet = ""
                })
        } ?: run { sheet = "" }
        sheet.startsWith("kind:") -> sheet.removePrefix("kind:").takeIf { it in s.liftKinds }?.let { k ->
            KindSheet(k, "lifts", onDismiss = { sheet = "" },
                onSave = { name ->
                    vm.updateSettings { s ->
                        val n = name.trim()
                        val other = s.liftKinds.firstOrNull { it != k && it.equals(n, ignoreCase = true) }
                        val to = other ?: n
                        if (n.isBlank()) s
                        else s.copy(
                            liftKinds = if (other != null) s.liftKinds - k else s.liftKinds.map { if (it == k) to else it },
                            exercises = s.exercises.map { if (it.kind == k) it.copy(kind = to) else it },
                        )
                    }
                    sheet = ""
                },
                onRemove = {
                    haptics.tap()
                    vm.updateWithUndo("$k removed") { s -> s.copy(liftKinds = s.liftKinds - k, exercises = s.exercises.map { if (it.kind == k) it.copy(kind = "") else it }) }
                    sheet = ""
                })
        } ?: run { sheet = "" }
    }
}

@Composable
private fun LiftSheet(e: Exercise?, kinds: List<LibKind>, kind0: String, onDismiss: () -> Unit, onSave: (Exercise) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember(e) { mutableStateOf(e?.name ?: "") }
    var kind by remember(e) { mutableStateOf(kind0) }
    var sets by remember(e) { mutableStateOf(e?.sets ?: 3) }
    var reps by remember(e) { mutableStateOf(e?.repTarget ?: "8 to 12") }
    var step by remember(e) { mutableStateOf(e?.let { fmtStep(it.step) } ?: "5") }
    val dirty = name != (e?.name ?: "") || kind != kind0 || sets != (e?.sets ?: 3) || reps != (e?.repTarget ?: "8 to 12") || step != (e?.let { fmtStep(it.step) } ?: "5")
    Sheet(if (e == null) "New lift" else "Edit lift", onDismiss, dirty = dirty) {
        SheetField("Name", name, { name = it }, text = true, big = true, placeholder = "What is it")
        SheetLabel("Kind")
        KindPicker(kinds, kind) { kind = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                SheetLabel("Sets")
                MiniStepper(sets.toString(), onMinus = { sets = (sets - 1).coerceAtLeast(1) }, onPlus = { sets = (sets + 1).coerceAtMost(10) })
            }
            SheetField("Reps", reps, { reps = it }, Modifier.weight(1f), text = true, placeholder = "8 to 12")
        }
        SheetField("Lb per tap", step, { step = it }, unit = "0 for bodyweight", last = true)
        SheetButtons(
            if (e == null) "Add it" else "Done", enabled = name.isNotBlank(),
            onPrimary = {
                val stp = step.num() ?: 0.0
                onSave(Exercise(
                    e?.id ?: "x${System.currentTimeMillis()}", name.trim(), e?.block ?: Block.OFF, sets, reps.trim(), stp,
                    unit = if (stp > 0) (e?.unit?.ifEmpty { "Lb" } ?: "Lb") else "", bwLabel = if (stp > 0) null else (e?.bwLabel ?: "Body"), kind = kind,
                ))
            },
            secondary = if (e == null) "Cancel" else "Delete", onSecondary = onDelete ?: onDismiss,
        )
    }
}

internal fun fmtStep(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else ProgramEngine.fmt1(v)
