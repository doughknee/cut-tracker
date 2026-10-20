package com.relentnet.cuttracker.ui.today

import com.relentnet.cuttracker.ui.components.ConfirmDialog
import com.relentnet.cuttracker.ui.components.int
import com.relentnet.cuttracker.ui.components.num
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.key
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.holdShape
import com.relentnet.cuttracker.ui.components.pressable
import com.relentnet.cuttracker.ui.route.dashedBorder
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.relentnet.cuttracker.ui.beta.dayTitle
import com.relentnet.cuttracker.ui.beta.dayWhere
import com.relentnet.cuttracker.ui.weight.paceWords
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d", Locale.US)

@Composable
fun TodayScreen(vm: AppViewModel, onSettings: () -> Unit, onWeight: () -> Unit, onRoute: () -> Unit) {
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
    LaunchedEffect(st.settings.tourSeen) { if (!st.settings.tourSeen) { infoOpen = true; vm.updateSettings { s -> s.copy(tourSeen = true) } } }
    // After an update, a few lines on what changed, once. Fresh installs and versions without a note are marked quietly.
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "" }
    var newsOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(st.settings.seenVersion, version) {
        if (version.isNotBlank() && st.settings.seenVersion != version) {
            if (st.settings.tourSeen && st.logs.isNotEmpty() && whatsNew[version] != null) newsOpen = true
            else vm.updateSettings { s -> s.copy(seenVersion = version) }
        }
    }
    val dv = st.day(date)
    val items = dv.items
    val done = dv.log.doneItemIds
    val mode = dv.log.mode
    val all = dv.isSend
    // A row just ticked stays in place, ticked, for a beat before it leaves for Done, so the tick is seen.
    // Derived in the same frame as the tick, so the row never blinks out before it shows as ticked.
    var seen by remember(date) { mutableStateOf(done) }
    val fresh = done - seen
    LaunchedEffect(done) { if (fresh.isNotEmpty()) kotlinx.coroutines.delay(450); seen = done }
    val remaining = items.filter { it.id !in done || it.id in fresh }
    val finished = items.filter { it.id in done && it.id !in fresh }
    val next = remaining.firstOrNull { it.id !in done }
    var changing by remember(date) { mutableStateOf(false) }
    val late = !past && mode == null && !changing && (!now.isBefore(st.settings.pickTime) || now.hour < st.settings.resetHour)
    val welcomeBack = !past && st.missedRun >= 2 && st.settings.welcomeBackSeen != st.today
    val prevWeigh = st.weighInBefore(date)

    fun closeInline() { weighOpen = false; pickOpen = null; liftOpen = false }
    fun view(d: LocalDate) {
        val t = if (d.isAfter(st.today)) st.today else if (d.isBefore(st.settings.startDate)) st.settings.startDate else d
        vm.viewDate = t.takeIf { it != st.today }
        closeInline()
    }
    // The weigh-in reminder lands here with the row open.
    LaunchedEffect(vm.pendingWeigh) { if (vm.pendingWeigh) { vm.pendingWeigh = false; view(st.today); weighOpen = true } }
    // Back closes an open panel first, then takes an earlier day back to today, and only then leaves.
    androidx.activity.compose.BackHandler(enabled = past) { view(st.today) }
    androidx.activity.compose.BackHandler(enabled = weighOpen || pickOpen != null || liftOpen) { closeInline() }
    var confirmNext by remember { mutableStateOf(false) }
    val dayReading = st.weighIns.firstOrNull { w -> w.date == date }
    val total = st.settings.protein
    val (stateLine, stateColor) = when {
        st.ended && !past -> "Route complete" to c.good
        !past && dv.isSend -> "Sent" to c.good
        !past && dv.isMin -> "Chain safe · today" to c.good
        !past && mode == null -> "Today · pick a day" to c.accent
        !past && ProgramSpec.ITEM_WEIGH !in done && dv.totals.protein < total -> "Today · weigh in + ${total - dv.totals.protein}g to a Min day" to c.accent
        !past && dv.totals.protein < total -> "Today · ${total - dv.totals.protein}g to a Min day" to c.accent
        !past -> "Today · weigh in for a Min day" to c.accent
        else -> "${resultName(dv.result)} · earlier day" to when (dv.result) {
            DayResult.SEND, DayResult.MIN -> c.good
            DayResult.FREEZE, DayResult.SKIP -> c.freeze
            else -> c.text2
        }
    }

    // The keyboard shrinks the scroll viewport (padding outside the scroll), so a focused field scrolls above it.
    Column(Modifier.fillMaxSize().background(c.ground).imePadding().verticalScroll(scroll)) {
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
                    Icon(CutIcons.Link, null, Modifier.size(15.dp), tint = if (st.chain.paused) c.freeze else c.good)
                    Text("${tickedInt(st.chain.count)}", color = c.text, style = cutStyle(16, 800, 1f, tabular = true))
                    Text(if (st.chain.paused) "PAUSED" else "CHAIN", color = if (st.chain.paused) c.freeze else c.text2, style = cutStyle(10, 700, 1f, 0.08f), modifier = Modifier.padding(start = 4.dp))
                }
            }
            Text(date.format(dayFmt), color = c.text, style = cutStyle(48, 800, 1f, -0.03f, display = true), maxLines = 1)
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).pressable(haptic = false, onClick = onRoute).padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val dn = ProgramEngine.dayNumber(date, st.settings.startDate)
                Sub(if (dn < 1) "Starts ${st.settings.startDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM d", Locale.US))}" else "Day ${dn.coerceAtMost(st.settings.totalDays)} of ${st.settings.totalDays}", size = 12)
                Text("ROUTE ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
            }
        }

        if (welcomeBack) WelcomeBack(st.chain.count, onOk = { vm.dismissWelcomeBack() })

        AnimatedContent(
            targetState = when { st.ended && !past -> 0; late -> 1; mode == null -> 2; else -> 3 },
            transitionSpec = {
                (fadeIn(tween(220, 60)) + scaleIn(tween(260), initialScale = 0.96f)) togetherWith (fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.98f))
            },
            label = "modeSection",
        ) { stage -> when (stage) {
            0 -> ToppedOut(st, onNext = { confirmNext = true })
            1 -> LateCard(
                at = st.settings.pickTime,
                onRest = { vm.setMode(DayMode.REST) },
                onClimb = { vm.setMode(DayMode.CLIMB, preCheck = setOf(ProgramSpec.ITEM_CLIMB)) },
                onOff = { vm.setMode(DayMode.OFF) },
            )
            2 -> PickCards(st, onPick = { haptics.tap(); vm.setMode(it, date = editDate) })
            else -> ModeChipRow(
                mode, dv, st.settings.protein,
                onChange = { weighOpen = false; pickOpen = null; changing = true; vm.setMode(null, date = editDate) },
                // An Off day with the block unchecked is a Rest day that was logged under the old rules; one tap fixes it.
                onMakeRest = if (mode == DayMode.OFF && ProgramSpec.ITEM_CORE !in done) ({ haptics.tap(); vm.setMode(DayMode.REST, date = editDate) }) else null,
            )
        } }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow(if (past) "Script · late ticks count" else "Script")
                Eyebrow("${tickedInt(finished.size)} / ${items.size}")
            }
            if (items.isEmpty() && !past && ProgramSpec.ITEM_WEIGH !in done) {
                CheckRow(
                    label = "Weigh in", sub = prevWeigh?.let { "Last ${ProgramEngine.fmt1(it.lb)}. Tap to log." } ?: "Before food. Log it, ignore the number.",
                    time = null, xp = ProgramSpec.XP_WEIGH, done = false, next = true,
                    onClick = { weighOpen = !weighOpen },
                )
                Unfold(weighOpen) {
                    WeighInline(prefill = prevWeigh?.lb ?: st.settings.startWeight, onLog = { lb -> weighOpen = false; haptics.tap(); vm.logWeight(lb) })
                }
            }
            if (items.isEmpty() && !(st.ended && !past)) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).dashedBorder(c.mutedBorder).padding(18.dp, 14.dp),
                ) {
                    Sub(if (past) "Nothing was logged. Pick what this day was and fill it in." else "Pick a day and the script shows up. A Min day is weigh in + protein. That keeps the chain.")
                }
            }
            for (it in remaining) {
                val isNext = it === next
                key(it.id) { Column(Modifier.animateContentSize(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow))) {
                CheckRow(
                    label = it.label,
                    sub = if (it.id == ProgramSpec.ITEM_WEIGH && prevWeigh != null) "Last ${ProgramEngine.fmt1(prevWeigh.lb)}. Tap to log." else it.sub,
                    time = it.time, xp = it.xp, done = it.id in done, next = isNext,
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
                    // Rows that open something (a meal picker, the lifts) still tick in one tap on the box, like the widget.
                    onCheck = if (it.pick || (it.workout && it.id != ProgramSpec.ITEM_CLIMB && !past)) ({ haptics.tap(); closeInline(); vm.toggleItem(it.id, date = editDate) }) else null,
                )
                Unfold(it.id == ProgramSpec.ITEM_WEIGH && weighOpen) {
                    WeighInline(
                        prefill = prevWeigh?.lb ?: st.settings.startWeight,
                        onLog = { lb -> weighOpen = false; haptics.tap(); vm.logWeight(lb, editDate) },
                    )
                }
                Unfold(it.pick && pickOpen == it.id) {
                    val slot = st.settings.meals.firstOrNull { m -> m.id == it.id }
                    if (slot != null) MealPicker(
                        st, slot, date, current = dv.log.picks[it.id],
                        onPick = { ref -> pickOpen = null; haptics.tap(); vm.pickMeal(it.id, ref, editDate) },
                        onSaveLib = { m -> vm.saveLibraryMeal(m) },
                    )
                }
                Unfold(it.workout && it.id != ProgramSpec.ITEM_CLIMB && liftOpen && !past) {
                    // Saving the session checks the block. Swiping the row checks it without a session.
                    LiftInline(vm, mode = mode ?: DayMode.OFF, tickBlock = true, onSaved = { liftOpen = false })
                }
                } }
            }
            if (mode == DayMode.REST && !past) {
                Text(
                    if (liftOpen) "CLOSE LIFTS" else "LIFT ANYWAY",
                    color = c.text2, style = cutStyle(11, 800, 1f, 0.1f),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).pressable { liftOpen = !liftOpen }.padding(horizontal = 6.dp, vertical = 14.dp),
                )
                if (liftOpen) LiftInline(vm, mode = DayMode.OFF, tickBlock = true, onSaved = { liftOpen = false })
            }
            if (finished.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button, onClickLabel = if (doneOpen) "Hide done items" else "Show done items") { doneOpen = !doneOpen },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(CutIcons.Forward, null, Modifier.size(12.dp).rotate(if (doneOpen) 90f else 0f), tint = c.text2)
                    Text("DONE · ${finished.size}", color = c.text2, style = cutStyle(11, 700, 1f, 0.1f))
                    Box(Modifier.weight(1f).height(1.dp).background(c.hairline))
                }
                Unfold(doneOpen) { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (it in finished) {
                        val reading = if (it.id == ProgramSpec.ITEM_WEIGH) dayReading else null
                        val meal = if (it.pick) it.sub?.substringBefore(". ")?.takeIf { m -> m.isNotBlank() } else null
                        CheckRow(
                            label = if (reading != null) "${it.label} · ${ProgramEngine.fmt1(reading.lb)} lb" else if (meal != null) "${it.label} · $meal" else it.label,
                            sub = null, time = null, xp = it.xp, done = true, compact = true,
                            onClick = {
                                when {
                                    reading != null -> { weighOpen = !weighOpen; pickOpen = null; liftOpen = false }
                                    it.pick -> { pickOpen = if (pickOpen == it.id) null else it.id; weighOpen = false; liftOpen = false }
                                    else -> vm.toggleItem(it.id, date = editDate)
                                }
                            },
                            onCheck = if (it.pick) ({ haptics.tap(); closeInline(); vm.toggleItem(it.id, date = editDate) }) else null,
                        )
                        if (it.pick) Unfold(pickOpen == it.id) {
                            val slot = st.settings.meals.firstOrNull { m -> m.id == it.id }
                            if (slot != null) MealPicker(
                                st, slot, date, current = dv.log.picks[it.id],
                                onPick = { ref -> pickOpen = null; haptics.tap(); vm.pickMeal(it.id, ref, editDate) },
                                onSaveLib = { m -> vm.saveLibraryMeal(m) },
                            )
                        }
                        if (reading != null) Unfold(weighOpen) {
                            WeighInline(prefill = reading.lb, onLog = { lb -> weighOpen = false; haptics.tap(); vm.logWeight(lb, editDate) })
                        }
                    }
                } }
            }
        }

        WeightTile(st, onWeight)
        }
    }
    if (infoOpen) WallInfoSheet(onDismiss = { infoOpen = false })
    if (newsOpen && !infoOpen) WhatsNewSheet(version, whatsNew[version].orEmpty()) { newsOpen = false; vm.updateSettings { s -> s.copy(seenVersion = version) } }
    if (confirmNext) ConfirmDialog(
        "Start the next route?",
        "Your logs, badges and xp stay. You set a new start, a new goal and a new length, and the wall starts again at day 1.",
        "Start it", onConfirm = { vm.updateSettings { s -> s.copy(onboarded = false) } }, onDismiss = { confirmNext = false },
    )
}

