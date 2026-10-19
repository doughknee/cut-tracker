package com.relentnet.cuttracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.ui.theme.CutColors
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.Decor
import com.relentnet.cuttracker.ui.theme.LocalDecor

/** Hold silhouette = kind of day. Jug is the easy one, crimp the hard one, sloper nothing to grab, pinch no pick. */
enum class HoldShape { JUG, CRIMP, SLOPER, PINCH }

fun holdShape(mode: DayMode?): HoldShape = when (mode) {
    DayMode.OFF -> HoldShape.JUG
    DayMode.CLIMB -> HoldShape.CRIMP
    DayMode.REST -> HoldShape.SLOPER
    null -> HoldShape.PINCH
}

/** Fill = how the day went. */
fun holdFill(r: DayResult, c: CutColors): Color = when (r) {
    DayResult.SEND -> c.good
    DayResult.MIN -> c.goodDim
    DayResult.FREEZE, DayResult.SKIP -> c.freeze
    DayResult.MISSED -> c.miss
    DayResult.TODAY -> c.surface
    DayResult.FUTURE, DayResult.BEFORE -> c.future
}

fun holdNumberColor(r: DayResult, c: CutColors): Color = when (r) {
    DayResult.SEND, DayResult.FREEZE, DayResult.SKIP -> c.ground
    DayResult.MIN, DayResult.TODAY -> c.text
    else -> c.text2
}

/** Draws [shape] into the whole DrawScope, 24-unit paths from the mockups scaled to fit. */
fun DrawScope.drawHold(shape: HoldShape, fill: Color, stroke: Color? = null, strokeWidth: Float = 0f) {
    val s = size.minDimension / 24f
    val outline = if (stroke != null && strokeWidth > 0f) Stroke(strokeWidth, join = StrokeJoin.Round) else null
    when (shape) {
        HoldShape.JUG -> {
            val pts = listOf(12f to 1f, 20.6f to 5.14f, 22.72f to 14.45f, 16.77f to 21.91f, 7.23f to 21.91f, 1.28f to 14.45f, 3.4f to 5.14f)
            val p = Path().apply {
                moveTo(pts[0].first * s, pts[0].second * s)
                for ((x, y) in pts.drop(1)) lineTo(x * s, y * s)
                close()
            }
            drawPath(p, fill)
            if (outline != null) drawPath(p, stroke!!, style = outline)
        }
        HoldShape.CRIMP -> rotate(-10f) {
            val tl = Offset(1.5f * s, 6f * s)
            val sz = Size(21f * s, 12f * s)
            drawRoundRect(fill, tl, sz, CornerRadius(6f * s))
            if (outline != null) drawRoundRect(stroke!!, tl, sz, CornerRadius(6f * s), style = outline)
        }
        HoldShape.SLOPER -> {
            // Tall dome so a two-digit number fits inside.
            val p = Path().apply {
                moveTo(1.5f * s, 20f * s)
                arcTo(Rect(1.5f * s, 6f * s, 22.5f * s, 34f * s), 180f, 180f, false)
                close()
            }
            drawPath(p, fill)
            if (outline != null) drawPath(p, stroke!!, style = outline)
        }
        HoldShape.PINCH -> rotate(16f) {
            val tl = Offset(4.5f * s, 1.5f * s)
            val sz = Size(15f * s, 21f * s)
            drawOval(fill, tl, sz)
            if (outline != null) drawOval(stroke!!, tl, sz, style = outline)
        }
    }
}

/**
 * The holiday hat: drawn along the top edge of the hold so a day number in the middle stays readable. Colors come from
 * the look, never from here, so the hat reads in both dark and light.
 */
fun DrawScope.drawDecor(decor: Decor, shape: HoldShape, fill: Color, c: CutColors) {
    val s = size.minDimension / 24f
    // Where the top of the silhouette sits, per shape, so the hat sits on the hold and not above it.
    val top = when (shape) { HoldShape.JUG -> 2.2f; HoldShape.CRIMP -> 6.5f; HoldShape.SLOPER -> 7.2f; HoldShape.PINCH -> 3.4f }
    when (decor) {
        Decor.NONE -> Unit
        Decor.PUMPKIN -> {
            // A stem, and two eyes cut into the hold just under the top edge.
            drawRoundRect(c.goodDim, Offset(10.9f * s, (top - 2.2f) * s), Size(2.2f * s, 2.8f * s), CornerRadius(0.8f * s))
            val eye = Path().apply { moveTo(7.2f * s, (top + 5.2f) * s); lineTo(9.8f * s, (top + 5.2f) * s); lineTo(8.5f * s, (top + 2.9f) * s); close() }
            val eye2 = Path().apply { moveTo(14.2f * s, (top + 5.2f) * s); lineTo(16.8f * s, (top + 5.2f) * s); lineTo(15.5f * s, (top + 2.9f) * s); close() }
            drawPath(eye, c.ground); drawPath(eye2, c.ground)
        }
        Decor.SNOW -> {
            // Three soft blobs along the top edge.
            val snow = c.text.copy(alpha = 0.9f)
            drawCircle(snow, 2.6f * s, Offset(8f * s, (top + 0.6f) * s))
            drawCircle(snow, 3.2f * s, Offset(12.4f * s, (top + 0.2f) * s))
            drawCircle(snow, 2.3f * s, Offset(16.4f * s, (top + 0.9f) * s))
        }
        Decor.HEART -> {
            // A small heart at the top right.
            val cx = 17.2f * s; val cy = (top + 2.2f) * s; val r = 1.6f * s
            drawCircle(c.accent, r, Offset(cx - r * 0.9f, cy - r * 0.4f))
            drawCircle(c.accent, r, Offset(cx + r * 0.9f, cy - r * 0.4f))
            val tip = Path().apply { moveTo(cx - r * 1.8f, cy); lineTo(cx + r * 1.8f, cy); lineTo(cx, cy + r * 2.1f); close() }
            drawPath(tip, c.accent)
        }
        Decor.STARS -> {
            // A four-point spark at the top right and a smaller one at the top left.
            fun spark(cx: Float, cy: Float, r: Float) {
                val p = Path().apply {
                    moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
                }
                drawPath(p, c.accent)
            }
            spark(17.6f * s, (top + 2.4f) * s, 3.2f * s)
            spark(6.4f * s, (top + 1.6f) * s, 1.9f * s)
        }
        Decor.BUNTING -> {
            // Three little flags hung from the top edge.
            val cols = listOf(c.accent, c.text, c.freeze)
            for (i in 0..2) {
                val x = (7f + i * 5f) * s
                val p = Path().apply { moveTo(x - 1.6f * s, (top + 0.6f) * s); lineTo(x + 1.6f * s, (top + 0.6f) * s); lineTo(x, (top + 4.2f) * s); close() }
                drawPath(p, cols[i])
            }
        }
    }
}

/** A hold with optional centered content (the day number). Wears the current look's decor. */
@Composable
fun Hold(
    shape: HoldShape,
    fill: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    stroke: Color? = null,
    strokeWidth: Dp = 0.dp,
    content: @Composable () -> Unit = {},
) {
    val decor = LocalDecor.current
    val c = CutTheme.colors
    Box(
        modifier.size(size).drawBehind { drawHold(shape, fill, stroke, strokeWidth.toPx()); if (decor != Decor.NONE) drawDecor(decor, shape, fill, c) },
        contentAlignment = Alignment.Center,
    ) { content() }
}
