package com.relentnet.cuttracker.ui.route

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.BadgeIds
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.projectTarget
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.H1
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
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
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Day ${st.dayN} of ${s.totalDays} · ${ProgramEngine.pct(st.dayN, s.totalDays)}%")
                    Eyebrow("${st.sends} send" + if (st.sends == 1) "" else "s", color = c.good)
                }
                H1("The Route")
            }
            ProjectCard(st)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("${s.totalDays} holds, bottom to top")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendDot(c.good, "Send"); LegendDot(c.goodDim, "Min"); LegendDot(c.freeze, "Freeze")
                }
            }
            Card(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (w in s.lengthWeeks downTo 1) {
                        val anchors = s.checkpoints.filter { !it.free && ProgramEngine.checkpointWeek(it, s.startDate, s.lengthWeeks) == w }
                        val firstDay = s.startDate.plusDays(((w - 1) * 7).toLong())
                        val fut = firstDay.isAfter(st.today)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("WK $w", Modifier.width(34.dp), color = if (fut) c.dim else c.text, style = cutStyle(10, 700, 1f, 0.06f))
                            for (d in 0 until 7) {
                                val date = firstDay.plusDays(d.toLong())
                                val r = st.results[date] ?: DayResult.FUTURE
                                Hold(r, Modifier.weight(1f)) { pop = date; haptics.tick() }
                            }
                            Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
                                val cp = anchors.firstOrNull()
                                if (cp != null) {
                                    val (state, _) = checkpointState(st, cp.localDate, cp.target)
                                    val tint = when {
                                        state == CpState.HIT -> c.good
                                        cp.localDate == current -> c.accent
                                        else -> c.mutedBorder
                                    }
                                    Icon(
                                        CutIcons.Anchor, contentDescription = "Checkpoint week $w",
                                        Modifier.size(16.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onCheckpoints),
                                        tint = tint,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            BadgeShelf(st.badges)
        }
        pop?.let { date -> HoldPopover(st, date, onClose = { pop = null }, onEdit = { pop = null; onEditDay(date) }) }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val c = CutTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label.uppercase(), color = c.text2, style = cutStyle(10, 600, 1f, 0.06f))
    }
}

@Composable
private fun Hold(r: DayResult, modifier: Modifier, onClick: () -> Unit) {
    val c = CutTheme.colors
    val (bg, border) = when (r) {
        DayResult.SEND -> c.good to c.good
        DayResult.MIN -> c.goodDim to c.goodDim
        DayResult.FREEZE -> c.freeze to c.freeze
        DayResult.MISSED -> c.miss to c.miss
        DayResult.TODAY -> c.ground to c.accent
        else -> c.future to c.future
    }
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.height(22.dp).clip(shape).background(bg, shape).border(2.dp, border, shape).clickable(onClick = onClick, role = Role.Button))
}

@Composable
private fun ProjectCard(st: AppState) {
    val c = CutTheme.colors
    val spec = st.projectSpec
    Card(Modifier.fillMaxWidth(), border = c.hairline, padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow("This week's project", color = c.accent, size = 11)
                if (spec != null) Text("${st.projectProgress.coerceAtMost(projectTarget(spec, st.settings))} / ${projectTarget(spec, st.settings)}", color = c.text2, style = cutStyle(11, 700, 1f, tabular = true))
            }
            Text(spec?.title ?: "A new project appears Monday.", color = c.text, style = cutStyle(17, 700, 1.2f))
            if (spec != null) {
                val filled = if (st.project?.done == true) 3 else (st.projectProgress * 3 / projectTarget(spec, st.settings).coerceAtLeast(1)).coerceIn(0, 3)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until 3) Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < filled) c.good else c.hairline))
                }
                Sub("+${ProgramSpec.XP_PROJECT} xp and the ${spec.badgeName} badge. New project every Monday.", size = 12)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
            val target = st.settings.workoutsPerWeek
            val w = st.workoutsThisWeek
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Workouts this week", color = c.text, style = cutStyle(13, 700, 1.2f))
                Text("$w of $target", color = if (w >= target) c.good else c.text2, style = cutStyle(11, 700, 1f, tabular = true))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until target.coerceAtLeast(1)) Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < w) c.good else c.hairline))
            }
            Sub("Push block on a Climb day or the core block on an Off day. +${ProgramSpec.XP_WORKOUT_TARGET} xp the week you hit it.", size = 12)
        }
    }
}

private fun badgeColor(id: String, c: com.relentnet.cuttracker.ui.theme.CutColors): Color = when {
    id.startsWith("chain_") -> c.good
    id == BadgeIds.CAME_BACK -> c.freeze
    else -> c.accent
}

private fun badgeIcon(id: String): ImageVector = when {
    id == BadgeIds.FIRST_SEND -> CutIcons.Star
    id.startsWith("chain_") -> CutIcons.Link
    id == BadgeIds.CAME_BACK -> CutIcons.Layers
    else -> CutIcons.Flag
}

@Composable
fun BadgeShelf(earned: Set<String>) {
    val c = CutTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in BadgeIds.all.chunked(4)) {
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
                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
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
private fun HoldPopover(st: AppState, date: LocalDate, onClose: () -> Unit, onEdit: () -> Unit) {
    val c = CutTheme.colors
    val r = st.results[date] ?: DayResult.FUTURE
    val log = st.logs.firstOrNull { it.date == date }
    val xp = st.xpEvents.filter { it.date == date }.sumOf { it.amount }
    val items = ProgramEngine.script(log?.mode, st.settings, log?.dinnerIndex)
    val done = log?.doneItemIds?.count { id -> items.any { it.id == id } } ?: 0
    val weight = st.weighIns.firstOrNull { it.date == date }?.lb
    val (title, color) = when (r) {
        DayResult.SEND -> "Send · +$xp xp" to c.good
        DayResult.MIN -> "Min day · +$xp xp" to c.good
        DayResult.FREEZE -> "Freeze · 0 xp" to c.text2
        DayResult.MISSED -> "Missed · 0 xp" to c.text2
        DayResult.TODAY -> "Today · so far" to c.accent
        else -> "Upcoming" to c.text2
    }
    val detail = when (r) {
        DayResult.FREEZE -> "Missed. A freeze covered it, chain kept."
        DayResult.MISSED -> "Nothing logged. Grey hold, that is all."
        DayResult.TODAY -> "In progress. Finish the script for a Send."
        DayResult.FUTURE, DayResult.BEFORE -> "Not here yet."
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
    Box(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)) {
        Card(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp).padding(bottom = 12.dp).windowInsetsPadding(WindowInsets.navigationBars).fillMaxWidth(),
            border = c.mutedBorder, radius = 14.dp, padding = PaddingValues(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(date.format(popFmt), color = c.text, style = cutStyle(15, 800, 1f))
                    Text(title, color = color, style = cutStyle(12, 800, 1f, tabular = true))
                }
                Sub(detail, size = 12)
                val editable = !date.isAfter(st.today) && !date.isBefore(st.settings.startDate)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editable) BigButton(if (date == st.today) "Open today" else "Edit day", onClick = onEdit, modifier = Modifier.weight(1f), height = 40.dp, fontSize = 12)
                    BigButton("Close", onClick = onClose, modifier = Modifier.weight(1f), secondary = true, height = 40.dp, fontSize = 12)
                }
            }
        }
    }
}
