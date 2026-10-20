package com.relentnet.cuttracker.ui.weight

import com.relentnet.cuttracker.data.w
import com.relentnet.cuttracker.data.wu
import com.relentnet.cuttracker.data.wr
import com.relentnet.cuttracker.data.wru
import com.relentnet.cuttracker.data.unit
import com.relentnet.cuttracker.data.shown
import com.relentnet.cuttracker.data.toLb
import com.relentnet.cuttracker.data.stepped
import com.relentnet.cuttracker.data.liftW
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.WeighIn
import com.relentnet.cuttracker.ui.route.currentCheckpointDate
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.Sub
import com.relentnet.cuttracker.ui.components.TileLabel
import com.relentnet.cuttracker.ui.components.tickedFloat
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor


/** The big weight number as Settings, Weight rounds it: "182.4" or "182". */
fun heroNumber(shown: Float, s: com.relentnet.cuttracker.data.Settings): String =
    if (s.prefs.weightDecimals == 0) Math.round(shown).toString() else ProgramEngine.fmt1(shown.toDouble())

/** The one sentence about the pace, from the average's gap to the pace line. Weight's hero and Today's tile both say it. */
fun paceWords(gap: Double, s: com.relentnet.cuttracker.data.Settings): String = when {
    gap <= -ProgramSpec.PACE_TOLERANCE_LB -> "${s.wru(-gap)} ahead of pace"
    gap <= ProgramSpec.PACE_TOLERANCE_LB -> "On pace"
    else -> "${s.wru(gap)} behind pace"
}

