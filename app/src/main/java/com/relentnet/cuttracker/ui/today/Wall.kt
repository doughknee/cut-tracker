package com.relentnet.cuttracker.ui.today

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.holdFill
import com.relentnet.cuttracker.ui.components.holdNumberColor
import com.relentnet.cuttracker.ui.components.holdShape
import com.relentnet.cuttracker.ui.components.drawWallDecor
import com.relentnet.cuttracker.ui.components.pressable
import com.relentnet.cuttracker.ui.components.ChromeButton
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val wdFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

/** A gentle S up the wall, Monday bottom-left to Sunday top-right. Same seven anchors every week, in a 390-wide box. */
private val ANCHORS = listOf(30 to 366, 104 to 332, 58 to 266, 146 to 236, 224 to 190, 296 to 158, 314 to 84)
private const val MOCK_WIDTH = 390f
private val HOLD_BASE = 44.dp
private val HOLD_BIG = 52.dp
/** Wall height below the status bar. */
val WALL_HEIGHT = 484.dp

fun resultName(r: DayResult, v: com.relentnet.cuttracker.data.Vocab): String = when (r) {
    DayResult.SEND -> v.send
    DayResult.MIN -> "Min day"
    DayResult.FREEZE -> "Freeze"
    DayResult.SKIP -> "Planned skip"
    DayResult.MISSED -> "Missed"
    DayResult.TODAY -> "In progress"
    DayResult.FUTURE -> "Ahead"
    DayResult.BEFORE -> "Before the start"
}

/**
 * The week as a climbing wall: one hold per day, the rope is the chain. Layers scroll at different speeds behind the
 * front chrome ([scroll] is the screen's scroll state), which is the parallax.
 */
