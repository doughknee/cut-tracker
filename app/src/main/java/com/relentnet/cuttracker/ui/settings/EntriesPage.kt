package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.unit
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.beta.liftStepWords
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.MiniStepper
import com.relentnet.cuttracker.ui.components.PlainRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle

/** What a new lift or meal starts with, and the gym's one comfort: a screen that stays on. */
@Composable
fun EntriesPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val p = s.prefs
    val v = CutTheme.vocab
    Screen(bottomInset = true) {
        BackHeader("You", "Meals and lifts", onBack)
        Sub("What a new lift or meal on ${v.beta} starts with. Lifts and meals you already have stay as they are.", size = 12)

        // A new lift as the sheet will open it.
        Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline, padding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A NEW LIFT AND MEAL START WITH", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Bottom) {
                    for ((label, value) in listOf("Sets" to "${p.liftSets}", "Reps" to p.liftReps.ifBlank { "any" }, "${s.unit} per tap" to liftStepWords(s))) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(label.uppercase(), color = c.text2, style = cutStyle(9, 800, 1f, 0.08f))
                            Text(value, color = c.text, style = cutStyle(20, 800, 1f, tabular = true))
                        }
                    }
                }
                Text(
                    if (p.foodPortion) "A food you add to a meal starts at one portion, like one egg." else "A food you add to a meal starts at 100 g.",
                    color = c.text2, style = cutStyle(12, 500, 1.3f),
                )
            }
        }

        KindCard("New lifts", "") {
            PlainRow("Sets", "how many a new lift asks for") {
                MiniStepper("${p.liftSets}", onMinus = { vm.updatePrefs { it.copy(liftSets = (it.liftSets - 1).coerceAtLeast(1)) } }, onPlus = { vm.updatePrefs { it.copy(liftSets = (it.liftSets + 1).coerceAtMost(10)) } })
            }
            var reps by rememberSaveable { mutableStateOf(p.liftReps) }
            PlainRow("Reps", "words or a number, like 8 to 12 or 5") {
                Field(
                    reps, { reps = it; vm.updatePrefs { pr -> pr.copy(liftReps = it.trim()) } }, Modifier.width(120.dp),
                    keyboard = KeyboardType.Text, fontSize = 15, placeholder = "8 to 12", background = c.ground,
                )
            }
            val usual = if (s.kg) 2.5 else 5.0
            ChoiceRow(
                "Weight per tap", "what one plus adds on the lift's stepper",
                listOf(0.5, 1.0, 2.0).map { k -> k.toString() to ((usual * k).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } + " " + s.unit) },
                p.liftStepScale.toString(),
            ) { k -> vm.updatePrefs { it.copy(liftStepScale = k.toDouble()) } }
        }

        KindCard("New meals", "") {
            SwitchRow("Foods start at one portion", "a food from the search starts at its first portion, like one egg. Off, every food starts at 100 g", p.foodPortion) {
                vm.updatePrefs { it.copy(foodPortion = !it.foodPortion) }
            }
        }

        KindCard("At the gym", "") {
            SwitchRow("Keep the screen on", "while the lifts are open on Today, so a rest between sets does not lock the phone", p.keepScreenOn) {
                vm.updatePrefs { it.copy(keepScreenOn = !it.keepScreenOn) }
            }
        }
        Sub("Each lift's own sets, reps and step are changed on ${v.beta}, Lifts.", size = 12, modifier = Modifier.padding(bottom = 4.dp))
    }
}
