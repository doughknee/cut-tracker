package com.relentnet.cuttracker.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.LibMeal
import com.relentnet.cuttracker.data.Meal
import com.relentnet.cuttracker.ui.components.Switch
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.engine.ScriptItem
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.CheckRow
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.TileLabel
import com.relentnet.cuttracker.ui.components.tickedFloat
import com.relentnet.cuttracker.ui.components.tickedInt
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d", Locale.US)

@Composable
fun TodayScreen(vm: AppViewModel, onSettings: () -> Unit, onWeight: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val now by vm.nowTime.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    var weighOpen by rememberSaveable { mutableStateOf(false) }
    var pickOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var doneOpen by rememberSaveable { mutableStateOf(false) }
    var liftOpen by rememberSaveable { mutableStateOf(false) }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()

    // A past day can be edited in place; anything today or later is today.
    val date = vm.viewDate?.takeIf { it.isBefore(st.today) && !it.isBefore(st.settings.startDate) } ?: st.today
    val past = date != st.today
    val editDate: LocalDate? = if (past) date else null
    LaunchedEffect(st.today) { if (vm.viewDate != null && !vm.viewDate!!.isBefore(st.today)) vm.viewDate = null }
    // First Today after onboarding: open the rules sheet once, then never again on its own.
    LaunchedEffect(st.settings.tourSeen) { if (!st.settings.tourSeen) { infoOpen = true; vm.saveSettings(st.settings.copy(tourSeen = true)) } }
    val dv = st.day(date)
    val items = dv.items
    val done = dv.log.doneItemIds
    val mode = dv.log.mode
    val all = dv.isSend
    val remaining = items.filter { it.id !in done }
    val finished = items.filter { it.id in done }
    val next = remaining.firstOrNull()
    val late = !past && mode == null && !now.isBefore(st.settings.pickTime)
    val welcomeBack = !past && st.missedRun >= 2 && st.settings.welcomeBackSeen != st.today
    val prevWeigh = st.weighInBefore(date)

    fun closeInline() { weighOpen = false; pickOpen = null; liftOpen = false }
    fun view(d: LocalDate) {
        val t = if (d.isAfter(st.today)) st.today else if (d.isBefore(st.settings.startDate)) st.settings.startDate else d
        vm.viewDate = t.takeIf { it != st.today }
        closeInline()
    }
    val total = st.settings.protein
    val (stateLine, stateColor) = when {
        st.ended && !past -> "Route complete" to c.good
        !past && dv.isSend -> "Sent" to c.good
        !past && dv.isMin -> "Chain safe · today" to c.good
        !past && mode == null -> "Today · pick a day" to c.accent
        !past && dv.totals.protein < total -> "Today · ${total - dv.totals.protein}g to safe" to c.accent
        !past -> "Today · weigh in to safe" to c.accent
        else -> "${resultName(dv.result)} · earlier day" to when (dv.result) {
            DayResult.SEND, DayResult.MIN -> c.good
            DayResult.FREEZE -> c.freeze
            else -> c.text2
        }
    }

    Column(Modifier.fillMaxSize().background(c.ground).verticalScroll(scroll).imePadding()) {
        Wall(
            st, date, scroll,
            onPickDay = { haptics.tick(); view(it) },
            onWeek = { view(date.plusWeeks(it.toLong())) },
            onInfo = { infoOpen = true },
            onSettings = onSettings,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Fade the wall into the content edge; it stays put while the holds parallax underneath.
                    val h = 70.dp.toPx()
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, c.ground), startY = -h, endY = 0f), Offset(0f, -h), Size(size.width, h))
                }
                .background(c.ground)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 34.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stateLine.uppercase(), color = stateColor, style = cutStyle(11, 800, 1f, 0.1f, tabular = true))
                if (past) NavPill("Back to today ›", { view(st.today) }, accent = true)
                else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(CutIcons.Link, null, Modifier.size(15.dp), tint = c.good)
                    Text("${tickedInt(st.chain.count)}", color = c.text, style = cutStyle(16, 800, 1f, tabular = true))
                    Text("CHAIN", color = c.text2, style = cutStyle(10, 700, 1f, 0.08f), modifier = Modifier.padding(start = 4.dp))
                }
            }
            Text(date.format(dayFmt), color = c.text, style = cutStyle(48, 800, 1f, -0.03f, display = true), maxLines = 1)
            Sub("Day ${ProgramEngine.dayNumber(date, st.settings.startDate).coerceAtMost(st.settings.totalDays)} of ${st.settings.totalDays}", size = 12)
        }

        if (welcomeBack) WelcomeBack(st.chain.count, onOk = { vm.dismissWelcomeBack() })

        when {
            st.ended && !past -> ToppedOut(st, onNext = { vm.saveSettings(st.settings.copy(onboarded = false)) })
            late -> LateCard(
                onRest = { vm.setMode(DayMode.REST) },
                onClimb = { vm.setMode(DayMode.CLIMB, preCheck = setOf(ProgramSpec.ITEM_CLIMB)) },
            )
            mode == null -> PickCards(st, onPick = { haptics.tap(); vm.setMode(it, date = editDate) })
            else -> ModeChipRow(
                mode, dv, st.settings.protein,
                onChange = { weighOpen = false; pickOpen = null; vm.setMode(null, date = editDate) },
                // An Off day with the block unchecked is a Rest day that was logged under the old rules; one tap fixes it.
                onMakeRest = if (mode == DayMode.OFF && ProgramSpec.ITEM_CORE !in done) ({ haptics.tap(); vm.setMode(DayMode.REST, date = editDate) }) else null,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow(if (past) "Script · late checks count" else "Script")
                Eyebrow("${tickedInt(finished.size)} / ${items.size}")
            }
            if (items.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).dashedBorder(c.mutedBorder).padding(18.dp, 14.dp),
                ) {
                    Sub(if (past) "Nothing was logged. Pick what this day was and fill it in." else "Pick a day and the script shows up. Minimum day is weigh in + protein. That keeps the chain.")
                }
            }
            for (it in remaining) {
                val isNext = it === next
                CheckRow(
                    label = it.label,
                    sub = if (it.id == ProgramSpec.ITEM_WEIGH && prevWeigh != null) "Last ${ProgramEngine.fmt1(prevWeigh.lb)}. Tap to log." else it.sub,
                    time = it.time, xp = it.xp, done = false, next = isNext,
                    tag = if (it.workout && it.id != ProgramSpec.ITEM_CLIMB && ProgramEngine.weekWon(st.liftSets, st.today)) "Week won" else null,
                    onClick = {
                        when {
                            it.id == ProgramSpec.ITEM_WEIGH -> { weighOpen = !weighOpen; pickOpen = null; liftOpen = false }
                            it.workout && it.id != ProgramSpec.ITEM_CLIMB && !past -> { liftOpen = !liftOpen; weighOpen = false; pickOpen = null }
                            it.pick -> { pickOpen = if (pickOpen == it.id) null else it.id; weighOpen = false; liftOpen = false }
                            else -> { haptics.tap(); vm.toggleItem(it.id, date = editDate) }
                        }
                    },
                    onSwipeCheck = { haptics.tap(); vm.toggleItem(it.id, date = editDate) },
                )
                if (it.id == ProgramSpec.ITEM_WEIGH && weighOpen) {
                    WeighInline(
                        prefill = prevWeigh?.lb ?: st.settings.startWeight,
                        onLog = { lb -> weighOpen = false; haptics.tap(); vm.logWeight(lb, editDate) },
                    )
                }
                if (it.pick && pickOpen == it.id) {
                    val slot = st.settings.meals.firstOrNull { m -> m.id == it.id }
                    if (slot != null) MealPicker(
                        st, slot, current = dv.log.picks[it.id],
                        onPick = { ref -> pickOpen = null; haptics.tap(); vm.pickMeal(it.id, ref, editDate) },
                        onSaveLib = { m -> vm.saveLibraryMeal(m) },
                    )
                }
                if (it.workout && it.id != ProgramSpec.ITEM_CLIMB && liftOpen && !past) {
                    // Saving the session checks the block. Swiping the row checks it without a session.
                    LiftInline(vm, blocks = listOf(if (mode == DayMode.CLIMB) Block.CLIMB else Block.OFF), onSaved = { liftOpen = false; vm.toggleItem(it.id) })
                }
            }
            if (mode == DayMode.REST && !past) {
                Text(
                    if (liftOpen) "CLOSE LIFTS" else "LIFT ANYWAY",
                    color = c.text2, style = cutStyle(11, 800, 1f, 0.1f),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { liftOpen = !liftOpen }.padding(horizontal = 4.dp, vertical = 6.dp),
                )
                if (liftOpen) LiftInline(vm, blocks = listOf(Block.CLIMB, Block.OFF), onSaved = { liftOpen = false })
            }
            if (finished.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button) { doneOpen = !doneOpen }.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(CutIcons.Forward, null, Modifier.size(12.dp).rotate(if (doneOpen) 90f else 0f), tint = c.text2)
                    Text("DONE · ${finished.size}", color = c.text2, style = cutStyle(11, 700, 1f, 0.1f))
                    Box(Modifier.weight(1f).height(1.dp).background(c.hairline))
                }
                if (doneOpen) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (it in finished) CheckRow(
                        label = it.label, sub = null, time = null, xp = it.xp, done = true, compact = true,
                        onClick = { vm.toggleItem(it.id, date = editDate) },
                    )
                }
            }
        }

        XpStrip(st)
        WeightTile(st, onWeight)
        }
    }
    if (infoOpen) WallInfoSheet(onDismiss = { infoOpen = false })
}