@Composable
fun Wall(
    st: AppState,
    selected: LocalDate,
    scroll: ScrollState,
    onPickDay: (LocalDate) -> Unit,
    onWeek: (Int) -> Unit,
    /** Null drops the wall guide button (onboarding's demo wall, which explains itself). */
    onInfo: (() -> Unit)?,
    /** Scales the wall's height on top of the short-screen scale; onboarding draws a lower one. */
    heightScale: Float = 1f,
    /** How far the rope's newest segment is drawn, 0 to 1; onboarding's demo reaches each hold with it. */
    ropeDraw: Float = 1f,
    /** Results that pop when a hold takes them; Today pops a fresh Send, the demo every landing. */
    popOn: Set<DayResult> = setOf(DayResult.SEND),
    /** Rings ripple out of the selected hold once: the first landing on a new route. */
    pulse: Boolean = false,
) {
    val c = CutTheme.colors
    val v = CutTheme.vocab
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val weekStart = ProgramEngine.weekStart(selected)
    val days = (0 until 7).map { weekStart.plusDays(it.toLong()) }
    val modes = st.logs.associate { it.date to it.mode }
    val canEarlier = weekStart.minusDays(1) >= st.settings.startDate
    val canLater = weekStart.plusDays(7) <= st.today

    // Short screens get a shorter wall so the date headline still lands above the fold.
    val ys = (LocalConfiguration.current.screenHeightDp / 860f).coerceIn(0.72f, 1f) * heightScale
    val decor = com.relentnet.cuttracker.ui.theme.LocalDecor.current
    val p = CutTheme.prefs
    // Bigger holds (Settings, Today) grow every hold and the selected one with it; the anchors stay.
    val hold = if (p.bigHolds) HOLD_BIG else HOLD_BASE
    val holdSel = hold + 16.dp
    // A sideways swipe on the wall is the week pill's arrows: left for the later week, right for the earlier one.
    val week by rememberUpdatedState(onWeek)
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(WALL_HEIGHT * ys + statusTop).background(c.wall).pointerInput(canEarlier, canLater) {
            var dx = 0f
            detectHorizontalDragGestures(onDragStart = { dx = 0f }, onDragEnd = {
                val far = 56.dp.toPx()
                if (dx < -far && canLater) week(1) else if (dx > far && canEarlier) week(-1)
            }) { _, d -> dx += d }
        },
    ) {
        val xs = maxWidth / MOCK_WIDTH.dp
        fun ax(i: Int): Dp = ANCHORS[i].first.dp * xs
        fun ay(i: Int): Dp = ANCHORS[i].second.dp * ys + statusTop
        val sel = days.indexOf(selected)
        // The chalk glides from hold to hold rather than jumping, so the eye follows the climb.
        val calm = CutTheme.calm
        val glide = spring<Dp>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
        val chalkX by animateDpAsState(if (sel >= 0) ax(sel) else 0.dp, if (calm) snap() else glide, label = "chalkX")
        val chalkY by animateDpAsState(if (sel >= 0) ay(sel) else 0.dp, if (calm) snap() else glide, label = "chalkY")
        val ripple = remember { Animatable(0f) }
        LaunchedEffect(pulse) { if (pulse && !calm) { repeat(2) { ripple.snapTo(0f); ripple.animateTo(1f, tween(900, easing = androidx.compose.animation.core.EaseOutCubic)) } ; ripple.snapTo(0f) } }

        // Back layer: panel texture and seams, slowest.
        Box(
            Modifier.fillMaxSize().graphicsLayer { translationY = scroll.value * 0.35f }.drawBehind {
                // A plain wall (Settings, Feel and access) drops the texture; the seams stay so the wall still reads as one.
                if (!p.plainWall) {
                val dot = c.text.copy(alpha = if (c.isDark) 0.09f else 0.18f)
                val stripe = c.text.copy(alpha = if (c.isDark) 0.022f else 0.05f)
                val step = 26.dp.toPx()
                var y = 1.dp.toPx()
                while (y < size.height) {
                    var x = 1.dp.toPx()
                    while (x < size.width) { drawCircle(dot, 1.dp.toPx(), Offset(x, y)); x += step }
                    y += step
                }
                val gap = 7.dp.toPx()
                var x0 = -size.height
                while (x0 < size.width) {
                    drawLine(stripe, Offset(x0, 0f), Offset(x0 + size.height, size.height), 2.dp.toPx())
                    x0 += gap
                }
                }
                drawWallDecor(decor, c, 96.dp.toPx())
                for (seam in listOf(226.dp, 366.dp)) {
                    val sy = seam.toPx() * ys + statusTop.toPx()
                    drawLine(c.text.copy(alpha = 0.07f), Offset(0f, sy), Offset(size.width, sy), 1.dp.toPx())
                }
            },
        )

        // Middle layer: chalk, rope and holds.
        Box(
            Modifier.fillMaxSize().graphicsLayer { translationY = scroll.value * 0.2f }.drawBehind {
                if (sel >= 0 && !p.plainWall) {
                    val cx = (chalkX + hold / 2).toPx()
                    val cy = (chalkY + hold / 2).toPx()
                    drawCircle(Brush.radialGradient(listOf(c.text.copy(alpha = 0.18f), Color.Transparent), Offset(cx, cy), 58.dp.toPx()), 58.dp.toPx(), Offset(cx, cy))
                }
                if (sel >= 0 && ripple.value > 0f) {
                    val center = Offset((ax(sel) + hold / 2).toPx(), (ay(sel) + hold / 2).toPx())
                    val r = (holdSel / 2).toPx() * (1f + ripple.value * 1.6f)
                    drawCircle(c.accent.copy(alpha = (1f - ripple.value) * 0.8f), r, center, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                }
                val onRope = if (!st.settings.prefs.showRope) emptyList() else days.indices.filter { (st.results[days[it]] ?: DayResult.FUTURE) in ROPE }
                for (k in 1 until onRope.size) {
                    val a = onRope[k - 1]; val b = onRope[k]
                    val from = Offset((ax(a) + hold / 2).toPx(), (ay(a) + hold / 2).toPx())
                    val to = Offset((ax(b) + hold / 2).toPx(), (ay(b) + hold / 2).toPx())
                    // The newest segment can be part-drawn, so the rope reaches the hold before it lands.
                    val t = if (k == onRope.size - 1) ropeDraw.coerceIn(0f, 1f) else 1f
                    if (t <= 0f) continue
                    drawLine(
                        c.accent.copy(alpha = 0.95f),
                        from,
                        from + (to - from) * t,
                        3.5.dp.toPx(), StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 8.dp.toPx())),
                    )
                }
            },
        ) {
            for ((i, day) in days.withIndex()) {
                val r = st.results[day] ?: if (day.isBefore(st.settings.startDate)) DayResult.BEFORE else DayResult.FUTURE
                val isSel = i == sel
                // The selected hold grows, fills cross-fade, and a fresh Send pops like a hold you just stuck.
                val size by animateDpAsState(if (isSel) holdSel else hold, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "holdSize")
                val fill by animateColorAsState(holdFill(r, c), tween(350), label = "holdFill")
                val pop = remember { Animatable(1f) }
                LaunchedEffect(r in popOn) {
                    if (r in popOn && !calm) { pop.snapTo(0.55f); pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium)) }
                }
                val x = ax(i) - (size - hold) / 2
                val y = ay(i) - (size - hold) / 2
                val open = r != DayResult.FUTURE && r != DayResult.BEFORE
                val stroke = when {
                    isSel -> if (day == st.today) c.accent else c.text
                    r == DayResult.TODAY -> c.accent
                    else -> null
                }
                Column(Modifier.offset(x, y).width(size), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Hold(
                        holdShape(modes[day]), fill, size,
                        Modifier
                            .scale(pop.value)
                            .semantics { contentDescription = "${day.format(wdFmt)} ${day.dayOfMonth}, ${resultName(r, v)}" }
                            .then(if (open) Modifier.clickable(onClick = { onPickDay(day) }, role = Role.Button) else Modifier),
                        stroke = stroke, strokeWidth = if (stroke != null) 2.2.dp else 0.dp, mark = r,
                    ) {
                        Text("${day.dayOfMonth}", color = holdNumberColor(r, c), style = cutStyle(if (isSel) 18 else if (p.bigHolds) 16 else 14, 900, 1f, tabular = true))
                    }
                    if (p.weekdayLabels) Text(
                        day.format(wdFmt).uppercase(), color = if (isSel) c.text else c.text2,
                        style = cutStyle(10, 800, 1f, 0.08f), textAlign = TextAlign.Center, modifier = Modifier.width(size),
                    )
                }
            }
        }

        // Front layer: route tag and the week chrome, full speed.
        if (st.settings.prefs.showTape) Row(
            Modifier.offset((-6).dp, 180.dp * ys + statusTop).rotate(-8f).background(c.accent).padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapeStep(CutIcons.Back, "Earlier week", canEarlier) { onWeek(-1) }
            // Numbers the wall on screen (Monday to Sunday) from the route's first wall, so each step reads a new week.
            val wall = java.time.temporal.ChronoUnit.WEEKS.between(ProgramEngine.weekStart(st.settings.startDate), weekStart) + 1
            Text("${v.level(st.level).uppercase()} · WEEK ${wall.coerceAtLeast(1)}", color = c.onAccent, style = cutStyle(12, 900, 1f, 0.08f, tabular = true))
            TapeStep(CutIcons.Forward, "Later week", canLater) { onWeek(1) }
        }
        if (onInfo != null) Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 46.dp).size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .clickable(onClickLabel = "How to read the wall", role = Role.Button, onClick = onInfo),
            contentAlignment = Alignment.Center,
        ) { Icon(CutIcons.Info, contentDescription = "How to read the wall", Modifier.size(20.dp), tint = c.text2) }
    }
}

private val ROPE = setOf(DayResult.SEND, DayResult.MIN, DayResult.FREEZE, DayResult.SKIP, DayResult.TODAY)

/** One arrow on the tape: the week before or after. Faded where there is no week to go to. */
@Composable
private fun TapeStep(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = CutTheme.colors
    Box(Modifier.size(40.dp, 34.dp).pressable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, Modifier.size(12.dp), tint = c.onAccent.copy(alpha = if (enabled) 1f else 0.3f))
    }
}
