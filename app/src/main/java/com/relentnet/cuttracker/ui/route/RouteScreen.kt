package com.relentnet.cuttracker.ui.route

import com.relentnet.cuttracker.ui.today.resultName
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.BadgeIds
import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.holdFill
import com.relentnet.cuttracker.ui.components.holdNumberColor
import com.relentnet.cuttracker.ui.components.holdShape
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.TileLabel
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val popFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)

/** Checkpoint state for the anchor icons and the Checkpoints timeline. */
enum class CpState { HIT, PASSED, CURRENT, UPCOMING }

fun checkpointState(st: AppState, cpDate: LocalDate, target: Double?): Pair<CpState, Double?> {
    val avg = ProgramEngine.sevenDayAverage(st.weighIns.filter { !it.date.isAfter(cpDate) }, cpDate)
    return when {
        st.today.isAfter(cpDate) -> (if (target != null && avg != null && avg <= target) CpState.HIT else CpState.PASSED) to avg
        else -> CpState.UPCOMING to avg
    }
}

/** The first checkpoint today has not passed is the current one. */
fun currentCheckpointDate(st: AppState): LocalDate? =
    st.settings.checkpoints.filter { !it.free }.map { it.localDate }.sorted().firstOrNull { !st.today.isAfter(it) }

@Composable
fun RouteScreen(vm: AppViewModel, onCheckpoints: () -> Unit, onEditDay: (LocalDate) -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    val s = st.settings
    var pop by remember { mutableStateOf<LocalDate?>(null) }
    val current = currentCheckpointDate(st)

    Box(Modifier.fillMaxSize()) {
        Screen {
            androidx.compose.foundation.layout.FlowRow(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Eyebrow("The Route · ${s.lengthWeeks} weeks")
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Text("Day ${st.dayN.coerceAtMost(s.totalDays)}", color = c.text, style = cutStyle(42, 800, 1f, -0.03f, display = true, tabular = true))
                        Text(" / ${s.totalDays}", color = c.dim, style = cutStyle(42, 800, 1f, -0.03f, display = true, tabular = true))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Stat("${st.sends}", "Sends", c.good)
                    Stat("${st.chain.count}", "Chain", c.text, icon = true)
                }
            }
            HoldCalendar(st, current, onDay = { pop = it; haptics.tick() }, onCheckpoints = onCheckpoints)
            NextCheckpoint(st, current, onCheckpoints)
            Legend()
            BadgeShelf(st.badges)
        }
        pop?.let { date ->
            HoldPopover(
                st, date, onClose = { pop = null }, onEdit = { pop = null; onEditDay(date) }, onCheckpoints = { pop = null; onCheckpoints() },
                onToggleSkip = {
                    vm.updateSettings { s -> val cps = s.checkpoints; val existing = cps.firstOrNull { it.free && it.covers(date) }; s.copy(checkpoints = if (existing != null) cps - existing else cps + Checkpoint(date.toString(), free = true, note = "Planned skip.")) }
                    pop = null
                },
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, icon: Boolean = false) {
    val c = CutTheme.colors
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (icon) Icon(CutIcons.Link, null, Modifier.size(14.dp), tint = c.good)
            Text(value, color = color, style = cutStyle(24, 800, 1f, tabular = true))
        }
        Text(label.uppercase(), color = c.text2, style = cutStyle(10, 700, 1f, 0.08f), softWrap = false)
    }
}

private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US)