/** 7-day average and pace. Tapping opens the full Weight sheet with the chart. */
@Composable
private fun WeightTile(st: AppState, onWeight: () -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    val shown = tickedFloat((avg ?: s.startWeight).toFloat())
    val onPace = avg != null && ProgramEngine.onPace(avg, st.dayN, s)
    Card(Modifier.fillMaxWidth().clickable(onClick = onWeight, role = Role.Button), padding = PaddingValues(14.dp, 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TileLabel("7-day average")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (avg == null) "--" else ProgramEngine.fmt1(shown.toDouble()), color = c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Text(" lb", color = c.text2, style = cutStyle(13, 600, 1f))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (avg != null) {
                    val delta = avg - s.startWeight
                    Text(
                        (if (delta < 0) "" else "+") + ProgramEngine.fmt1(delta) + " · " + (if (onPace) "on pace" else "behind pace"),
                        color = if (onPace) c.good else c.accent, style = cutStyle(12, 700, 1f, tabular = true),
                    )
                } else Text("No readings yet", color = c.text2, style = cutStyle(12, 600, 1f))
                Text("CHART ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
            }
        }
    }
}

@Composable
private fun NavPill(label: String, onClick: () -> Unit, accent: Boolean = false) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.height(32.dp).clip(shape).background(if (accent) c.accent else c.surface).border(1.dp, if (accent) c.accent else c.hairline, shape)
            .clickable(onClick = onClick, role = Role.Button).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label.uppercase(), color = if (accent) c.onAccent else c.text, style = cutStyle(11, 800, 1f, 0.06f)) }
}

