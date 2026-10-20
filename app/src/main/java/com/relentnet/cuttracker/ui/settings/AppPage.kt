package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.Clock
import com.relentnet.cuttracker.data.Vocab
import com.relentnet.cuttracker.data.unit
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.ChipRow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalTime

/** How the app talks and counts: the words, the weight unit, the week, the clock. */
@Composable
fun AppPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val p = s.prefs
    val haptics = LocalHaptics.current
    val context = LocalContext.current
    Screen(bottomInset = true) {
        BackHeader("You", "Words and units", onBack)
        Sub("How Sendline talks and counts. Switching back and forth loses nothing.", size = 12)

        KindCard("Words", if (s.training == null) "climbing" else "plain", sub = "Climbing says Send, hold, V grades and Beta. Something else keeps the wall and says Full day, Level and Plan.") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("climb" to "Climbing", "plain" to "Something else"), if (s.training == null) "climb" else "plain", onSelect = { k ->
                    haptics.tick(); vm.updateSettings { s -> s.copy(training = if (k == "climb") null else s.training ?: "") }
                })
            }
            // The same four words in both voices, side by side, so the choice is visible before it is made.
            val climb = Vocab.CLIMBING
            val plain = Vocab.plain(s.training ?: "")
            Row(Modifier.fillMaxWidth().padding(14.dp, 0.dp, 14.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((voice, on) in listOf(climb to (s.training == null), plain to (s.training != null))) {
                    Card(Modifier.weight(1f), radius = 12.dp, background = c.ground, border = if (on) c.accent else c.hairline) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (w in listOf(voice.trainDay, "3 ${voice.sends}", voice.level(3), voice.beta + " tab")) {
                                Text(w, color = if (on) c.text else c.text2, style = cutStyle(12, 600, 1.3f))
                            }
                        }
                    }
                }
            }
            if (s.training != null) {
                var name by rememberSaveable { mutableStateOf(s.training ?: "") }
                Column(Modifier.padding(14.dp, 0.dp, 14.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("WHAT YOU CALL YOUR TRAINING", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f))
                    Field(
                        name, { name = it; vm.updateSettings { s -> s.copy(training = it.trim()) } }, Modifier.fillMaxWidth(),
                        keyboard = KeyboardType.Text, fontSize = 16, placeholder = "Run, Gym, Practice", background = c.surface,
                    )
                }
            }
        }

        KindCard("Weight", s.unit, sub = "Every weight you read or type: the weigh-in, the widget, Weight, the targets, checkpoints and lifts. Your history is kept exactly.") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("lb" to "Pounds", "kg" to "Kilograms"), if (s.kg) "kg" else "lb", onSelect = { k -> haptics.tick(); vm.updateSettings { s -> s.copy(kg = k == "kg") } })
            }
        }

        KindCard("The week", if (p.weekStartsSunday) "Sunday" else "Monday", sub = "Where the wall, This week, the workout count and the Route calendar start each week.") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("mon" to "Monday", "sun" to "Sunday"), if (p.weekStartsSunday) "sun" else "mon", onSelect = { k -> haptics.tick(); vm.updatePrefs { it.copy(weekStartsSunday = k == "sun") } })
            }
        }

        KindCard("The clock", Clock.text(context, LocalTime.of(19, 30)), sub = "Times in reminders and settings. The phone's choice unless you pick one.") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("phone" to "Phone", "12" to "12-hour", "24" to "24-hour"), p.clock, onSelect = { k -> haptics.tick(); vm.updatePrefs { it.copy(clock = k) } })
            }
        }
        Sub("Applies at once, on the widget and in the reminders too.", size = 12)
    }
}