/** 7-day average and the pace sentence Weight uses. Tapping opens Weight. */
@Composable
private fun WeightTile(st: AppState, onWeight: () -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    val shown = tickedFloat((avg ?: s.startWeight).toFloat())
    val gap = avg?.let { it - ProgramEngine.paceWeight(st.dayN.coerceIn(1, s.totalDays), s, ProgramEngine.paceAnchor(st.weighIns, s)) }
    val onPace = gap != null && gap <= ProgramSpec.PACE_TOLERANCE_LB
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
                if (gap != null) Text(paceWords(gap), color = if (onPace) c.good else c.accent, style = cutStyle(12, 700, 1f, tabular = true))
                else Text("No readings yet", color = c.text2, style = cutStyle(12, 600, 1f))
                Text("WEIGHT ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
            }
        }
    }
}

@Composable
private fun NavPill(label: String, onClick: () -> Unit, accent: Boolean = false) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier.minimumInteractiveComponentSize().height(32.dp).clip(shape).background(if (accent) c.accent else c.surface).border(1.dp, if (accent) c.accent else c.hairline, shape)
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


/** Inline panels grow out of their row and shrink back, instead of popping. */
@Composable
private fun Unfold(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(160)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
    ) { content() }
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

/** "7pm", "7:30pm": the pick time from Settings as the card says it. */
private fun clockWords(t: java.time.LocalTime): String = t.format(DateTimeFormatter.ofPattern(if (t.minute == 0) "ha" else "h:mma", Locale.US)).lowercase()

