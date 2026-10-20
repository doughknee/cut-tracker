package com.relentnet.cuttracker.ui.beta

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import java.time.LocalDate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.MiniStepper
import com.relentnet.cuttracker.ui.components.PlainRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** What a day has to hit, and where the route ends. Every tap saves; the math card follows. */
@Composable
fun TargetsPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    fun set(f: (Settings) -> Settings) { haptics.tick(); vm.updateSettings { s -> f(s) } }
    val total = s.startWeight - s.targetWeight
    val rate = total / s.lengthWeeks
    val pct = if (s.startWeight > 0) rate / s.startWeight * 100 else 0.0
    val (note, warn) = when {
        total <= 0 -> "The goal is not below the start. The route has nothing to cut." to true
        pct > 1 -> "Faster than 1% of bodyweight a week. The pace line will be steep and the freezes will come quickly." to true
        pct < 0.4 -> "A gentle cut. Plenty of room for a bad week." to false
        else -> "A steady cut. The pace line and the checkpoints follow these numbers." to false
    }
    Screen(bottomInset = true) {
        BackHeader("Beta", "Targets", onBack)
        Sub("What a day has to hit, and where the route ends.", size = 12)
        KindCard("Every day", "") {
            PlainRow("Protein", "the one that keeps the chain") { MiniStepper("${s.protein}", { set { it.copy(protein = (it.protein - 5).coerceAtLeast(0)) } }, { set { it.copy(protein = it.protein + 5) } }, "g") }
            PlainRow("Calories", "stay under") { MiniStepper("%,d".format(Locale.US, s.calories), { set { it.copy(caloriesP1 = (it.caloriesP1 - 50).coerceAtLeast(0)) } }, { set { it.copy(caloriesP1 = it.caloriesP1 + 50) } }, "cal") }
        }
        KindCard("Every week", "") {
            PlainRow("Workouts", "blocks on Climb and Off days") { MiniStepper("${s.workoutsPerWeek}", { set { it.copy(workoutsPerWeek = (it.workoutsPerWeek - 1).coerceAtLeast(0)) } }, { set { it.copy(workoutsPerWeek = (it.workoutsPerWeek + 1).coerceAtMost(7)) } }, "a week") }
        }
        KindCard("The route", "") {
            PlainRow("Start", "weighed on ${s.startDate.format(shortDate)}") { MiniStepper(ProgramEngine.fmt1(s.startWeight), { set { it.copy(startWeight = it.startWeight - 0.5) } }, { set { it.copy(startWeight = it.startWeight + 0.5) } }, "lb") }
            PlainRow("Goal", "where the route ends") { MiniStepper(ProgramEngine.fmt1(s.targetWeight), { set { it.copy(targetWeight = it.targetWeight - 0.5) } }, { set { it.copy(targetWeight = it.targetWeight + 0.5) } }, "lb") }
            PlainRow("Weeks", "${s.startDate.format(shortDate)} to ${s.endDate.format(shortDate)}") { MiniStepper("${s.lengthWeeks}", { set { it.copy(lengthWeeks = (it.lengthWeeks - 1).coerceAtLeast(1)) } }, { set { it.copy(lengthWeeks = (it.lengthWeeks + 1).coerceAtMost(52)) } }, "wk") }
            PlainRow("Starts", "day 1 of the route; what you logged is kept") {
                val context = LocalContext.current
                val shape = RoundedCornerShape(8.dp)
                Box(
                    Modifier.height(36.dp).clip(shape).background(c.ground).border(1.dp, c.mutedBorder, shape).clickable(role = Role.Button) {
                        DatePickerDialog(context, { _, y, m, d -> vm.updateSettings { s -> s.copy(startDate = LocalDate.of(y, m + 1, d)) } }, s.startDate.year, s.startDate.monthValue - 1, s.startDate.dayOfMonth).show()
                    }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(s.startDate.format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)), color = c.text, style = cutStyle(14, 800, 1f, tabular = true)) }
            }
        }
        Card(Modifier.fillMaxWidth(), radius = 14.dp, padding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Big(ProgramEngine.fmt1(total), "lb to lose")
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ProgramEngine.fmt1(rate), color = c.text, style = cutStyle(24, 800, 1f, -0.02f, display = true, tabular = true))
                    Text("lb a week, ${"%.2f".format(Locale.US, pct)}% of bodyweight", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true), modifier = Modifier.padding(bottom = 2.dp))
                }
                Text(note, color = if (warn) c.accent else c.text2, style = cutStyle(12, 500, 1.4f))
            }
        }
        Sub("Changing the start, the goal or the weeks redraws the pace line and moves every checkpoint that sits on it.", size = 12)
    }
}

@Composable
private fun Big(n: String, label: String) {
    val c = CutTheme.colors
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(n, color = c.text, style = cutStyle(24, 800, 1f, -0.02f, display = true, tabular = true))
        Text(label, color = c.text2, style = cutStyle(12, 600, 1f), modifier = Modifier.padding(bottom = 2.dp))
    }
}
