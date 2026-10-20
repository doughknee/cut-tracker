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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.wu
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.CheckRow
import com.relentnet.cuttracker.ui.components.ChipRow
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

/** What Today shows and how it behaves: the wall, the script, the day's edges, where the app opens. */
@Composable
fun TodayPage(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val p = s.prefs
    val v = CutTheme.vocab
    Screen(bottomInset = true) {
        BackHeader("You", "Today", onBack)
        Sub("What Today shows and how it behaves. The card below follows every switch.", size = 12)
        TodayCorner(st)

        KindCard("The wall", "") {
            SwitchRow("The tape", "the week on an orange tape; its arrows step the week. Off, swipe the wall instead", p.showTape) { vm.updatePrefs { it.copy(showTape = !it.showTape) } }
            SwitchRow("The rope", "the line that joins the days of your chain", p.showRope) { vm.updatePrefs { it.copy(showRope = !it.showRope) } }
            SwitchRow("Bigger ${v.holds}", "every ${v.hold} on the wall a size up, the day number too", p.bigHolds) { vm.updatePrefs { it.copy(bigHolds = !it.bigHolds) } }
            SwitchRow("Day names", "MON, TUE under each ${v.hold}", p.weekdayLabels) { vm.updatePrefs { it.copy(weekdayLabels = !it.weekdayLabels) } }
            SwitchRow("Holiday decorations", "on the wall and the ${v.holds} when the calendar look brings some", p.wallDecor) { vm.updatePrefs { it.copy(wallDecor = !it.wallDecor) } }
        }

        KindCard("The script", "") {
            SwitchRow("Hide completed", "ticked items fold into a Done list under the script", s.hideDone) { vm.updateSettings { s -> s.copy(hideDone = !s.hideDone) } }
            if (s.hideDone) SwitchRow("Done list starts open", "the folded items show without a tap", p.doneOpen) { vm.updatePrefs { it.copy(doneOpen = !it.doneOpen) } }
            SwitchRow("Up next", "the next item to do is raised, with UP NEXT over it", p.showUpNext) { vm.updatePrefs { it.copy(showUpNext = !it.showUpNext) } }
            SwitchRow("The line under each item", "the plan's note, the last weigh-in", p.showSubs) { vm.updatePrefs { it.copy(showSubs = !it.showSubs) } }
            SwitchRow("Strike through ticked items", "a line through what is done", p.strikeDone) { vm.updatePrefs { it.copy(strikeDone = !it.strikeDone) } }
            SwitchRow("XP on each item", "the +10 on the right of every row", p.showXp) { vm.updatePrefs { it.copy(showXp = !it.showXp) } }
            SwitchRow("Times on each item", "when each meal or block is planned", p.showTimes) { vm.updatePrefs { it.copy(showTimes = !it.showTimes) } }
            SwitchRow("Swipe to tick", "a swipe to the right ticks a row. Off, rows tick by tap", p.swipeToTick) { vm.updatePrefs { it.copy(swipeToTick = !it.swipeToTick) } }
        }

        KindCard("The numbers", "") {
            SwitchRow("Protein", "grams so far and the target, beside the kind of day", p.showProtein) { vm.updatePrefs { it.copy(showProtein = !it.showProtein) } }
            SwitchRow("Calories", "beside protein in the day's totals", p.showCalories) { vm.updatePrefs { it.copy(showCalories = !it.showCalories) } }
            SwitchRow("The chain", "the count at the top right. It still counts with this off", p.showChain) { vm.updatePrefs { it.copy(showChain = !it.showChain) } }
            SwitchRow("The weight tile", "the 7-day average at the foot of Today. Off, Weight is the link beside Route at the top", p.showWeightTile) { vm.updatePrefs { it.copy(showWeightTile = !it.showWeightTile) } }
        }

        KindCard("The day", "") {
            PlainRow("The day rolls over at", "a late night still counts as today until then") { TimePick(LocalTime.of(s.resetHour, 0)) { vm.updateSettings { s -> s.copy(resetHour = it.hour) } } }
            SwitchRow("The evening card", "past the pick-a-day time, a card that guesses Rest day", p.eveningCard) { vm.updatePrefs { it.copy(eveningCard = !it.eveningCard) } }
            ChoiceRow(
                "Weigh-in steps", "the minus and plus beside the weigh-in, here and on the widget",
                listOf("0.1" to "0.1", "0.2" to "0.2", "0.5" to "0.5"), ProgramEngine.fmt1(p.weighStep),
            ) { k -> vm.updatePrefs { it.copy(weighStep = k.toDouble()) } }
        }

        KindCard("Opening the app", "") {
            ChoiceRow(
                "Open on", "the tab you land on when the app starts",
                listOf("today" to "Today", "beta" to v.beta, "route" to "Route"), p.startTab,
            ) { k -> vm.updatePrefs { it.copy(startTab = k) } }
        }
    }
}

