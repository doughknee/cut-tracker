package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import com.relentnet.cuttracker.data.Dates
import com.relentnet.cuttracker.data.shown
import com.relentnet.cuttracker.data.unit
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.TileLabel
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import com.relentnet.cuttracker.ui.weight.WeightChart
import com.relentnet.cuttracker.ui.weight.heroNumber

/** How Weight and Today's weight tile read: the big number, its decimals, and what the chart draws. */
@Composable
fun WeightPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val p = s.prefs
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    val latest = st.weighIns.maxByOrNull { it.date }
    Screen(bottomInset = true) {
        BackHeader("You", "Weight", onBack)
        Sub("How Weight reads. The number and the chart below are yours, drawn the way these switches say.", size = 12)

        // The real thing: the big number and the chart, from your readings.
        Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline, padding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val heroLatest = p.weightHero == "latest" && latest != null
                TileLabel(if (heroLatest) "Latest · ${Dates.md(latest!!.date)}" else "7-day average")
                Row(verticalAlignment = Alignment.Bottom) {
                    val n = if (heroLatest) latest!!.lb else avg
                    Text(if (n == null) "--" else heroNumber(s.shown(n).toFloat(), s), color = c.text, style = cutStyle(34, 800, 1f, -0.02f, tabular = true))
                    Text(" ${s.unit}", color = c.text2, style = cutStyle(13, 600, 1f), modifier = Modifier.padding(bottom = 4.dp))
                }
                WeightChart(st)
            }
        }

        KindCard("The big number", "", sub = "On Weight and on Today's weight tile. The pace sentence always comes from the 7-day average.") {
            ChoiceRow("Shows", null, listOf("average" to "7-day average", "latest" to "Latest reading"), p.weightHero) { k -> vm.updatePrefs { it.copy(weightHero = k) } }
            // The chips read the big number as it stands, one way and the other.
            val sample = s.shown((if (p.weightHero == "latest") latest?.lb else null) ?: avg ?: latest?.lb ?: s.startWeight).toFloat()
            ChoiceRow(
                "Decimals", "on the big number, the pace and the rate. The weigh-in row keeps its tenth",
                listOf("1" to ProgramEngine.fmt1(sample.toDouble()), "0" to Math.round(sample).toString()), p.weightDecimals.toString(),
            ) { k -> vm.updatePrefs { it.copy(weightDecimals = k.toInt()) } }
        }

        KindCard("The chart", "") {
            // Only the windows shorter than the route are worth a chip.
            val windows = listOf(0 to "Whole route", 4 to "Last 4 weeks", 8 to "Last 8 weeks").filter { it.first == 0 || it.first < s.lengthWeeks }
            ChoiceRow("Shows", null, windows.map { it.first.toString() to it.second }, p.chartWeeks.toString()) { k -> vm.updatePrefs { it.copy(chartWeeks = k.toInt()) } }
            SwitchRow("Every reading", "a dot for each weigh-in, without holding the chart", p.chartDots) { vm.updatePrefs { it.copy(chartDots = !it.chartDots) } }
            SwitchRow("The rate line", "where the current rate lands by the end", p.chartRate) { vm.updatePrefs { it.copy(chartRate = !it.chartRate) } }
            SwitchRow("Checkpoints", "a ring at each checkpoint's target", p.chartCheckpoints) { vm.updatePrefs { it.copy(chartCheckpoints = !it.chartCheckpoints) } }
        }
        Sub("Readings are typed on Today's weigh-in row. Pounds or kilograms live on Words and units.", size = 12)
    }
}
