package com.relentnet.cuttracker.ui.beta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.LibMeal
import com.relentnet.cuttracker.data.Meal
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetField
import com.relentnet.cuttracker.ui.components.SheetLabel

/** Meals: everything you can put on a day, grouped by kind. A kind is a slot on the script (Breakfast, Lunch, ...). */
@Composable
fun MealsPage(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    var sheet by rememberSaveable { mutableStateOf("") }   // "", "new", "meal:<id>", "kind:<slot id>"
    val kinds = s.meals.map { LibKind(it.id, it.name) }
    val ws = ProgramEngine.weekStart(st.today)
    val uses = remember(st.logs) {
        st.logs.filter { !it.date.isBefore(ws) }.flatMap { it.picks.values }.filter { it.startsWith("lib:") }.groupingBy { it.removePrefix("lib:") }.eachCount()
    }
    val rows = s.library.sortedByDescending { it.protein }.map { m ->
        val n = uses[m.id] ?: 0
        LibRow(m.id, if (s.meals.any { it.id == m.kind }) m.kind else "", m.protein.toString(), "g", m.name, "${m.calories} cal" + (if (n > 0) ", $n× this week" else ""))
    }
    LibraryScreen(
        "Meals", "${s.library.size} meals you can put on a day. The number is protein.", onBack,
        kinds, rows, searchHint = "Search meals",
        onRow = { sheet = "meal:" + it.key }, onAdd = { sheet = "new" }, onEditKind = { sheet = "kind:" + it.key },
        onNewKind = { name -> vm.saveSettings(addMealKind(s, name)) },
        empty = "No meals yet. Tap + to add one.",
    )
    when {
        sheet == "new" -> MealSheet(null, kinds, kinds.firstOrNull()?.key ?: "", onDismiss = { sheet = "" },
            onSave = { m -> vm.saveSettings(s.copy(library = s.library + m)); sheet = "" }, onDelete = null)
        sheet.startsWith("meal:") -> s.library.firstOrNull { it.id == sheet.removePrefix("meal:") }?.let { m ->
            MealSheet(m, kinds, m.kind, onDismiss = { sheet = "" },
                onSave = { nm -> vm.saveSettings(s.copy(library = s.library.map { if (it.id == m.id) nm else it })); sheet = "" },
                onDelete = {
                    haptics.tap()
                    vm.saveSettings(s.copy(library = s.library.filter { it.id != m.id }, meals = s.meals.map { if (it.defaultLib == m.id) it.copy(defaultLib = null) else it }))
                    sheet = ""
                })
        } ?: run { sheet = "" }
        sheet.startsWith("kind:") -> s.meals.firstOrNull { it.id == sheet.removePrefix("kind:") }?.let { slot ->
            KindSheet(slot.name, "meals", onDismiss = { sheet = "" },
                onSave = { name -> vm.saveSettings(s.copy(meals = s.meals.map { if (it.id == slot.id) it.copy(name = name) else it })); sheet = "" },
                onRemove = {
                    haptics.tap()
                    vm.saveSettings(s.copy(
                        meals = s.meals.filter { it.id != slot.id },
                        library = s.library.map { if (it.kind == slot.id) it.copy(kind = "") else it },
                        plans = s.plans.mapValues { (_, p) -> p.copy(slots = p.slots - slot.id) },
                    ))
                    sheet = ""
                })
        } ?: run { sheet = "" }
    }
}

/** A new kind is a new slot on the script, on for every kind of day until a day page says otherwise. */
fun addMealKind(s: Settings, name: String): Settings {
    val id = "m" + System.currentTimeMillis()
    return s.copy(meals = s.meals + Meal(id, name, "", "", 0, 0, 10, true, null), plans = s.plans.mapValues { (_, p) -> p.copy(slots = p.slots + id) })
}

@Composable
private fun MealSheet(m: LibMeal?, kinds: List<LibKind>, kind0: String, onDismiss: () -> Unit, onSave: (LibMeal) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember(m) { mutableStateOf(m?.name ?: "") }
    var kind by remember(m) { mutableStateOf(kind0) }
    var pro by remember(m) { mutableStateOf(m?.protein?.toString() ?: "") }
    var cal by remember(m) { mutableStateOf(m?.calories?.toString() ?: "") }
    var recipe by remember(m) { mutableStateOf(m?.recipe ?: "") }
    Sheet(if (m == null) "New meal" else "Edit meal", onDismiss) {
        SheetField("Name", name, { name = it }, text = true, big = true, placeholder = "What is it")
        SheetLabel("Kind")
        KindPicker(kinds, kind) { kind = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetField("Protein", pro, { pro = it }, Modifier.weight(1f), unit = "g")
            SheetField("Calories", cal, { cal = it }, Modifier.weight(1f))
        }
        SheetField("Recipe or note", recipe, { recipe = it }, text = true, last = true, placeholder = "Optional")
        SheetButtons(
            if (m == null) "Add it" else "Done", enabled = name.isNotBlank(),
            onPrimary = { onSave(LibMeal(m?.id ?: "lib_${System.currentTimeMillis()}", name.trim(), pro.toIntOrNull() ?: 0, cal.toIntOrNull() ?: 0, recipe.trim(), kind)) },
            secondary = if (m == null) "Cancel" else "Delete", onSecondary = onDelete ?: onDismiss,
        )
    }
}

/** Rename or remove a kind. Removing keeps what was in it; those rows move to Other. */
@Composable
fun KindSheet(name0: String, noun: String, onDismiss: () -> Unit, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var name by remember(name0) { mutableStateOf(name0) }
    Sheet("Edit kind", onDismiss) {
        SheetField("Kind", name, { name = it }, text = true, big = true, last = true)
        SheetNote("Removing a kind keeps its $noun. They move to Other.")
        SheetButtons("Done", onPrimary = { onSave(name.trim().ifBlank { name0 }) }, secondary = "Remove kind", onSecondary = onRemove, enabled = name.isNotBlank())
    }
}
