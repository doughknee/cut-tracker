package com.relentnet.cuttracker.ui.weight

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

private val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** The one sentence about the pace, from the average's gap to the pace line. Weight's hero and Today's tile both say it. */
fun paceWords(gap: Double): String = when {
    gap <= -ProgramSpec.PACE_TOLERANCE_LB -> "${ProgramEngine.fmt1(-gap)} lb ahead of pace"
    gap <= ProgramSpec.PACE_TOLERANCE_LB -> "On pace"
    else -> "${ProgramEngine.fmt1(gap)} lb behind pace"
}

@Composable
fun WeightScreen(vm: AppViewModel, onBack: () -> Unit, onCheckpoints: () -> Unit) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    val shownAvg = tickedFloat((avg ?: s.startWeight).toFloat())
    val anchor = ProgramEngine.paceAnchor(st.weighIns, s)
    val pace = ProgramEngine.paceWeight(st.dayN, s, anchor)
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
                if (gap != null) Text(paceWords(gap), color = if (gap <= ProgramSpec.PACE_TOLERANCE_LB) c.good else c.accent, style = cutStyle(24, 800, 1f, -0.02f, display = true, tabular = true))
                else Text("No average yet", color = c.text2, style = cutStyle(24, 800, 1f, -0.02f, display = true))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TileLabel("7-day average")
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(if (avg == null) "--" else ProgramEngine.fmt1(shownAvg.toDouble()), color = c.text, style = cutStyle(40, 800, 1f, -0.02f, tabular = true))
                            Text(" lb", color = c.text2, style = cutStyle(14, 600, 1f), modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        TileLabel(if (anchor != s.startWeight) "Pace today · from week 1" else "Pace today")
                        Text(ProgramEngine.fmt1(pace), color = c.text2, style = cutStyle(22, 800, 1f, tabular = true))
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
                    Text(if (wk == null) "--" else (if (wk > 0) "+" else "") + ProgramEngine.fmt1(wk), color = if (wk != null && wk < 0) c.good else c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Sub(if (wk == null) "needs two weeks" else "lb vs last week", size = 11)
                }
            }
            Card(Modifier.weight(1f), padding = PaddingValues(14.dp, 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TileLabel("At this rate")
                    Text(if (projected == null) "--" else ProgramEngine.fmt1(projected), color = if (projected != null && projected <= s.targetWeight + ProgramSpec.PACE_TOLERANCE_LB) c.good else c.text, style = cutStyle(22, 800, 1f, tabular = true))
                    Sub(if (projected == null) "needs a week of readings" else "on ${s.endDate.format(shortDate)} · goal ${ProgramEngine.fmt1(s.targetWeight)}", size = 11)
                }
            }
        }
        if (cp != null && cp.target != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(st.today, cp.localDate).toInt()
            val toLose = ((avg ?: s.startWeight) - cp.target).coerceAtLeast(0.0)
            Card(Modifier.fillMaxWidth().clickable(onClick = onCheckpoints, role = Role.Button), border = c.hairline, padding = PaddingValues(14.dp, 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TileLabel("Next checkpoint · ${cp.localDate.format(shortDate)}")
                        Text(
                            if (toLose == 0.0) "Already under ${ProgramEngine.fmt1(cp.target)}" else "${ProgramEngine.fmt1(toLose)} lb to ${ProgramEngine.fmt1(cp.target)}",
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
            Eyebrow("${s.lengthWeeks} weeks · hold to show daily")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.accent))
                    Text("AVG", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(2.dp).dashed(c.mutedBorder))
                    Text("PACE", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
                if (projected != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.width(14.dp).height(2.dp).dashed(c.accent.copy(alpha = 0.5f)))
                    Text("RATE", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
            }
        }
        Card(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp, 14.dp, 12.dp, 10.dp)) { WeightChart(st) }
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onBack, role = Role.Button).padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sub("Readings are logged on Today's weigh-in row.", size = 12)
            Text("TODAY ›", color = c.text2, style = cutStyle(10, 800, 1f, 0.08f))
        }
        Sub("Daily readings stay hidden. The average, the pace line and the rate are what matter; one reading is noise.", size = 12)
    }
}

private fun Modifier.dashed(color: Color) = drawBehind {
    drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))
}

