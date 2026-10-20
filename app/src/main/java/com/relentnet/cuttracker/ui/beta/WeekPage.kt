package com.relentnet.cuttracker.ui.beta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Chip
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.NumRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sheet
import com.relentnet.cuttracker.ui.components.SheetButtons
import com.relentnet.cuttracker.ui.components.SheetLabel
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.holdFill
import com.relentnet.cuttracker.ui.components.holdShape
import com.relentnet.cuttracker.ui.components.pressable
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.US)

/** How the plan is going this week. Read only; everything here is logged from Today. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun WeekPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val ws = ProgramEngine.weekStart(st.today)
    val days = (0..6).map { ws.plusDays(it.toLong()) }
    val past = days.filter { !it.isAfter(st.today) && !it.isBefore(s.startDate) && it.isBefore(s.endDate) && (it != st.today || st.day(it).totals.protein >= s.protein) }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val hit = past.count { st.day(it).totals.protein >= s.protein }
    Screen(bottomInset = true) {
        BackHeader(CutTheme.vocab.beta, "This week", onBack)
        Sub("${com.relentnet.cuttracker.data.Dates.span(ws, ws.plusDays(6))}. How the plan is going.", size = 12)
        Card(Modifier.fillMaxWidth(), radius = 14.dp, padding = PaddingValues(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (d in days) {
                    val dv = st.day(d)
                    Column(Modifier.pressable(haptic = false) { open = d.toString() }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(d.dayOfWeek.name.take(1), color = c.text2, style = cutStyle(10, 800, 1f))
                        Hold(holdShape(dv.log.mode), if (dv.result == DayResult.TODAY) c.accent else holdFill(dv.result, c), 26.dp, mark = dv.result)
                        Text("${d.dayOfMonth}", color = c.text, style = cutStyle(12, 800, 1f, tabular = true))
                    }
                }
            }
        }
        KindCard("Protein", if (past.isEmpty()) "" else "$hit of ${com.relentnet.cuttracker.ui.components.count(past.size, "day")}") {
            if (past.isEmpty()) Sub("The week starts today.", Modifier.padding(14.dp, 4.dp, 14.dp, 14.dp), size = 12)
            for (d in past) {
                val dv = st.day(d)
                val p = dv.totals.protein
                val ok = p >= s.protein
                NumRow("$p", d.format(dayFmt), unit = "g", sub = loggedMeals(st, d).ifEmpty { "nothing logged" }, right = if (ok) "hit" else "short", rightColor = if (ok) c.good else c.accent, onClick = { open = d.toString() })
            }
        }
        KindCard("Workouts", "${st.workoutsThisWeek} of ${s.workoutsPerWeek}") {
            val planned = days.map { d -> d to st.day(d) }.filter { (_, dv) -> dv.items.any { it.workout && it.id != ProgramSpec.ITEM_CLIMB } }
            if (planned.isEmpty()) Sub("No block picked yet this week. Pick a ${CutTheme.vocab.train} or Off day on Today.", Modifier.padding(14.dp, 4.dp, 14.dp, 14.dp), size = 12)
            for ((d, dv) in planned) {
                val block = dv.items.first { it.workout && it.id != ProgramSpec.ITEM_CLIMB }
                val done = block.id in dv.log.doneItemIds
                NumRow(if (done) "✓" else "–", block.label, sub = d.format(dayFmt), right = if (done) "done" else if (d.isAfter(st.today)) "ahead" else if (d == st.today) "not yet" else "not done", rightColor = if (done) c.good else c.text2, chevron = false)
            }
        }
        Sub("Read only. Everything here is logged from Today.", size = 12)
    }
    open?.let { iso ->
        val d = LocalDate.parse(iso)
        val dv = st.day(d)
        val name = when (dv.result) {
            DayResult.SEND -> CutTheme.vocab.send; DayResult.MIN -> "Min day, chain safe"; DayResult.FREEZE -> "Missed, freeze spent"; DayResult.SKIP -> "Planned skip"
            DayResult.MISSED -> "Missed"; DayResult.TODAY -> "Today"; DayResult.FUTURE -> "Ahead"; DayResult.BEFORE -> "Before the start"
        }
        Sheet(com.relentnet.cuttracker.data.Dates.wwmd(d), onDismiss = { open = null }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Hold(holdShape(dv.log.mode), if (dv.result == DayResult.TODAY) c.accent else holdFill(dv.result, c), 40.dp, mark = dv.result)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, color = c.text, style = cutStyle(17, 800, 1f, display = true))
                    Text(dv.log.mode?.let { CutTheme.vocab.day(it) } ?: "No day picked", color = c.text2, style = cutStyle(12, 500, 1f))
                }
            }
            if (dv.items.isNotEmpty()) {
                SheetLabel("Script")
                Sub("${dv.totals.done} of ${dv.totals.total} ticked, ${dv.totals.protein} g protein, ${dv.totals.calories} cal", size = 12)
            }
            val meals = loggedMealList(st, d)
            if (meals.isNotEmpty()) {
                SheetLabel("Logged")
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in meals) Chip(m, on = false) {}
                }
            }
            val editable = !d.isAfter(st.today) && !d.isBefore(st.settings.startDate)
            SheetButtons("Close", onPrimary = { open = null }, secondary = if (editable) "Edit on Today" else null, onSecondary = {
                open = null
                vm.viewDate = d.takeIf { it != st.today }
                vm.requestTab(com.relentnet.cuttracker.ui.Routes.TODAY)
            })
        }
    }
}

private fun loggedMealList(st: AppState, d: LocalDate): List<String> {
    val dv = st.day(d)
    return dv.items.filter { it.pick && it.id in dv.log.doneItemIds }.map { it.sub.substringBefore(". ") }
}

private fun loggedMeals(st: AppState, d: LocalDate) = loggedMealList(st, d).joinToString(", ")
