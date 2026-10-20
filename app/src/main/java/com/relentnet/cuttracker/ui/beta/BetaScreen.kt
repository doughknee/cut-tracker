package com.relentnet.cuttracker.ui.beta

import com.relentnet.cuttracker.data.w
import com.relentnet.cuttracker.data.wu
import com.relentnet.cuttracker.data.unit
import com.relentnet.cuttracker.data.shown
import com.relentnet.cuttracker.data.toLb
import com.relentnet.cuttracker.data.stepped
import com.relentnet.cuttracker.data.vocab
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import com.relentnet.cuttracker.ui.components.holdShape
import com.relentnet.cuttracker.ui.components.pressable
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val cpFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** Beta: the plan for the route. A grid of tiles that fills the page; every tile is its own page. */
@Composable
fun BetaScreen(
    vm: AppViewModel,
    onMeals: () -> Unit, onLifts: () -> Unit, onDay: (DayMode) -> Unit,
    onCheckpoints: () -> Unit, onTargets: () -> Unit, onWeek: () -> Unit,
) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    Column(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 14.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Column {
                Eyebrow("The plan for the route")
                Text(CutTheme.vocab.beta, color = c.text, style = cutStyle(40, 800, 1f, -0.02f, display = true), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            }
        }
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f)) {
        val scrolls = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.15f || maxHeight < 560.dp
        Column(
            if (scrolls) Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()) else Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Fixed shares of the screen when it fits, a minimum height each when the page scrolls.
            fun ColumnScope.share(w: Float): Modifier = if (scrolls) Modifier.heightIn(min = (w * 150).dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min) else Modifier.weight(w)
            Row(share(1.3f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorTile(
                    Modifier.weight(1f), CutIcons.Meals, "Meals",
                    rows = s.meals.map { m -> m.name to s.library.count { it.kind == m.id }.toString() }.take(4),
                    foot = "${com.relentnet.cuttracker.ui.components.count(s.library.size, "meal")}, ${st.mealsLoggedThisWeek()} logged this week", onClick = onMeals,
                )
                EditorTile(
                    Modifier.weight(1f), CutIcons.Lift, "Lifts",
                    rows = s.liftKinds.map { k -> k to s.exercises.count { it.kind == k }.toString() }.take(4),
                    foot = com.relentnet.cuttracker.ui.components.count(s.exercises.size, "lift"), onClick = onLifts,
                )
            }
            Row(share(0.9f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in DayMode.entries) DayTile(Modifier.weight(1f), mode, st) { onDay(mode) }
            }
            CheckpointTile(share(0.9f), st, onCheckpoints)
            Row(share(1.05f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditorTile(
                    Modifier.weight(1f), CutIcons.Target, "Targets",
                    rows = listOf("Protein" to "${s.protein} g", "Calories" to "%,d".format(Locale.US, s.calories), "Workouts" to "${s.workoutsPerWeek} a week"),
                    foot = null, onClick = onTargets,
                )
                WeekTile(Modifier.weight(1f), st, onWeek)
            }
        }
        }
    }
}

/** How many meal rows were ticked in the Monday-to-Sunday week of today. */
private fun AppState.mealsLoggedThisWeek(): Int {
    val ws = ProgramEngine.weekStart(today)
    val slots = settings.meals.map { it.id }.toSet()
    return logs.filter { !it.date.isBefore(ws) && !it.date.isAfter(ws.plusDays(6)) }.sumOf { l -> l.doneItemIds.count { it in slots } }
}

@Composable
private fun TileFrame(modifier: Modifier, outlined: Boolean = false, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = CutTheme.colors
    Card(
        modifier.fillMaxHeight().pressable(onClick = onClick), radius = 16.dp,
        background = if (outlined) c.ground else c.surface, border = if (outlined) c.hairline else null,
    ) { Column(Modifier.fillMaxSize().padding(14.dp), content = content) }
}

@Composable
private fun TileTop(icon: ImageVector? = null, shape: HoldShape? = null, tint: Color = CutTheme.colors.accent, holdSize: Int = 30, badge: String? = null) {
    val c = CutTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        if (shape != null) Hold(shape, tint, holdSize.dp) else if (icon != null) Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (badge != null) Text(badge.uppercase(), color = c.accent, style = cutStyle(10, 800, 1f, 0.08f))
            Icon(CutIcons.Forward, null, Modifier.size(12.dp), tint = c.text2)
        }
    }
}

