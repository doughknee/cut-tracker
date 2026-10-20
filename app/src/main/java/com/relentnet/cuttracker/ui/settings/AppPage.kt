package com.relentnet.cuttracker.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.unit
import com.relentnet.cuttracker.data.wu
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.CheckRow
import com.relentnet.cuttracker.ui.components.ChipRow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.KindCard
import com.relentnet.cuttracker.ui.components.PlainRow
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.Switch
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.delay
import java.time.LocalTime

/** How the app talks, counts and feels: the words, Today's options, the unit, haptics. The look is on [ThemePage]. */
@Composable
fun AppPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val haptics = LocalHaptics.current
    val v = CutTheme.vocab
    Screen(bottomInset = true) {
        BackHeader("Settings", "App", onBack)
        Sub("How Sendline talks, counts and feels. The look is on Theme.", size = 12)
        TodayCorner(st)

        KindCard("Words", if (s.training == null) "climbing" else "plain", sub = "Climbing says Send, hold, V grades and Beta. Something else keeps the wall and says Full day, Level and Plan.") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("climb" to "Climbing", "plain" to "Something else"), if (s.training == null) "climb" else "plain", onSelect = { k ->
                    haptics.tick(); vm.updateSettings { s -> s.copy(training = if (k == "climb") null else s.training ?: "") }
                })
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

        KindCard("Today", "") {
            PlainRow("Hide completed", "ticked items fold into a Done list under the script") { Switch(s.hideDone, "Hide completed") { haptics.tick(); vm.updateSettings { s -> s.copy(hideDone = !s.hideDone) } } }
            PlainRow("The day rolls over at", "a late night still counts as today until then") { TimePick(LocalTime.of(s.resetHour, 0)) { vm.updateSettings { s -> s.copy(resetHour = it.hour) } } }
        }

        KindCard("Units", s.unit, sub = "Every weight you read or type: the weigh-in, the widget, Weight, the targets, checkpoints and lifts. Switching back and forth loses nothing.") {
            Box(Modifier.padding(14.dp, 6.dp, 14.dp, 12.dp)) {
                ChipRow(listOf("lb" to "Pounds", "kg" to "Kilograms"), if (s.kg) "kg" else "lb", onSelect = { k -> haptics.tick(); vm.updateSettings { s -> s.copy(kg = k == "kg") } })
            }
        }

        KindCard("Feel", "") {
            PlainRow("Haptics and confetti", "a buzz on every tick, a burst on a ${v.send}") { Switch(s.haptics, "Haptics and confetti") { vm.updateSettings { s -> s.copy(haptics = !s.haptics) } } }
        }
        Sub("Applies at once, on the widget and in the reminders too.", size = 12)
    }
}

/**
 * Three rows of Today in the words, unit and Hide completed setting chosen below, so each switch shows what it does.
 * Tick all three and the day reads as done, then it resets.
 */
@Composable
private fun TodayCorner(st: AppState) {
    val c = CutTheme.colors
    val v = CutTheme.vocab
    val s = st.settings
    val haptics = LocalHaptics.current
    val last = st.weighIns.maxByOrNull { it.date }?.lb ?: s.startWeight
    var done by remember { mutableStateOf(setOf<String>()) }
    data class Row3(val id: String, val label: String, val sub: String, val xp: Int)
    val rows = listOf(
        Row3("weigh", "Weigh in · ${s.wu(last)}", "Tap to fix it.", 10),
        Row3("session", v.session, v.sessionSub, 30),
        Row3("protein", "Protein", "${s.protein} g today", 20),
    )
    val all = done.size == rows.size
    LaunchedEffect(all) { if (all) { haptics.pattern(0, 30, 40, 60); delay(1600); done = emptySet() } }
    val head by animateColorAsState(if (all) c.good else c.accent, tween(250), label = "cornerHead")
    Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline) {
        Column(Modifier.padding(14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (all) v.sent.uppercase() else "TODAY · ${v.trainDay.uppercase()}", Modifier.weight(1f), color = head, style = cutStyle(11, 800, 1f, 0.1f))
                Icon(CutIcons.Link, null, Modifier.size(13.dp), tint = c.good)
                Text(" ${if (all) 5 else 4} CHAIN", color = c.text2, style = cutStyle(10, 700, 1f, 0.08f))
            }
            val next = rows.firstOrNull { it.id !in done }?.id
            for (r in rows) if (!s.hideDone || r.id !in done) {
                CheckRow(r.label, r.sub, null, r.xp, done = r.id in done, next = r.id == next, compact = true, onClick = {
                    haptics.tick(); done = if (r.id in done) done - r.id else done + r.id
                })
            }
            if (s.hideDone && done.isNotEmpty()) Text("DONE · ${done.size}", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}