/**
 * A row with a switch. The whole row is the toggle, so it is a big target and TalkBack reads it once:
 * the title, the line under it, then on or off.
 */
@Composable
internal fun SwitchRow(title: String, sub: String, on: Boolean, onToggle: () -> Unit) {
    val haptics = LocalHaptics.current
    Box(Modifier.toggleable(value = on, role = Role.Switch, onValueChange = { haptics.tick(); onToggle() })) {
        PlainRow(title, sub) { Switch(on, interactive = false) {} }
    }
}

/** A titled choice: the words on top, the chips under them. */
@Composable
internal fun ChoiceRow(title: String, sub: String?, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val haptics = LocalHaptics.current
    PlainRow(title, sub) {}
    Box(Modifier.padding(14.dp, 0.dp, 14.dp, 12.dp)) {
        ChipRow(options, selected, onSelect = { haptics.tick(); onSelect(it) })
    }
}

/**
 * Three rows of Today in the words, unit and Today options chosen here, so each switch shows what it does.
 * Tick all three and the day reads as done, then it resets.
 */
@Composable
internal fun TodayCorner(st: AppState) {
    val c = CutTheme.colors
    val v = CutTheme.vocab
    val s = st.settings
    val haptics = LocalHaptics.current
    val last = st.weighIns.maxByOrNull { it.date }?.lb ?: s.startWeight
    var done by remember { mutableStateOf(setOf<String>()) }
    data class Row3(val id: String, val label: String, val sub: String, val time: String, val xp: Int)
    val rows = listOf(
        Row3("weigh", "Weigh in · ${s.wu(last)}", "Tap to fix it.", "8:30", 10),
        Row3("session", v.session, v.sessionSub, "11:00", 30),
        Row3("protein", "Protein", "${s.protein} g today", "any", 20),
    )
    val all = done.size == rows.size
    LaunchedEffect(all) { if (all) { haptics.pattern(0, 30, 40, 60); delay(1600); done = emptySet() } }
    val head by animateColorAsState(if (all) c.good else c.accent, tween(250), label = "cornerHead")
    Card(Modifier.fillMaxWidth(), radius = 16.dp, background = c.ground, border = c.hairline) {
        Column(Modifier.padding(14.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (all) v.sent.uppercase() else "TODAY · ${v.trainDay.uppercase()}", Modifier.weight(1f), color = head, style = cutStyle(11, 800, 1f, 0.1f))
                if (s.prefs.showChain) {
                    Icon(CutIcons.Link, null, Modifier.size(13.dp), tint = c.good)
                    Text(" ${if (all) 5 else 4} CHAIN", color = c.text2, style = cutStyle(10, 700, 1f, 0.08f))
                }
            }
            val next = rows.firstOrNull { it.id !in done }?.id
            for (r in rows) if (!s.hideDone || r.id !in done) {
                CheckRow(r.label, r.sub, r.time, r.xp, done = r.id in done, next = r.id == next, onClick = {
                    haptics.tick(); done = if (r.id in done) done - r.id else done + r.id
                }, onSwipeCheck = { haptics.tap(); done = done + r.id })
            }
            if (s.hideDone && done.isNotEmpty()) Text("DONE · ${done.size}", color = c.text2, style = cutStyle(10, 800, 1f, 0.1f), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}