@Composable
private fun WelcomeBack(chain: Int, onOk: () -> Unit) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth(), border = c.freeze, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Welcome back.", color = c.text, style = cutStyle(18, 800, 1.1f))
            Sub("Chain paused at $chain. Nothing reset. Today counts, and a Min day (weigh in + protein) picks the chain right back up.")
            BigButton("Got it", onClick = onOk, modifier = Modifier.fillMaxWidth(), secondary = true, height = 44.dp, fontSize = 13)
        }
    }
}

@Composable
fun XpStrip(st: AppState) {
    val c = CutTheme.colors
    val level = st.level
    val next = ProgramEngine.nextLevelXp(level)
    val xp = tickedInt(st.xp)
    val progress by animateFloatAsState(ProgramEngine.levelProgress(st.xp), tween(450), label = "xpbar")
    Card(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp, 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Text("V$level", color = c.onAccent, style = cutStyle(18, 900, 1f, -0.02f))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("$xp XP", color = c.text, style = cutStyle(12, 700, 1f, 0.08f, tabular = true))
                    Text(if (next != null) "${next - st.xp} to V${level + 1}" else "maxed", color = c.text2, style = cutStyle(11, 600, 1f, tabular = true))
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.hairline)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(4.dp)).background(c.accent))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(CutIcons.Shield, null, Modifier.size(12.dp), tint = if (st.chain.securedToday) c.good else c.text2)
                    Text(
                        when {
                            st.chain.securedToday -> "SECURED TODAY"
                            st.chain.paused -> "CHAIN PAUSED"
                            else -> "CHAIN OPEN"
                        },
                        color = if (st.chain.securedToday) c.good else c.text2, style = cutStyle(10, 800, 1f, 0.1f),
                    )
                }
                val f = st.chain.freezesLeft
                Text("$f FREEZE" + (if (f == 1) "" else "S") + " LEFT", color = c.text2, style = cutStyle(10, 600, 1f, 0.1f, tabular = true))
            }
        }
    }
}

