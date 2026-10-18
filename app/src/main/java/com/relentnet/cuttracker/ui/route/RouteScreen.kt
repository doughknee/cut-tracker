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
import androidx.compose.foundation.layout.offset
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
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.holdFill
import com.relentnet.cuttracker.ui.components.holdNumberColor
import com.relentnet.cuttracker.ui.components.holdShape
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Eyebrow("The Route · ${s.lengthWeeks} weeks")
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Text("Day ${st.dayN}", color = c.text, style = cutStyle(42, 800, 1f, -0.03f, display = true, tabular = true))
                        Text(" / ${s.totalDays}", color = c.dim, style = cutStyle(42, 800, 1f, -0.03f, display = true, tabular = true))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Stat("${st.sends}", "Sends", c.good)
                    Stat("${st.chain.count}", "Chain", c.text, icon = true)
                }
            }
            ProjectCard(st)
            HoldCalendar(st, current, onDay = { pop = it; haptics.tick() })
            NextCheckpoint(st, current, onCheckpoints)
            Legend()
            BadgeShelf(st.badges)
        }
        pop?.let { date -> HoldPopover(st, date, onClose = { pop = null }, onEdit = { pop = null; onEditDay(date) }) }
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
        Text(label.uppercase(), color = c.text2, style = cutStyle(10, 700, 1f, 0.08f))
    }
}

private val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US)

/** Twelve Monday-to-Sunday rows. Climbed days are holds (shape = kind of day, fill = result); the rest are numbers. */
@Composable
private fun HoldCalendar(st: AppState, current: LocalDate?, onDay: (LocalDate) -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val modes = st.logs.associate { it.date to it.mode }
    val firstMon = ProgramEngine.weekStart(s.startDate)
    val rows = (java.time.temporal.ChronoUnit.DAYS.between(firstMon, s.endDate).toInt() + 6) / 7
    val cps = s.checkpoints.associateBy { it.localDate }
    var lastMonth = -1
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
                val reached = !mon.isAfter(st.today)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.width(34.dp), color = if (reached) c.text else c.dim, style = cutStyle(10, 800, 1f, 0.06f))
                    for (d in 0 until 7) {
                        val day = mon.plusDays(d.toLong())
                        val r = st.results[day]
                        val cp = cps[day]
                        Box(Modifier.weight(1f).height(34.dp), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                when (r) {
                                    null -> Text("${day.dayOfMonth}", color = c.dim.copy(alpha = 0.5f), style = cutStyle(11, 600, 1f, tabular = true))
                                    DayResult.FUTURE, DayResult.BEFORE -> Text("${day.dayOfMonth}", color = c.dim, style = cutStyle(11, 700, 1f, tabular = true))
                                    else -> Hold(
                                        holdShape(modes[day]), holdFill(r, c), if (r == DayResult.TODAY) 32.dp else 30.dp,
                                        Modifier.clickable(onClick = { onDay(day) }, role = Role.Button),
                                        stroke = if (r == DayResult.TODAY) c.accent else null, strokeWidth = if (r == DayResult.TODAY) 2.4.dp else 0.dp,
                                    ) { Text("${day.dayOfMonth}", color = holdNumberColor(r, c), style = cutStyle(11, 900, 1f, tabular = true)) }
                                }
                                if (cp != null) Icon(
                                    CutIcons.Flag, null, Modifier.align(Alignment.TopEnd).offset(4.dp, (-6).dp).size(11.dp),
                                    tint = when {
                                        cp.localDate == current -> c.accent
                                        cp.free -> c.freeze
                                        else -> c.mutedBorder
                                    },
                                )
                            }
                        }
                    }
                    Text("${w + 1}", Modifier.width(22.dp), color = if (reached) c.text2 else c.dim, style = cutStyle(9, 700, 1f, tabular = true), textAlign = TextAlign.End)
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
            Key("Freeze") { Swatch(c.freeze) }
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
    val items = ProgramEngine.script(log?.mode, st.settings, log?.picks ?: emptyMap())
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