@Composable
fun WeightScreen(vm: AppViewModel, onBack: () -> Unit, onCheckpoints: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    // Settings, Weight can put the latest reading in the big number; the pace sentence still comes from the average.
    val latest = st.weighIns.maxByOrNull { it.date }
    val heroLatest = s.prefs.weightHero == "latest" && latest != null
    val shownAvg = tickedFloat(s.shown(if (heroLatest) latest!!.lb else avg ?: s.startWeight).toFloat())
    val anchor = ProgramEngine.paceAnchor(st.weighIns, s)
    val pace = ProgramEngine.paceWeight(st.dayN.coerceIn(1, s.totalDays), s, anchor)
    val gap = avg?.let { it - pace }
    val rate = ProgramEngine.rate(st.weighIns, st.today)
    val projected = ProgramEngine.projectedAt(st.weighIns, st.today, s.endDate)
    val weekAgo = ProgramEngine.sevenDayAverage(st.weighIns.filter { it.date.isBefore(st.today.minusDays(6)) }, st.today.minusDays(7))
    val cpDate = currentCheckpointDate(st)
    val cp = s.checkpoints.firstOrNull { it.localDate == cpDate }

    Screen(bottomInset = true) {
        BackHeader(backLabel = "Today", title = "Weight", onBack = onBack)
        Card(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The verdict is the glance; the numbers it comes from sit under it.
                if (gap != null) Text(paceWords(gap, s), color = if (gap <= ProgramSpec.PACE_TOLERANCE_LB) c.good else c.accent, style = cutStyle(24, 800, 1f, -0.02f, display = true, tabular = true))
                else Text("No average yet", color = c.text2, style = cutStyle(24, 800, 1f, -0.02f, display = true))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TileLabel(if (heroLatest) "Latest · ${com.relentnet.cuttracker.data.Dates.md(latest!!.date)}" else "7-day average")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(if (avg == null) "--" else heroNumber(shownAvg, s), color = c.text, style = cutStyle(40, 800, 1f, -0.02f, tabular = true))
                            Text(" ${s.unit}", color = c.text2, style = cutStyle(14, 600, 1f), modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TileLabel(if (anchor != s.startWeight) "Pace today · from week 1" else "Pace today")
                        Text(s.wr(pace), color = c.text2, style = cutStyle(22, 800, 1f, tabular = true))
                    }
                }
                if (avg == null) Sub("Log a weigh-in on Today to start the average.", size = 12)
            }
        }
        val wk = if (avg != null && weekAgo != null) avg - weekAgo else null
        // Week one has neither number yet: one line says when they come, instead of two tiles of dashes.
        if (wk == null && projected == null) Sub("A week of readings unlocks the rate; two weeks, the change against last week.", size = 12)
        else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f), padding = PaddingValues(14.dp, 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TileLabel("This week")
                    Text(if (wk == null) "--" else (if (wk > 0) "+" else "") + s.wr(wk), color = if (wk != null && wk < 0) c.good else c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Sub(if (wk == null) "needs two weeks" else "${s.unit} vs last week", size = 11)
                }
            }
            Card(Modifier.weight(1f), padding = PaddingValues(14.dp, 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TileLabel("At this rate")
                    Text(if (projected == null) "--" else s.wr(projected), color = if (projected != null && projected <= s.targetWeight + ProgramSpec.PACE_TOLERANCE_LB) c.good else c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Sub(if (projected == null) "needs a week of readings" else "on ${com.relentnet.cuttracker.data.Dates.md(s.endDate)} · goal ${s.w(s.targetWeight)}", size = 11)
                }
            }
        }
        if (cp != null && cp.target != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(st.today, cp.localDate).toInt()
            val toLose = ((avg ?: s.startWeight) - cp.target).coerceAtLeast(0.0)
            Card(Modifier.fillMaxWidth().clickable(onClick = onCheckpoints, role = Role.Button), border = c.hairline, padding = PaddingValues(14.dp, 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TileLabel("Next checkpoint · ${com.relentnet.cuttracker.data.Dates.md(cp.localDate)}")
                        Text(
                            if (toLose == 0.0) "Already under ${s.w(cp.target)}" else "${s.wru(toLose)} to ${s.w(cp.target)}",
                            color = c.text, style = cutStyle(15, 700, 1.1f, tabular = true),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(when (days) { 0 -> "today"; 1 -> "tomorrow"; else -> "$days days" }, color = c.text2, style = cutStyle(12, 700, 1f, tabular = true))
                        Text("CHECKPOINTS ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val win = chartWindow(st.dayN, s.totalDays, s.prefs.chartWeeks)
            Eyebrow((if (win.first == 1 && win.last == s.totalDays) "${s.lengthWeeks} weeks" else "Last ${s.prefs.chartWeeks} weeks") + if (s.prefs.chartDots) " · every reading" else " · hold to show daily")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
                    Text("AVG", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(2.dp).dashed(c.mutedBorder))
                    Text("PACE", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
                if (projected != null && s.prefs.chartRate) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(2.dp).dashed(c.accent.copy(alpha = 0.5f)))
                    Text("RATE", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
            }
        }
        Card(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp, 14.dp, 12.dp, 10.dp)) { WeightChart(st) }
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) {
                if (st.weighIns.none { it.date == st.today }) vm.pendingWeigh = true
                onBack()
            }.padding(vertical = 14.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sub("Readings are logged on Today's weigh-in row.", size = 12)
            Text("TODAY ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
        }
        Sub(
            if (heroLatest || s.prefs.chartDots) "One reading is noise. The pace sentence, the pace line and the rate come from the 7-day average."
            else "Daily readings stay hidden. The average, the pace line and the rate are what matter; one reading is noise.",
            size = 12,
        )
    }
}

private fun Modifier.dashed(color: Color) = drawBehind {
    drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
}

/**
 * The route chart: dashed pace line, accent 7-day average, daily dots while pressed (or always, from Settings). Settings,
 * Weight can narrow it to the last few weeks and drop the rate line or the checkpoint rings. Pure Canvas.
 */
@Composable
fun WeightChart(st: AppState) {
    val c = CutTheme.colors
    val s = st.settings
    val p = s.prefs
    var pressed by remember { mutableStateOf(false) }
    val showDots = pressed || p.chartDots
    val measurer = rememberTextMeasurer()
    val labelStyle = cutStyle(10, 500, 1f, tabular = true).copy(color = c.text2)
    val axisStyle = cutStyle(9, 500, 1f).copy(color = c.dim)
    val sorted = remember(st.weighIns) { st.weighIns.sortedBy { it.date } }
    val current = currentCheckpointDate(st)
    val days = chartWindow(st.dayN, s.totalDays, p.chartWeeks)
    fun dayOf(w: WeighIn) = ProgramEngine.dayNumber(w.date, s.startDate)
    val avgs: List<Pair<WeighIn, Double>> = remember(sorted, s.startDate) {
        sorted.mapIndexed { i, w -> w to (ProgramEngine.sevenDayAverage(sorted.subList(0, i + 1), w.date) ?: w.lb) }
            .filter { !it.first.date.isBefore(s.startDate) }
    }.filter { dayOf(it.first) in days }
    val inRange = sorted.filter { !it.date.isBefore(s.startDate) && dayOf(it) in days }
    val readings = inRange.map { it.lb }
    val projected = ProgramEngine.projectedAt(sorted, st.today, s.endDate)
    val anchor = ProgramEngine.paceAnchor(sorted, s)
    // The pace line as the chart draws it: the anchor on day 1, the goal on the last day.
    fun paceAt(d: Int) = anchor - (anchor - s.targetWeight) * (d - 1).toDouble() / (s.totalDays - 1).coerceAtLeast(1)
    // Where the rate line meets the chart's right edge: the end of the route, or the window's last day.
    val lastAvgDay = avgs.lastOrNull()?.let { dayOf(it.first) }
    val projectedEdge = if (projected == null || lastAvgDay == null) null
        else if (days.last >= s.totalDays) projected
        else avgs.last().second + (projected - avgs.last().second) * (days.last - lastAvgDay).toDouble() / (s.totalDays - lastAvgDay).coerceAtLeast(1)
    val whole = days.first == 1 && days.last == s.totalDays
    val (top, bot) = if (whole) chartRange(s.startWeight, s.targetWeight, readings, projected)
        else chartRange(paceAt(days.first), paceAt(days.last), readings + avgs.map { it.second }, projectedEdge?.takeIf { p.chartRate })
    val span = (days.last - days.first).coerceAtLeast(1).toFloat()
    val firstDate = s.startDate.plusDays(days.first - 1L)
    val lastDate = s.startDate.plusDays(days.last - 1L)

    val said = "Weight chart${if (whole) "" else ", " + com.relentnet.cuttracker.data.Dates.span(firstDate, lastDate)}. ${readings.size} readings" + (readings.lastOrNull()?.let { ", the last ${s.wu(it)}" } ?: "") +
        ", the goal ${s.wu(s.targetWeight)}" + (projected?.let { ", on this pace ${s.wu(it)} at the end" } ?: "") + "."
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(326f / 190f).semantics { contentDescription = said }.pointerInput(Unit) {
            detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false })
        },
    ) {
        val left = 28.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val yTop = 8.dp.toPx()
        val yBot = size.height - 10.dp.toPx()
        fun yOf(lb: Double) = (yTop + (top - lb) / (top - bot) * (yBot - yTop)).toFloat()
        fun xOf(dayN: Int) = left + ((dayN - days.first).coerceIn(0, days.last - days.first) / span) * (right - left)
        val mid = (top + bot) / 2
        for (v in listOf(top, mid, bot)) {
            drawLine(c.hairline, Offset(left, yOf(v)), Offset(right, yOf(v)), 1.dp.toPx())
            drawText(measurer, s.liftW(v), Offset(0f, yOf(v) - 6.dp.toPx()), labelStyle)
        }
        drawLine(
            c.mutedBorder, Offset(xOf(days.first), yOf(paceAt(days.first))), Offset(xOf(days.last), yOf(paceAt(days.last))), 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
        )
        // Today's column and the checkpoint targets, so the chart says where you are, not only where you were.
        val xToday = xOf(st.dayN.coerceIn(1, s.totalDays))
        drawLine(c.hairline, Offset(xToday, yTop), Offset(xToday, yBot), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
        if (p.chartCheckpoints) for (cp in s.checkpoints) {
            val t = cp.target ?: continue
            if (t < bot || t > top) continue
            val d = ProgramEngine.dayNumber(cp.localDate, s.startDate)
            if (d !in days) continue
            val x = xOf(d)
            // The current flag wears the accent ring it wears on Route; passed ones are text2, later ones muted.
            if (cp.localDate == current) drawCircle(c.accent, 4.dp.toPx(), Offset(x, yOf(t)), style = Stroke(2.dp.toPx()))
            else drawCircle(if (!cp.localDate.isAfter(st.today)) c.text2 else c.mutedBorder, 3.dp.toPx(), Offset(x, yOf(t)))
        }
        if (showDots) for (w in inRange) drawCircle(c.dim, 2.5.dp.toPx(), Offset(xOf(dayOf(w)), yOf(w.lb)))
        if (avgs.isNotEmpty()) {
            val path = Path()
            avgs.forEachIndexed { i, (w, a) ->
                val pt = Offset(xOf(dayOf(w)), yOf(a))
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(path, c.accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val (lw, la) = avgs.last()
            val end = Offset(xOf(dayOf(lw)), yOf(la))
            // Where the current rate lands: the glance the page is for.
            if (projectedEdge != null && p.chartRate) drawLine(
                c.accent.copy(alpha = 0.5f), end, Offset(xOf(days.last), yOf(projectedEdge.coerceIn(bot, top))), 2.dp.toPx(), StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())),
            )
            drawCircle(c.surface, 7.dp.toPx(), end)
            drawCircle(c.accent, 5.dp.toPx(), end)
        }
        drawText(measurer, com.relentnet.cuttracker.data.Dates.md(firstDate), Offset(left, yBot - 4.dp.toPx() - 9.sp.toPx()), axisStyle)
        val endLabel = measurer.measure(com.relentnet.cuttracker.data.Dates.md(lastDate), axisStyle)
        drawText(endLabel, topLeft = Offset(right - endLabel.size.width, yBot - 4.dp.toPx() - 9.sp.toPx()))
    }
}

/**
 * The days the chart shows: the whole route for [weeks] 0, else that many weeks ending today, slid forward at the start
 * of the route so the window is always full when the route is long enough.
 */
fun chartWindow(dayN: Int, total: Int, weeks: Int): IntRange {
    if (weeks <= 0 || weeks * 7 >= total) return 1..total
    val last = dayN.coerceIn(weeks * 7, total)
    return (last - weeks * 7 + 1)..last
}

/**
 * The chart's y range (top, bottom): start to goal, stretched to the readings and to the projection, the projection no
 * further than 5 lb past either end so a wild early rate cannot flatten the route.
 */
fun chartRange(start: Double, target: Double, readings: List<Double>, projected: Double?): Pair<Double, Double> {
    val p = projected?.coerceIn(target - 5, start + 5)
    val top = ceil(listOfNotNull(start, readings.maxOrNull(), p).max())
    val bot = floor(listOfNotNull(target, readings.minOrNull(), p).min())
    return top to bot
}

private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