/** Past the last day: the route is done, the numbers stay, and the next one starts from onboarding. */
@Composable
private fun ToppedOut(st: AppState, onNext: () -> Unit) {
    val c = CutTheme.colors
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    Card(Modifier.fillMaxWidth(), border = c.good, borderWidth = 2.dp, radius = 14.dp, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Topped out.", color = c.text, style = cutStyle(22, 800, 1.1f, -0.02f, display = true))
            Sub(
                "${st.settings.totalDays} days, ${st.sends} send" + (if (st.sends == 1) "" else "s") + ", chain ${st.chain.count}" +
                    (if (avg != null) ", ${ProgramEngine.fmt1(avg)} lb on the last average." else ".") +
                    " Everything you logged stays. Start the next route whenever you are ready.",
            )
            BigButton("Start the next route", onClick = onNext, modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 13)
        }
    }
}

@Composable
private fun LateCard(onRest: () -> Unit, onClimb: () -> Unit) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth(), border = c.accent, borderWidth = 2.dp, radius = 14.dp, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("It's 7pm and no day is picked.", color = c.text, style = cutStyle(20, 800, 1.1f, -0.02f, display = true))
            Sub("Guessing Rest day. Weigh in and hit protein and the chain is safe. Nothing else required tonight.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("Yep, Rest day", onClick = onRest, modifier = Modifier.weight(1f), height = 48.dp, fontSize = 13)
                BigButton("I climbed", onClick = onClimb, modifier = Modifier.weight(1f), secondary = true, height = 48.dp, fontSize = 13)
            }
        }
    }
}

@Composable
private fun PickCards(st: AppState, onPick: (DayMode) -> Unit) {
    val climb = ProgramEngine.script(DayMode.CLIMB, st.settings)
    val off = ProgramEngine.script(DayMode.OFF, st.settings)
    val rest = ProgramEngine.script(DayMode.REST, st.settings)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PickCard(Modifier.weight(1f), CutIcons.Climb, "Climb day", "Climb, then 15 min push at the gym. ${climb.size} items, ${ProgramEngine.totalXp(climb)} xp.") { onPick(DayMode.CLIMB) }
            PickCard(Modifier.weight(1f), CutIcons.Off, "Off day", "15 min core + push at home. ${off.size} items, ${ProgramEngine.totalXp(off)} xp.") { onPick(DayMode.OFF) }
        }
        PickCard(
            Modifier.fillMaxWidth(), CutIcons.Rest, "Rest day",
            "Weigh in and eat the script. No block. ${rest.size} items, ${ProgramEngine.totalXp(rest)} xp. Workouts ${st.workoutsThisWeek} of ${st.settings.workoutsPerWeek} this week.",
            wide = true,
        ) { onPick(DayMode.REST) }
    }
}