/** Title first, then what is inside as hairline rows, then the number that matters. */
@Composable
private fun EditorTile(modifier: Modifier, icon: ImageVector, title: String, rows: List<Pair<String, String>>, foot: String?, onClick: () -> Unit) {
    val c = CutTheme.colors
    TileFrame(modifier, onClick = onClick) {
        TileTop(icon)
        Text(title, color = c.text, style = cutStyle(22, 800, 1f, -0.02f, display = true), modifier = Modifier.padding(top = 10.dp))
        Column(Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
            for ((name, value) in rows) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = c.text, style = cutStyle(11, 700, 1.3f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text(value, color = c.text2, style = cutStyle(11, 600, 1.3f, tabular = true), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        if (foot != null) Text(foot, color = c.text2, style = cutStyle(11, 500, 1.2f, tabular = true), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DayTile(modifier: Modifier, mode: DayMode, st: AppState, onClick: () -> Unit) {
    val c = CutTheme.colors
    val items = ProgramEngine.script(mode, st.settings)
    // The short form of dayWhere: it follows the plan too, so a Rest day with a lift does not say "weigh in + eat".
    val block = ProgramEngine.exercisesFor(mode, st.settings).isNotEmpty()
    val line = when (mode) {
        DayMode.CLIMB -> if (block) "Session + push" else "Session only"
        DayMode.OFF -> if (block) "Block at home" else "No block"
        DayMode.REST -> if (block) "Weigh in + block" else "Weigh in + eat"
    }
    TileFrame(modifier, onClick = onClick) {
        TileTop(shape = holdShape(mode))
        Text(CutTheme.vocab.day(mode), color = c.text, style = cutStyle(16, 800, 1f, display = true), modifier = Modifier.padding(top = 10.dp))
        Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.Center) {
            Text(line, color = c.text2, style = cutStyle(11, 500, 1.35f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${items.size} on, ${ProgramEngine.totalXp(items)} xp", color = c.text2, style = cutStyle(11, 500, 1.35f, tabular = true), maxLines = 1)
        }
    }
}


/** What a kind of day asks, in one sentence that follows the plan: the day page and Today's cards both say it. */
fun dayWhere(mode: DayMode, s: com.relentnet.cuttracker.data.Settings): String {
    val block = ProgramEngine.exercisesFor(mode, s).isNotEmpty()
    val v = s.vocab
    return when (mode) {
        DayMode.CLIMB -> if (block) v.sessionThenBlock else v.sessionNoBlock
        DayMode.OFF -> if (block) "${v.noSession} A short block at home." else "${v.noSession} Weigh in and eat the script."
        DayMode.REST -> if (block) "Weigh in and eat the script. A short block at home." else "Weigh in and eat the script. Nothing else is asked."
    }
}

@Composable
private fun CheckpointTile(modifier: Modifier, st: AppState, onClick: () -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val cps = s.checkpoints.filter { !it.free }.sortedBy { it.date }
    val next = cps.firstOrNull { !st.today.isAfter(it.localDate) }
    val idx = cps.indexOf(next)
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today) ?: st.lastWeighIn?.lb ?: s.startWeight
    val anchor = ProgramEngine.paceAnchor(st.weighIns, s)
    val behind = !ProgramEngine.onPace(avg, st.dayN.coerceIn(1, s.totalDays), s, anchor)
    TileFrame(modifier, outlined = true, onClick = onClick) {
        TileTop(CutIcons.Flag, tint = c.text2, badge = if (!st.ended && com.relentnet.cuttracker.ui.route.photoDueToday(st)) "Photo due" else null)
        Text("Checkpoints", color = c.text2, style = cutStyle(16, 800, 1f, display = true), modifier = Modifier.padding(top = 10.dp))
        Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.Center) {
            if (next?.target != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.w(next.target), color = c.text, style = cutStyle(32, 800, 1f, -0.03f, display = true, tabular = true))
                    Text("${s.unit} on ${next.localDate.format(cpFmt)}, ${com.relentnet.cuttracker.ui.components.inDays(ChronoUnit.DAYS.between(st.today, next.localDate))}", color = c.text2, style = cutStyle(11, 700, 1f, tabular = true), modifier = Modifier.padding(bottom = 3.dp).weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in cps.indices) Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < idx) c.good else if (i == idx) c.accent else c.hairline))
                }
            } else Text("No checkpoint ahead", color = c.text, style = cutStyle(15, 700, 1.2f))
        }
        if (next?.target != null) {
            val toGo = avg - next.target
            Text(
                "${idx + 1} of ${cps.size}, ${s.w(kotlin.math.abs(toGo))} ${if (toGo >= 0) "to go" else "under"}, ${if (behind) "behind pace" else "on pace"}",
                color = if (behind) c.accent else c.good, style = cutStyle(11, 600, 1.2f, tabular = true), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeekTile(modifier: Modifier, st: AppState, onClick: () -> Unit) {
    val c = CutTheme.colors
    val s = st.settings
    val ws = ProgramEngine.weekStart(st.today)
    val hitOn = { d: java.time.LocalDate -> st.day(d).totals.protein >= s.protein }
    val days = (0..6).map { ws.plusDays(it.toLong()) }.filter { !it.isAfter(st.today) && !it.isBefore(s.startDate) && it.isBefore(s.endDate) && (it != st.today || hitOn(it)) }
    val hit = days.count(hitOn)
    TileFrame(modifier, outlined = true, onClick = onClick) {
        TileTop(CutIcons.Week, tint = c.text2)
        Text("This week", color = c.text2, style = cutStyle(16, 800, 1f, display = true), modifier = Modifier.padding(top = 10.dp))
        Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.Center) {
            Meter("Protein", hit, days.size, c.good)
            Box(Modifier.height(8.dp))
            Meter("Workouts", st.workoutsThisWeek, s.workoutsPerWeek.coerceAtLeast(1), c.good)
        }
        Text("${ws.format(cpFmt)} to ${ws.plusDays(6).dayOfMonth}", color = c.text2, style = cutStyle(11, 500, 1.2f, tabular = true))
    }
}

@Composable
private fun Meter(label: String, done: Int, of: Int, color: Color) {
    val c = CutTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = c.text, style = cutStyle(11, 700, 1f))
            Text(if (of == 0) "none yet" else "$done of $of", color = if (of > 0 && done >= of) color else c.text2, style = cutStyle(11, 700, 1f, tabular = true))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 0 until of.coerceIn(1, 7)) Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i < done) color else c.hairline))
        }
    }
}