@Composable
private fun LateCard(at: java.time.LocalTime, onRest: () -> Unit, onClimb: () -> Unit, onOff: () -> Unit) {
    val c = CutTheme.colors
    Card(Modifier.fillMaxWidth(), border = c.accent, borderWidth = 2.dp, radius = 14.dp, padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("It's past ${clockWords(at)} and no day is picked.", color = c.text, style = cutStyle(20, 800, 1.1f, -0.02f, display = true))
            Sub("Rest day is the guess. Tap it, weigh in and hit protein, and the chain is safe. Nothing else required tonight.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("Yep, Rest day", onClick = onRest, modifier = Modifier.weight(1f), height = 48.dp, fontSize = 13)
                BigButton("I climbed", onClick = onClimb, modifier = Modifier.weight(1f), secondary = true, height = 48.dp, fontSize = 13)
            }
            Text(
                "IT WAS AN OFF DAY", color = c.text2, style = cutStyle(11, 800, 1f, 0.08f),
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onOff).padding(vertical = 12.dp, horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun PickCards(st: AppState, onPick: (DayMode) -> Unit) {
    val climb = ProgramEngine.script(DayMode.CLIMB, st.settings)
    val off = ProgramEngine.script(DayMode.OFF, st.settings)
    val rest = ProgramEngine.script(DayMode.REST, st.settings)
    // The sentence under each card is the day page's own (Beta), so the cards tell the truth about the plan as edited there.
    fun sub(mode: DayMode, items: List<ScriptItem>) = "${dayWhere(mode, st.settings)} ${com.relentnet.cuttracker.ui.components.count(items.size, "item")}, ${ProgramEngine.totalXp(items)} xp."
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PickCard(Modifier.weight(1f), HoldShape.CRIMP, dayTitle(DayMode.CLIMB), sub(DayMode.CLIMB, climb)) { onPick(DayMode.CLIMB) }
            PickCard(Modifier.weight(1f), HoldShape.JUG, dayTitle(DayMode.OFF), sub(DayMode.OFF, off)) { onPick(DayMode.OFF) }
        }
        PickCard(
            Modifier.fillMaxWidth(), HoldShape.SLOPER, dayTitle(DayMode.REST),
            sub(DayMode.REST, rest) + " Workouts ${st.workoutsThisWeek} of ${st.settings.workoutsPerWeek} this week.",
            wide = true,
        ) { onPick(DayMode.REST) }
    }
}

@Composable
private fun PickCard(modifier: Modifier, hold: HoldShape, title: String, sub: String, wide: Boolean = false, onClick: () -> Unit) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .heightIn(min = if (wide) 0.dp else 150.dp)
            .clip(shape)
            .background(c.surface, shape)
            .border(2.dp, c.mutedBorder, shape)
            .pressable(haptic = false, onClick = onClick)
            .padding(14.dp, 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(c.ground), contentAlignment = Alignment.Center) {
            Hold(hold, c.accent, 26.dp)
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
    androidx.compose.foundation.layout.FlowRow(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
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
                Hold(holdShape(mode), c.onAccent, 15.dp)
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
        Row(Modifier.heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${tickedInt(dv.totals.protein)}", color = if (hit) c.good else c.text, style = cutStyle(13, 800, 1f, tabular = true), softWrap = false)
            Text(" / $proteinTarget g", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true), softWrap = false)
            Text("   ${tickedInt(dv.totals.calories)} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true), softWrap = false)
        }
    }
}

@Composable
private fun WeighInline(prefill: Double, onLog: (Double) -> Unit) {
    val c = CutTheme.colors
    var text by rememberSaveable { mutableStateOf(ProgramEngine.fmt1(prefill)) }
    var bad by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val haptics = LocalHaptics.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    fun adjust(d: Double) { text = ProgramEngine.fmt1((text.num() ?: prefill) + d); haptics.tick() }
    fun go() {
        val lb = text.num()?.takeIf { it in 40.0..1000.0 }
        if (lb != null) onLog(lb) else { haptics.pattern(0, 40, 60, 40); bad = true }
    }
    Column(Modifier.fillMaxWidth().padding(start = 40.dp, top = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Chip("-.2") { adjust(-0.2) }
        Field(text, { text = it; bad = false }, Modifier.weight(1f), onDone = ::go, focusRequester = focus)
        Chip("+.2") { adjust(0.2) }
        Box(
            Modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(c.accent).clickable(onClick = ::go, role = Role.Button).padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Text("LOG", color = c.onAccent, style = cutStyle(12, 800, 1f, 0.06f)) }
    }
    if (bad) Text("Type a weight like 181.4.", color = c.accent, style = cutStyle(12, 600, 1.2f))
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

/** The slot's most logged meals this month, yesterday's pick, then the library (default pinned), a one-off entry, and a way to add to the library. */
@Composable
private fun MealPicker(st: AppState, slot: Meal, date: LocalDate, current: String?, onPick: (String?) -> Unit, onSaveLib: (LibMeal) -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val def = ProgramEngine.defaultChoice(slot, s)
    val counts = st.logs.mapNotNull { it.picks[slot.id] }.groupingBy { it }.eachCount()
    // What this slot logged in the last 30 days, most often first: the top five lead, so a repeat is one tap.
    val month = st.logs.filter { it.date != date && !it.date.isBefore(date.minusDays(30)) }.mapNotNull { it.picks[slot.id] }.groupingBy { it }.eachCount()
    // A one-off frozen from a library meal (an edit or delete) points back at that meal while it exists by name.
    fun live(ref: String): String = if (!ref.startsWith("custom:")) ref else {
        val n = ProgramEngine.resolveChoice(slot, ref, s).name
        s.library.firstOrNull { it.name.equals(n, ignoreCase = true) }?.let { "lib:" + it.id } ?: ref
    }
    val top = month.entries.groupBy({ live(it.key) }, { it.value }).mapValues { it.value.sum() }.entries.sortedByDescending { it.value }.map { it.key }
        .filter { it.startsWith("custom:") || ProgramEngine.libMeal(it.removePrefix("lib:"), s) != null }.take(5)
    val yesterday = st.logs.firstOrNull { it.date == date.minusDays(1) }?.picks?.get(slot.id)?.let(::live)
    // The slot's own kind first, then whatever gets picked most.
    val sorted = s.library.sortedWith(compareByDescending<LibMeal> { it.kind == slot.id }.thenByDescending { counts["lib:" + it.id] ?: 0 }).let { l ->
        val d = l.firstOrNull { "lib:" + it.id == def.ref }
        if (d != null) listOf(d) + l.filter { it !== d } else l
    }
    val selected = current ?: def.ref
    var recipeOpen by remember { mutableStateOf<String?>(null) }
    var form by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Boolean?>(null) }  // null closed, false = one-off, true = new library meal
    // A long library gets a search box; the top five and yesterday stay above it.
    var query by remember { mutableStateOf("") }
    Column(Modifier.padding(start = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (ref in top) {
            val n = month[ref] ?: 0
            PickRow(ProgramEngine.resolveChoice(slot, ref, s), (if (ref == def.ref) "DEFAULT · " else "") + "$n× IN 30 DAYS", selected == ref) { onPick(ref) }
        }
        if (yesterday != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("SAME AS YESTERDAY") { onPick(yesterday) }
            Text(ProgramEngine.resolveChoice(slot, yesterday, s).name, color = c.text2, style = cutStyle(12, 600, 1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (s.library.size > 8) Field(
            query, { query = it }, Modifier.fillMaxWidth(), height = 44.dp, fontSize = 14, weight = 600,
            keyboard = androidx.compose.ui.text.input.KeyboardType.Text, placeholder = "Search ${s.library.size} meals",
        )
        for (m in sorted.filter { com.relentnet.cuttracker.ui.components.matchesQuery(query, it.name, it.recipe) }) {
            val ref = "lib:" + m.id
            if (ref in top && query.isBlank()) continue
            val shape = RoundedCornerShape(9.dp)
            Column(Modifier.fillMaxWidth().clip(shape).background(c.ground).border(1.dp, if (selected == ref) c.accent else c.hairline, shape)) {
                Row(
                    Modifier.fillMaxWidth().pressable(haptic = false, onClick = { onPick(ref) }).padding(12.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(m.name, color = c.text, style = cutStyle(14, 600, 1.1f))
                        if (ref == def.ref) Text("DEFAULT", color = c.text2, style = cutStyle(9, 800, 1f, 0.1f))
                    }
                    if (m.recipe.isNotBlank()) Text(
                        "RECIPE", color = c.accent, style = cutStyle(10, 800, 1f, 0.08f),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { recipeOpen = if (recipeOpen == m.id) null else m.id }.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                    Text("${m.protein}g · ${m.calories} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
                }
                if (recipeOpen == m.id) Text(m.recipe, color = c.text2, style = cutStyle(13, 500, 1.4f), modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp))
            }
        }
        // The last few one-off entries for this slot, so a repeat is one tap instead of retyping.
        val recents = st.logs.sortedByDescending { it.date }.mapNotNull { it.picks[slot.id] }.filter { it.startsWith("custom:") && it != current && it !in top }.distinct().take(3)
        for (ref in recents) PickRow(ProgramEngine.resolveChoice(slot, ref, s), "RECENT", false) { onPick(ref) }
        if (current != null && current.startsWith("custom:") && current !in top) PickRow(ProgramEngine.resolveChoice(slot, current, s), null, true, null)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(if (form == false) "CLOSE" else "SOMETHING ELSE") { form = if (form == false) null else false }
            Chip(if (form == true) "CLOSE" else "+ NEW MEAL") { form = if (form == true) null else true }
        }
        if (form != null) CustomMealForm(
            toLibrary = form == true,
            onLog = { name, p, cal, save ->
                if (save) {
                    val m = LibMeal("lib_" + System.currentTimeMillis(), name.ifBlank { "New meal" }, p, cal, kind = slot.id)
                    onSaveLib(m); onPick("lib:" + m.id)
                } else onPick(ProgramEngine.customRef(p, cal, name))
                form = null
            },
        )
    }
}

/** One choice in the picker: name, an eyebrow saying why it is here, and the numbers. No [onClick] means it is the current pick. */
@Composable
private fun PickRow(ch: ProgramEngine.MealChoice, eyebrow: String?, selected: Boolean, onClick: (() -> Unit)?) {
    val c = CutTheme.colors
    val shape = RoundedCornerShape(9.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.ground).border(1.dp, if (selected) c.accent else c.hairline, shape)
            .then(if (onClick != null) Modifier.pressable(haptic = false, onClick = onClick) else Modifier).padding(12.dp, 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ch.name, color = c.text, style = cutStyle(14, 600, 1.1f))
            if (eyebrow != null) Text(eyebrow, color = c.text2, style = cutStyle(9, 800, 1f, 0.1f))
        }
        Text("${ch.protein}g · ${ch.calories} cal", color = c.text2, style = cutStyle(12, 600, 1f, tabular = true))
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
                    Switch(save, "Save to the library") { save = !save }
                    Sub("Save to library", size = 12)
                }
                BigButton("Log it", onClick = { onLog(name, pro.int() ?: 0, cal.int() ?: 0, save) }, modifier = Modifier.width(110.dp), height = 40.dp, fontSize = 12, enabled = pro.int() != null || cal.int() != null)
            }
        }
    }
}