@Composable
private fun PickCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, wide: Boolean = false, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .heightIn(min = if (wide) 0.dp else 150.dp)
            .clip(shape)
            .background(c.surface, shape)
            .border(2.dp, c.mutedBorder, shape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(14.dp, 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.ground), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = c.accent)
        }
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = c.text, style = cutStyle(22, 800, 1f, -0.02f, display = true))
            Text(sub, color = c.text2, style = cutStyle(12, 500, 1.35f))
        }
    }
}

@Composable
private fun ModeChipRow(mode: DayMode?, dv: AppState.DayView, proteinTarget: Int, onChange: () -> Unit, onMakeRest: (() -> Unit)? = null) {
    val c = CutTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(c.surface)
                .border(1.dp, c.hairline, RoundedCornerShape(999.dp))
                .clickable(onClick = onChange, role = Role.Button)
                .padding(start = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(when (mode) { DayMode.CLIMB -> CutIcons.Climb; DayMode.REST -> CutIcons.Rest; else -> CutIcons.Off }, null, Modifier.size(13.dp), tint = c.onAccent)
            }
            Text(when (mode) { DayMode.CLIMB -> "Climb day"; DayMode.REST -> "Rest day"; else -> "Off day" }, color = c.text2, style = cutStyle(12, 600, 1f, 0.04f))
            Text("Change", color = c.accent, style = cutStyle(12, 800, 1f, 0.06f))
        }
        if (onMakeRest != null) Box(
            Modifier.height(36.dp).clip(RoundedCornerShape(999.dp)).border(1.dp, c.mutedBorder, RoundedCornerShape(999.dp))
                .clickable(onClick = onMakeRest, role = Role.Button).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) { Text("MAKE IT REST", color = c.text, style = cutStyle(11, 800, 1f, 0.06f)) }
      }
        val hit = dv.totals.protein >= proteinTarget
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${tickedInt(dv.totals.protein)}", color = if (hit) c.good else c.text, style = cutStyle(13, 800, 1f, tabular = true))
            Text(" / $proteinTarget g", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
            Text("   ${tickedInt(dv.totals.calories)} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
        }
    }
}

