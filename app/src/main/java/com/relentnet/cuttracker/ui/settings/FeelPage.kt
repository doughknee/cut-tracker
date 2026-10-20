package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.holdFill
import com.relentnet.cuttracker.ui.components.holdNumberColor
import com.relentnet.cuttracker.ui.today.resultName

/** Text size, contrast, motion, celebrations and haptics. Every switch here applies to the whole app at once. */
@Composable
fun FeelPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val p = s.prefs
    val v = CutTheme.vocab
    Screen(bottomInset = true) {
        BackHeader("You", "Feel and access", onBack)
        Sub("How the app reads, moves and answers. The whole app follows each switch at once.", size = 12)

        // A piece of Today at the chosen size and contrast.
        Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TODAY · 117 G TO A MIN DAY", color = c.accent, style = cutStyle(11, 800, 1f, 0.1f))
                Text("Tuesday 20", color = c.text, style = cutStyle(36, 800, 1f, -0.03f, display = true))
                Text("Day 7 of ${s.totalDays}. Weigh in, eat the script, tick it off.", color = c.text2, style = cutStyle(13, 500, 1.35f))
                Text("Quiet words read like this", color = c.dim, style = cutStyle(12, 600, 1.2f))
                // Four days of the wall: the marks switch and the plain wall show here first.
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    for ((i, rs) in listOf(DayResult.SEND to HoldShape.CRIMP, DayResult.MIN to HoldShape.JUG, DayResult.FREEZE to HoldShape.SLOPER, DayResult.MISSED to HoldShape.PINCH).withIndex()) {
                        val (r, shape) = rs
                        Hold(shape, holdFill(r, c), 40.dp, mark = r, modifier = Modifier.semantics { contentDescription = resultName(r, v) }) {
                            Text("${16 + i}", color = holdNumberColor(r, c), style = cutStyle(13, 900, 1f, tabular = true))
                        }
                    }
                }
            }
        }

        KindCard("Reading", "") {
            ChoiceRow(
                "Text size", "on top of the phone's own size",
                listOf("1.0" to "Phone", "1.15" to "Large", "1.3" to "Larger"), when { p.textScale > 1.2f -> "1.3"; p.textScale > 1.05f -> "1.15"; else -> "1.0" },
            ) { k -> vm.updatePrefs { it.copy(textScale = k.toFloat()) } }
            SwitchRow("Bold text", "heavier words everywhere, on top of the phone's own setting", p.boldText) { vm.updatePrefs { it.copy(boldText = !it.boldText) } }
            SwitchRow("Higher contrast", "brighter secondary words and borders", p.highContrast) { vm.updatePrefs { it.copy(highContrast = !it.highContrast) } }
        }

        KindCard("Seeing the wall", "") {
            SwitchRow("Marks on the ${v.holds}", "a tick, dot, bars or cross for how each day went, so color is never the only cue", p.holdMarks) { vm.updatePrefs { it.copy(holdMarks = !it.holdMarks) } }
            SwitchRow("A plain wall", "no dots, stripes or chalk glow behind the ${v.holds}", p.plainWall) { vm.updatePrefs { it.copy(plainWall = !it.plainWall) } }
        }

        KindCard("Touch", "") {
            SwitchRow("Bigger tap targets", "taller chips and more room around every button and switch", p.bigTargets) { vm.updatePrefs { it.copy(bigTargets = !it.bigTargets) } }
        }

        KindCard("Motion", if (CutTheme.calm) "calm" else "full") {
            ChoiceRow(
                "Calm motion", "no confetti or bounce, and pages fade instead of sliding",
                listOf("phone" to "Follow the phone", "on" to "Always"), p.reduceMotion,
            ) { k -> vm.updatePrefs { it.copy(reduceMotion = k) } }
        }

        KindCard("Celebrations", "", sub = "What a ${v.send} and a new grade look like.") {
            ChoiceRow(
                "When you ${v.send.lowercase()}", null,
                listOf("big" to "Full screen", "quiet" to "A short note", "off" to "Nothing"), p.celebrate,
            ) { k -> vm.updatePrefs { it.copy(celebrate = k) } }
            SwitchRow("Haptics", "a buzz on every tick", s.haptics) { vm.updateSettings { s -> s.copy(haptics = !s.haptics) } }
        }
    }
}