/** Twelve Monday-to-Sunday rows. Climbed days are holds (shape = kind of day, fill = result); the rest are numbers. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HoldCalendar(st: AppState, current: LocalDate?, onDay: (LocalDate) -> Unit, onCheckpoints: () -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val modes = st.logs.associate { it.date to it.mode }
    val firstMon = ProgramEngine.weekStart(s.startDate)
    val rows = (java.time.temporal.ChronoUnit.DAYS.between(firstMon, s.endDate).toInt() + 6) / 7
    val cps = s.checkpoints.associateBy { it.localDate }
    var lastMonth = -1
    val thisWeekRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) { delay(380); thisWeekRequester.bringIntoView() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 34.dp, end = 22.dp)) {
            for (d in "MTWTFSS") Text("$d", Modifier.weight(1f), color = c.dim, style = cutStyle(9, 800, 1f, 0.06f), textAlign = TextAlign.Center)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (w in 0 until rows) {
                val mon = firstMon.plusDays(7L * w)
                val mid = mon.plusDays(3)
                val label = if (mid.monthValue != lastMonth) mid.format(monthFmt).uppercase() else ""
                lastMonth = mid.monthValue
                val week = ProgramEngine.rowWeek(mon, s.startDate, s.lengthWeeks)
                val reached = !mon.isAfter(st.today)
                val thisWeek = !mon.isAfter(st.today) && !mon.plusDays(6).isBefore(st.today)
                // Rows cascade in on first open, 22ms apart; the current week is where the page lands.
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(40L + w * 22L); shown = true }
                val rowAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(240), label = "row")
                val rowLift by animateFloatAsState(if (shown) 0f else 10f, tween(260), label = "rowLift")
                Row(
                    Modifier
                        .graphicsLayer { alpha = rowAlpha; translationY = rowLift }
                        .then(if (thisWeek) Modifier.bringIntoViewRequester(thisWeekRequester) else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, Modifier.width(34.dp), color = if (reached) c.text else c.dim, style = cutStyle(10, 800, 1f, 0.06f))
                    for (d in 0 until 7) {
                        val day = mon.plusDays(d.toLong())
                        val r = st.results[day]
                        val cp = cps[day]
                        val tap: (() -> Unit)? = when {
                            r != null && r != DayResult.BEFORE -> ({ onDay(day) })
                            cp != null -> onCheckpoints
                            else -> null
                        }
                        val said = day.format(java.time.format.DateTimeFormatter.ofPattern("EEEE MMMM d", java.util.Locale.US)) +
                            (if (r != null && r != DayResult.BEFORE) ", " + resultName(r) else "") +
                            (if (cp != null && !cp.free) ", checkpoint" + (cp.target?.let { " ${ProgramEngine.fmt1(it)} lb" } ?: "") else if (cp?.free == true) ", planned day off" else "")
                        Box(
                            Modifier.weight(1f).height(34.dp).then(if (tap != null) Modifier.clickable(onClick = tap, role = Role.Button) else Modifier)
                                .semantics(mergeDescendants = true) { contentDescription = said },
                            contentAlignment = Alignment.Center,
                        ) {
                            val flag = cp != null && !cp.free
                            val hit = flag && checkpointState(st, cp!!.localDate, cp.target).first == CpState.HIT
                            val flagColor = when { hit -> c.good; cp?.localDate == current -> c.accent; else -> c.mutedBorder }
                            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                if (flag && (r == null || r == DayResult.FUTURE)) Box(Modifier.size(30.dp).border(2.dp, flagColor, CircleShape))
                                when (r) {
                                    null -> Text("${day.dayOfMonth}", color = c.dim.copy(alpha = 0.5f), style = cutStyle(11, 600, 1f, tabular = true))
                                    DayResult.FUTURE -> if (cp?.free == true) Hold(HoldShape.PINCH, Color.Transparent, 30.dp, stroke = c.freeze, strokeWidth = 2.dp) {
                                        Text("${day.dayOfMonth}", color = c.freeze, style = cutStyle(11, 900, 1f, tabular = true))
                                    } else Text("${day.dayOfMonth}", color = c.dim, style = cutStyle(11, 700, 1f, tabular = true))
                                    DayResult.BEFORE -> Text("${day.dayOfMonth}", color = c.dim, style = cutStyle(11, 700, 1f, tabular = true))
                                    else -> Hold(
                                        holdShape(modes[day]), holdFill(r, c), if (day == st.today) 32.dp else 30.dp,
                                        stroke = if (day == st.today) c.accent else null, strokeWidth = if (day == st.today) 2.4.dp else 0.dp,
                                    ) { Text("${day.dayOfMonth}", color = holdNumberColor(r, c), style = cutStyle(11, 900, 1f, tabular = true)) }
                                }
                                if (flag) Icon(CutIcons.Flag, null, Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(12.dp), tint = flagColor)
                            }
                        }
                    }
                    Text(week?.toString() ?: "", Modifier.width(22.dp), color = if (reached) c.text2 else c.dim, style = cutStyle(9, 700, 1f, tabular = true), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun NextCheckpoint(st: AppState, current: LocalDate?, onCheckpoints: () -> Unit) {
    val c = CutTheme.colors
    val cp = st.settings.checkpoints.firstOrNull { it.localDate == current } ?: return
    val days = java.time.temporal.ChronoUnit.DAYS.between(st.today, cp.localDate).toInt()
    val whenText = when (days) { 0 -> "today"; 1 -> "tomorrow"; else -> "in $days days" }
    Card(Modifier.fillMaxWidth().clickable(onClick = onCheckpoints, role = Role.Button), border = c.accent, padding = PaddingValues(12.dp, 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(CutIcons.Flag, null, Modifier.size(16.dp), tint = c.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TileLabel("Next checkpoint")
                Text(cp.localDate.format(cpFmt) + (cp.target?.let { " · ${ProgramEngine.fmt1(it)} lb" } ?: "") + " · $whenText", color = c.text, style = cutStyle(12, 800, 1f, tabular = true))
                if (cp.note.isNotBlank()) Sub(cp.note, size = 11)
            }
        }
    }
}

private val cpFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

@Composable
private fun Legend() {
    val c = CutTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Key("Off") { Hold(HoldShape.JUG, c.mutedBorder, 13.dp) }
            Key("Climb") { Hold(HoldShape.CRIMP, c.mutedBorder, 13.dp) }
            Key("Rest") { Hold(HoldShape.SLOPER, c.mutedBorder, 13.dp) }
            Key("No pick") { Hold(HoldShape.PINCH, c.mutedBorder, 13.dp) }
            Key("Checkpoint") { Icon(CutIcons.Flag, null, Modifier.size(11.dp), tint = c.mutedBorder) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Key("Send") { Swatch(c.good) }
            Key("Min") { Swatch(c.goodDim) }
            Key("Freeze or skip") { Swatch(c.freeze) }
            Key("Missed") { Swatch(c.miss) }
            Key("Today") { Swatch(Color.Transparent, ring = c.accent) }
        }
    }
}

@Composable
private fun Key(label: String, icon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        icon()
        Text(label.uppercase(), color = CutTheme.colors.text2, style = cutStyle(10, 700, 1f, 0.06f))
    }
}

@Composable
private fun Swatch(fill: Color, ring: Color? = null) {
    val shape = RoundedCornerShape(3.dp)
    Box(Modifier.size(11.dp).clip(shape).background(fill).then(if (ring != null) Modifier.border(2.dp, ring, shape) else Modifier))
}

private fun badgeColor(id: String, c: com.relentnet.cuttracker.ui.theme.CutColors): Color = if (id.startsWith("chain_")) c.good else c.accent

private fun badgeIcon(id: String): ImageVector = if (id.startsWith("chain_")) CutIcons.Link else CutIcons.Star

@Composable
fun BadgeShelf(earned: Set<String>) {
    val c = CutTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in BadgeIds.all.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (b in row) {
                    val on = b.id in earned
                    val shape = RoundedCornerShape(10.dp)
                    Column(
                        Modifier.weight(1f).clip(shape).then(if (on) Modifier.background(c.surface, shape) else Modifier.dashedBorder(c.mutedBorder)).padding(6.dp, 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val color = badgeColor(b.id, c)
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (on) color else c.hairline), contentAlignment = Alignment.Center) {
                            Icon(if (on) badgeIcon(b.id) else CutIcons.Plus, null, Modifier.size(18.dp), tint = if (on) (if (color == c.accent) c.onAccent else c.ground) else c.dim)
                        }
                        Text(b.name.uppercase(), color = if (on) c.text else c.text2, style = cutStyle(10, 700, 1.2f, 0.06f), textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

fun Modifier.dashedBorder(color: Color, radius: Float = 10f) = drawBehind {
    val stroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
    drawRoundRect(color, style = stroke, cornerRadius = CornerRadius(radius.dp.toPx()))
}

/** Bottom popover naming the day, its result with XP, and one line of detail. */
@Composable
private fun HoldPopover(st: AppState, date: LocalDate, onClose: () -> Unit, onEdit: () -> Unit, onCheckpoints: () -> Unit, onToggleSkip: () -> Unit) {
    val c = CutTheme.colors
    androidx.activity.compose.BackHandler { onClose() }
    val r = st.results[date] ?: DayResult.FUTURE
    val cp = st.settings.checkpoints.firstOrNull { !it.free && it.localDate == date }
    // A checkpoint ahead is the whole story of that day; a checkpoint behind is a second line under the day's result.
    val cpAhead = cp != null && r == DayResult.FUTURE
    val cpNote = cp?.note?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    val cpColor = cp?.let { if (it.localDate == currentCheckpointDate(st)) c.accent else c.text2 } ?: c.text2
    val cpLine = cp?.takeIf { !cpAhead }?.let {
        val (state, avg) = checkpointState(st, it.localDate, it.target)
        val target = it.target?.let { t -> "${ProgramEngine.fmt1(t)} lb target" } ?: "no target"
        val how = when (state) {
            CpState.HIT -> "hit at ${ProgramEngine.fmt1(avg!!)}"
            CpState.PASSED -> if (avg != null) "was ${ProgramEngine.fmt1(avg)}" else "passed"
            else -> "today"
        }
        "Checkpoint, $target, $how.$cpNote" to (if (state == CpState.HIT) c.good else cpColor)
    }
    val log = st.logs.firstOrNull { it.date == date }
    val xp = st.xpEvents.filter { it.date == date }.sumOf { it.amount }
    val items = ProgramEngine.script(log?.mode, st.settings, log?.picks ?: emptyMap())
    val done = log?.doneItemIds?.count { id -> items.any { it.id == id } } ?: 0
    val weight = st.weighIns.firstOrNull { it.date == date }?.lb
    val skipped = st.settings.checkpoints.any { it.free && it.covers(date) }
    val (title, color) = if (cpAhead) ("Checkpoint" + (cp!!.target?.let { " · ${ProgramEngine.fmt1(it)} lb" } ?: "")) to cpColor else when (r) {
        DayResult.SEND -> "Send · +$xp xp" to c.good
        DayResult.MIN -> "Min day · +$xp xp" to c.good
        DayResult.FREEZE -> "Freeze · 0 xp" to c.text2
        DayResult.SKIP -> "Planned skip" to c.freeze
        DayResult.MISSED -> "Missed · 0 xp" to c.text2
        DayResult.TODAY -> "Today · so far" to c.accent
        DayResult.FUTURE -> (if (skipped) "Planned skip" else "Upcoming") to (if (skipped) c.freeze else c.text2)
        else -> "Upcoming" to c.text2
    }
    val detail = if (cpAhead) when (val days = java.time.temporal.ChronoUnit.DAYS.between(st.today, date).toInt()) { 0 -> "Today."; 1 -> "Tomorrow."; else -> "In $days days." } + cpNote else when (r) {
        DayResult.FREEZE -> "Missed. A freeze covered it, chain kept."
        DayResult.SKIP -> "A day off you planned. Chain kept, no freeze spent."
        DayResult.MISSED -> "Nothing logged. Grey hold, that is all."
        DayResult.TODAY -> "In progress. Finish the script for a Send."
        DayResult.FUTURE -> if (skipped) "Planned skip. Chain kept, no freeze spent." else "Not here yet. Plan a skip if you know you will be off."
        DayResult.BEFORE -> "Before the start."
        else -> buildList {
            when (log?.mode) { DayMode.CLIMB -> add("Climb day."); DayMode.OFF -> add("Off day."); DayMode.REST -> add("Rest day."); null -> add("Free day.") }
            if (items.isNotEmpty()) add("$done of ${items.size} items.")
            if (weight != null) add("${ProgramEngine.fmt1(weight)} lb.")
            st.liftSets.filter { it.date == date }.groupBy { it.exerciseId }.forEach { (id, sets) ->
                val e = st.settings.exercises.firstOrNull { it.id == id } ?: return@forEach
                val w = sets.first().weightLb
                add("${e.name} ${sets.size}×${sets.first().reps}" + (if (w > 0) " at ${w.toInt()}." else "."))
            }
        }.joinToString(" ")
    }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)) {
        val scrim by animateFloatAsState(if (shown) 0.55f else 0f, tween(200), label = "scrim")
        Box(Modifier.matchParentSize().background(c.ground.copy(alpha = scrim)))
        AnimatedVisibility(
            shown, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f, visibilityThreshold = IntOffset.VisibilityThreshold)) { it } + fadeIn(tween(150)),
        ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(c.surface)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp, 10.dp, 20.dp, 16.dp).windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(c.mutedBorder).align(Alignment.CenterHorizontally))
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(date.format(popFmt), color = c.text, style = cutStyle(15, 800, 1f))
                    Text(title, color = color, style = cutStyle(12, 800, 1f, tabular = true))
                }
                Sub(detail, size = 12)
                if (cpLine != null) Sub(cpLine.first, size = 12, color = cpLine.second)
                val editable = !date.isAfter(st.today) && !date.isBefore(st.settings.startDate)
                val plannable = cp == null && date.isAfter(st.today) && date.isBefore(st.settings.endDate)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editable) BigButton(if (date == st.today) "Open today" else "Edit day", onClick = onEdit, modifier = Modifier.weight(1f), height = 40.dp, fontSize = 12)
                    if (cp != null) BigButton("Checkpoints", onClick = onCheckpoints, modifier = Modifier.weight(1f), height = 40.dp, fontSize = 12, secondary = editable)
                    if (plannable || (skipped && !editable)) BigButton(if (skipped) "Unskip" else "Plan a skip", onClick = onToggleSkip, modifier = Modifier.weight(1f), height = 40.dp, fontSize = 12)
                    BigButton("Close", onClick = onClose, modifier = Modifier.weight(1f), secondary = true, height = 40.dp, fontSize = 12)
                }
            }
        }
        }
    }
}