@Composable
private fun WeighInline(prefill: Double, onLog: (Double) -> Unit) {
    val c = CutTheme.colors
    var text by rememberSaveable { mutableStateOf(ProgramEngine.fmt1(prefill)) }
    val focus = remember { FocusRequester() }
    val haptics = LocalHaptics.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun adjust(d: Double) { text = ProgramEngine.fmt1((text.toDoubleOrNull() ?: prefill) + d); haptics.tick() }
    fun go() { text.toDoubleOrNull()?.takeIf { it > 0 }?.let(onLog) }
    Row(Modifier.fillMaxWidth().padding(start = 40.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("-.2") { adjust(-0.2) }
        Field(text, { text = it }, Modifier.weight(1f), onDone = ::go, focusRequester = focus)
        Chip("+.2") { adjust(0.2) }
        Box(
            Modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(c.accent).clickable(onClick = ::go, role = Role.Button).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("LOG", color = c.onAccent, style = cutStyle(12, 800, 1f, 0.06f)) }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    val c = CutTheme.colors
    Box(
        Modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(c.surface).border(1.dp, c.mutedBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick, role = Role.Button).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = c.text, style = cutStyle(12, 800, 1f, 0.06f, tabular = true)) }
}

/** Library meals for a slot (default pinned, then most picked), a one-off entry, and a way to add to the library. */
@Composable
private fun MealPicker(st: AppState, slot: Meal, current: String?, onPick: (String?) -> Unit, onSaveLib: (LibMeal) -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val def = ProgramEngine.defaultChoice(slot, s)
    val counts = st.logs.mapNotNull { it.picks[slot.id] }.groupingBy { it }.eachCount()
    val sorted = s.library.sortedByDescending { counts["lib:" + it.id] ?: 0 }.let { l ->
        val d = l.firstOrNull { "lib:" + it.id == def.ref }
        if (d != null) listOf(d) + l.filter { it !== d } else l
    }
    val selected = current ?: def.ref
    var recipeOpen by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf<Boolean?>(null) }  // null closed, false = one-off, true = new library meal
    Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (m in sorted) {
            val ref = "lib:" + m.id
            val shape = RoundedCornerShape(9.dp)
            Column(Modifier.fillMaxWidth().clip(shape).background(c.ground).border(1.dp, if (selected == ref) c.accent else c.hairline, shape)) {
                Row(
                    Modifier.fillMaxWidth().clickable(onClick = { onPick(ref) }, role = Role.Button).padding(12.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(m.name, color = c.text, style = cutStyle(14, 600, 1.1f))
                        if (ref == def.ref) Text("DEFAULT", color = c.text2, style = cutStyle(9, 800, 1f, 0.1f))
                    }
                    if (m.recipe.isNotBlank()) Text(
                        "RECIPE", color = c.accent, style = cutStyle(10, 800, 1f, 0.08f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { recipeOpen = if (recipeOpen == m.id) null else m.id }.padding(6.dp),
                    )
                    Text("${m.protein}g · ${m.calories} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
                }
                if (recipeOpen == m.id) Text(m.recipe, color = c.text2, style = cutStyle(13, 500, 1.4f), modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp))
            }
        }
        if (current != null && current.startsWith("custom:")) {
            val ch = ProgramEngine.resolveChoice(slot, current, s)
            val shape = RoundedCornerShape(9.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(c.ground).border(1.dp, c.accent, shape).padding(12.dp, 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ch.name, color = c.text, style = cutStyle(14, 600, 1f))
                Text("${ch.protein}g · ${ch.calories} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(if (form == false) "CLOSE" else "SOMETHING ELSE") { form = if (form == false) null else false }
            Chip(if (form == true) "CLOSE" else "+ NEW MEAL") { form = if (form == true) null else true }
        }
        if (form != null) CustomMealForm(
            toLibrary = form == true,
            onLog = { name, p, cal, save ->
                if (save) {
                    val m = LibMeal("lib_" + System.currentTimeMillis(), name.ifBlank { "New meal" }, p, cal)
                    onSaveLib(m); onPick("lib:" + m.id)
                } else onPick(ProgramEngine.customRef(p, cal, name))
                form = null
            },
        )
    }
}

/** Name, protein, calories, and whether to keep it in the library. */
@Composable
private fun CustomMealForm(toLibrary: Boolean, onLog: (String, Int, Int, Boolean) -> Unit) {
    val c = CutTheme.colors
    var name by rememberSaveable { mutableStateOf("") }
    var pro by rememberSaveable { mutableStateOf("") }
    var cal by rememberSaveable { mutableStateOf("") }
    var save by rememberSaveable(toLibrary) { mutableStateOf(toLibrary) }
    Card(Modifier.fillMaxWidth(), border = c.hairline, padding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(name, { name = it }, Modifier.fillMaxWidth(), height = 40.dp, fontSize = 14, weight = 700, keyboard = KeyboardType.Text, imeAction = ImeAction.Next, borderWidth = 1.dp, radius = 8.dp, padding = 10.dp, placeholder = if (toLibrary) "Meal name" else "What was it (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(pro, { pro = it }, Modifier.weight(1f), height = 40.dp, fontSize = 16, center = true, imeAction = ImeAction.Next, borderWidth = 1.dp, radius = 8.dp, padding = 8.dp, placeholder = "protein g")
                Field(cal, { cal = it }, Modifier.weight(1f), height = 40.dp, fontSize = 16, center = true, borderWidth = 1.dp, radius = 8.dp, padding = 8.dp, placeholder = "calories")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(save) { save = !save }
                    Sub("Save to library", size = 12)
                }
                BigButton("Log it", onClick = { onLog(name, pro.toIntOrNull() ?: 0, cal.toIntOrNull() ?: 0, save) }, modifier = Modifier.width(110.dp), height = 40.dp, fontSize = 12, enabled = pro.toIntOrNull() != null || cal.toIntOrNull() != null)
            }
        }
    }
}

private fun Modifier.dashedBorder(color: Color) = drawBehind {
    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    drawRoundRect(color, style = stroke, cornerRadius = CornerRadius(10.dp.toPx()))
}
