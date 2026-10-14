package com.relentnet.cuttracker.ui.weight

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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.relentnet.cuttracker.data.WeighIn
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Field
import com.relentnet.cuttracker.ui.components.Header
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

@Composable
fun WeightScreen(vm: AppViewModel) {
    val c = CutTheme.colors
    val st by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHaptics.current
    val s = st.settings
    val avg = ProgramEngine.sevenDayAverage(st.weighIns, st.today)
    val shownAvg = tickedFloat((avg ?: s.startWeight).toFloat())
    val onPace = avg != null && ProgramEngine.onPace(avg, st.dayN, s)
    val prefill = st.lastWeighIn?.lb ?: s.startWeight
    var text by rememberSaveable(prefill) { mutableStateOf(ProgramEngine.fmt1(prefill)) }
    fun log() { text.toDoubleOrNull()?.takeIf { it > 0 }?.let { haptics.tap(); vm.logWeight(it) } }

    Screen {
        Header(eyebrow = "Weigh-in · Week ${st.weekN}", title = "Weight")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1.4f), padding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TileLabel("7-day average")
                    Text(if (avg == null) "--" else ProgramEngine.fmt1(shownAvg.toDouble()), color = c.text, style = cutStyle(46, 800, 1f, -0.02f, tabular = true))
                    if (avg != null) {
                        val delta = avg - s.startWeight
                        Text(
                            (if (delta < 0) "" else "+") + ProgramEngine.fmt1(delta) + " lb since start · " + (if (onPace) "on pace" else "behind, cut 200 cal"),
                            color = if (onPace) c.good else c.accent, style = cutStyle(13, 600, 1.2f, tabular = true), modifier = Modifier.padding(top = 4.dp),
                        )
                    } else Sub("Log a weigh-in to start the average.", Modifier.padding(top = 4.dp), size = 12)
                }
            }
            Card(Modifier.weight(1f), padding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    TileLabel("Target ${s.endDate.format(shortDate)}")
                    Text(ProgramEngine.fmt1(s.targetWeight), color = c.text, style = cutStyle(30, 800, 1.2f, tabular = true))
                    Text("${ProgramEngine.fmt1(((avg ?: s.startWeight) - s.targetWeight).coerceAtLeast(0.0))} lb to go", color = c.text2, style = cutStyle(12, 500, 1f, tabular = true))
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
                    Text("TARGET", color = c.text2, style = cutStyle(11, 600, 1f, 0.06f))
                }
            }
        }
        Card(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp, 14.dp, 12.dp, 10.dp)) { WeightChart(st) }
        Eyebrow("Today, before food")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(text, { text = it }, Modifier.weight(1f), height = 64.dp, fontSize = 32, onDone = ::log, placeholder = ProgramEngine.fmt1(prefill), radius = 12.dp, padding = 18.dp)
            BigButton("Log", onClick = ::log, modifier = Modifier.width(110.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (q in listOf(-1.0, -0.2, 0.2, 1.0)) {
                Box(
                    Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(9.dp)).background(c.surface).border(1.dp, c.mutedBorder, RoundedCornerShape(9.dp))
                        .clickable(role = Role.Button) { text = ProgramEngine.fmt1((text.toDoubleOrNull() ?: prefill) + q); haptics.tick() },
                    contentAlignment = Alignment.Center,
                ) { Text((if (q > 0) "+" else "") + ProgramEngine.fmt1(q), color = c.text, style = cutStyle(13, 700, 1f, tabular = true)) }
            }
        }
        Sub("Daily readings stay hidden. You see the 7-day average and nothing else.", size = 12)
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
    val avgs: List<Pair<WeighIn, Double>> = remember(sorted) {
        sorted.mapIndexed { i, w -> w to (ProgramEngine.sevenDayAverage(sorted.subList(0, i + 1), w.date) ?: w.lb) }
    }
    val readings = sorted.map { it.lb }
    val top = ceil(maxOf(s.startWeight, readings.maxOrNull() ?: s.startWeight))
    val bot = floor(minOf(s.targetWeight, readings.minOrNull() ?: s.targetWeight))
    val total = s.totalDays.toFloat()

    Canvas(
        Modifier.fillMaxWidth().aspectRatio(326f / 190f).pointerInput(Unit) {
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
        drawLine(
            c.mutedBorder, Offset(xOf(1), yOf(s.startWeight)), Offset(xOf(s.totalDays), yOf(s.targetWeight)), 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
        )
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
            drawCircle(c.surface, 7.dp.toPx(), end)
            drawCircle(c.accent, 5.dp.toPx(), end)
        }
        drawText(measurer, s.startDate.format(shortDate), Offset(left, yBot - 4.dp.toPx() - 9.sp.toPx()), axisStyle)
        val endLabel = measurer.measure(s.endDate.format(shortDate), axisStyle)
        drawText(endLabel, topLeft = Offset(right - endLabel.size.width, yBot - 4.dp.toPx() - 9.sp.toPx()))
    }
}

private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