/** 12-week chart: dashed target line, accent 7-day average, daily dots only while pressed. Pure Canvas. */
@Composable
fun WeightChart(st: AppState) {
    val c = CutTheme.colors
    val s = st.settings
    var showDots by remember { mutableStateOf(false) }
    val measurer = rememberTextMeasurer()
    val labelStyle = cutStyle(10, 500, 1f, tabular = true).copy(color = c.text2)
    val axisStyle = cutStyle(9, 500, 1f).copy(color = c.dim)
    val sorted = remember(st.weighIns) { st.weighIns.sortedBy { it.date } }
    val current = currentCheckpointDate(st)
    val avgs: List<Pair<WeighIn, Double>> = remember(sorted) {
        sorted.mapIndexed { i, w -> w to (ProgramEngine.sevenDayAverage(sorted.subList(0, i + 1), w.date) ?: w.lb) }
    }
    val readings = sorted.map { it.lb }
    val projected = ProgramEngine.projectedAt(sorted, st.today, s.endDate)
    val (top, bot) = chartRange(s.startWeight, s.targetWeight, readings, projected)
    val total = s.totalDays.toFloat()

    val said = "Weight chart. ${readings.size} readings" + (readings.lastOrNull()?.let { ", the last ${ProgramEngine.fmt1(it)} lb" } ?: "") +
        ", the goal ${ProgramEngine.fmt1(s.targetWeight)} lb" + (projected?.let { ", on this pace ${ProgramEngine.fmt1(it)} lb at the end" } ?: "") + "."
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(326f / 190f).semantics { contentDescription = said }.pointerInput(Unit) {
            detectTapGestures(onPress = { showDots = true; tryAwaitRelease(); showDots = false })
        },
    ) {
        val left = 28.dp.toPx()
        val right = size.width - 6.dp.toPx()
        val yTop = 8.dp.toPx()
        val yBot = size.height - 10.dp.toPx()
        fun yOf(lb: Double) = (yTop + (top - lb) / (top - bot) * (yBot - yTop)).toFloat()
        fun xOf(dayN: Int) = left + ((dayN - 1).coerceIn(0, s.totalDays) / (total - 1)) * (right - left)
        val mid = (top + bot) / 2
        for (v in listOf(top, mid, bot)) {
            drawLine(c.hairline, Offset(left, yOf(v)), Offset(right, yOf(v)), 1.dp.toPx())
            drawText(measurer, ProgramEngine.fmt1(v).removeSuffix(".0"), Offset(0f, yOf(v) - 6.dp.toPx()), labelStyle)
        }
        val anchor = ProgramEngine.paceAnchor(sorted, s)
        drawLine(
            c.mutedBorder, Offset(xOf(1), yOf(anchor)), Offset(xOf(s.totalDays), yOf(s.targetWeight)), 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
        )
        // Today's column and the checkpoint targets, so the chart says where you are, not only where you were.
        val xToday = xOf(st.dayN)
        drawLine(c.hairline, Offset(xToday, yTop), Offset(xToday, yBot), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
        for (cp in s.checkpoints) {
            val t = cp.target ?: continue
            if (t < bot || t > top) continue
            val x = xOf(ProgramEngine.dayNumber(cp.localDate, s.startDate))
            // The current flag wears the accent ring it wears on Route; passed ones are text2, later ones muted.
            if (cp.localDate == current) drawCircle(c.accent, 4.dp.toPx(), Offset(x, yOf(t)), style = Stroke(2.dp.toPx()))
            else drawCircle(if (!cp.localDate.isAfter(st.today)) c.text2 else c.mutedBorder, 3.dp.toPx(), Offset(x, yOf(t)))
        }
        if (showDots) for (w in sorted) drawCircle(c.dim, 2.5.dp.toPx(), Offset(xOf(ProgramEngine.dayNumber(w.date, s.startDate)), yOf(w.lb)))
        if (avgs.isNotEmpty()) {
            val path = Path()
            avgs.forEachIndexed { i, (w, a) ->
                val p = Offset(xOf(ProgramEngine.dayNumber(w.date, s.startDate)), yOf(a))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, c.accent, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val (lw, la) = avgs.last()
            val end = Offset(xOf(ProgramEngine.dayNumber(lw.date, s.startDate)), yOf(la))
            // Where the current rate lands: the glance the page is for.
            if (projected != null) drawLine(
                c.accent.copy(alpha = 0.5f), end, Offset(xOf(s.totalDays), yOf(projected)), 2.dp.toPx(), StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())),
            )
            drawCircle(c.surface, 7.dp.toPx(), end)
            drawCircle(c.accent, 5.dp.toPx(), end)
        }
        drawText(measurer, s.startDate.format(shortDate), Offset(left, yBot - 4.dp.toPx() - 9.sp.toPx()), axisStyle)
        val endLabel = measurer.measure(s.endDate.format(shortDate), axisStyle)
        drawText(endLabel, topLeft = Offset(right - endLabel.size.width, yBot - 4.dp.toPx() - 9.sp.toPx()))
    }
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
