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
import androidx.compose.ui.graphics.StrokeCap
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
    // Where the top and bottom of the silhouette sit, per shape, so the hat sits on the hold and the grin inside it.
    val top = when (shape) { HoldShape.JUG -> 2.2f; HoldShape.CRIMP -> 6.5f; HoldShape.SLOPER -> 7.2f; HoldShape.PINCH -> 3.4f }
    val bottom = when (shape) { HoldShape.JUG -> 21.5f; HoldShape.CRIMP -> 17.5f; HoldShape.SLOPER -> 20f; HoldShape.PINCH -> 21f }
    when (decor) {
        Decor.NONE -> Unit
        Decor.PUMPKIN -> {
            // Stem, two eyes under the top edge, and a jagged grin along the bottom. The number sits between the eyes and the grin.
            drawRoundRect(c.goodDim, Offset(10.6f * s, (top - 3f) * s), Size(2.8f * s, 3.6f * s), CornerRadius(1f * s))
            val cut = c.ground
            fun eye(cx: Float) = Path().apply { moveTo((cx - 2f) * s, (top + 5.6f) * s); lineTo((cx + 2f) * s, (top + 5.6f) * s); lineTo(cx * s, (top + 2.4f) * s); close() }
            drawPath(eye(8.2f), cut); drawPath(eye(15.8f), cut)
            val grin = Path().apply {
                val y0 = (bottom - 4.2f) * s; val y1 = (bottom - 1.6f) * s
                moveTo(5.5f * s, y0)
                var x = 5.5f; var up = false
                while (x < 18.5f) { x += 1.6f; lineTo(x * s, if (up) y0 else y1); up = !up }
                lineTo(18.5f * s, y0); lineTo(18.5f * s, y0 - 0.8f * s); lineTo(5.5f * s, y0 - 0.8f * s); close()
            }
            drawPath(grin, cut)
        }
        Decor.SNOW -> {
            val snow = c.text.copy(alpha = 0.92f)
            drawCircle(snow, 3.4f * s, Offset(7.4f * s, (top + 0.6f) * s))
            drawCircle(snow, 4.2f * s, Offset(12.4f * s, (top + 0.1f) * s))
            drawCircle(snow, 3f * s, Offset(17f * s, (top + 0.9f) * s))
        }
        Decor.HEART -> {
            val cx = 17f * s; val cy = (top + 3f) * s; val r = 2.3f * s
            drawCircle(c.accent, r, Offset(cx - r * 0.9f, cy - r * 0.4f))
            drawCircle(c.accent, r, Offset(cx + r * 0.9f, cy - r * 0.4f))
            val tip = Path().apply { moveTo(cx - r * 1.85f, cy); lineTo(cx + r * 1.85f, cy); lineTo(cx, cy + r * 2.2f); close() }
            drawPath(tip, c.accent)
        }
        Decor.STARS -> {
            fun spark(cx: Float, cy: Float, r: Float) {
                val p = Path().apply {
                    moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
                }
                drawPath(p, c.accent)
            }
            spark(18f * s, (top + 2.6f) * s, 4.4f * s)
            spark(6f * s, (top + 1.4f) * s, 2.6f * s)
        }
        Decor.BUNTING -> {
            val cols = listOf(c.accent, c.text, c.freeze)
            for (i in 0..2) {
                val x = (6.5f + i * 5.5f) * s
                val p = Path().apply { moveTo(x - 2.2f * s, (top + 0.4f) * s); lineTo(x + 2.2f * s, (top + 0.4f) * s); lineTo(x, (top + 5.2f) * s); close() }
                drawPath(p, cols[i])
            }
        }
    }
}

/**
 * The wall behind the week joins in: motifs scattered over the panel in the look's accent, faint enough to sit behind
 * the holds. Deterministic jitter so the wall does not twitch on recomposition.
 */
fun DrawScope.drawWallDecor(decor: Decor, c: CutColors, cell: Float) {
    if (decor == Decor.NONE) return
    val ink = c.accent.copy(alpha = if (c.isDark) 0.22f else 0.24f)
    val pale = c.text.copy(alpha = if (c.isDark) 0.16f else 0.22f)
    var row = 0
    var y = cell * 0.5f
    while (y < size.height) {
        var col = 0
        var x = if (row % 2 == 0) cell * 0.5f else cell
        while (x < size.width) {
            val j = ((row * 7 + col * 13) % 5 - 2) * cell * 0.08f
            val cx = x + j; val cy = y - j
            val r = cell * 0.11f
            when (decor) {
                Decor.PUMPKIN -> bat(cx, cy, r * 1.4f, ink)
                Decor.SNOW -> flake(cx, cy, r, pale)
                Decor.HEART -> heart(cx, cy, r * 0.8f, ink)
                Decor.STARS -> spark(cx, cy, r * 1.1f, ink)
                Decor.BUNTING -> star(cx, cy, r, if ((row + col) % 2 == 0) ink else pale)
                Decor.NONE -> Unit
            }
            x += cell; col++
        }
        y += cell * 0.86f; row++
    }
}

private fun DrawScope.bat(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx - 2f * r, cy)
        quadraticTo(cx - 1.4f * r, cy - 1.2f * r, cx - 0.6f * r, cy - 0.5f * r)
        quadraticTo(cx - 0.3f * r, cy - 1.1f * r, cx, cy - 0.7f * r)
        quadraticTo(cx + 0.3f * r, cy - 1.1f * r, cx + 0.6f * r, cy - 0.5f * r)
        quadraticTo(cx + 1.4f * r, cy - 1.2f * r, cx + 2f * r, cy)
        quadraticTo(cx + 1.1f * r, cy + 0.1f, cx + 0.7f * r, cy + 0.7f * r)
        quadraticTo(cx + 0.3f * r, cy + 0.2f * r, cx, cy + 0.6f * r)
        quadraticTo(cx - 0.3f * r, cy + 0.2f * r, cx - 0.7f * r, cy + 0.7f * r)
        quadraticTo(cx - 1.1f * r, cy + 0.1f, cx - 2f * r, cy)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.flake(cx: Float, cy: Float, r: Float, color: Color) {
    for (k in 0 until 3) rotate(k * 60f, Offset(cx, cy)) {
        drawLine(color, Offset(cx, cy - r), Offset(cx, cy + r), r * 0.22f, StrokeCap.Round)
        drawLine(color, Offset(cx - r * 0.35f, cy - r * 0.55f), Offset(cx, cy - r * 0.25f), r * 0.18f, StrokeCap.Round)
        drawLine(color, Offset(cx + r * 0.35f, cy - r * 0.55f), Offset(cx, cy - r * 0.25f), r * 0.18f, StrokeCap.Round)
    }
}

private fun DrawScope.heart(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color, r, Offset(cx - r * 0.9f, cy - r * 0.4f))
    drawCircle(color, r, Offset(cx + r * 0.9f, cy - r * 0.4f))
    val tip = Path().apply { moveTo(cx - r * 1.85f, cy); lineTo(cx + r * 1.85f, cy); lineTo(cx, cy + r * 2.2f); close() }
    drawPath(tip, color)
}

private fun DrawScope.spark(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - r); quadraticTo(cx, cy, cx + r, cy); quadraticTo(cx, cy, cx, cy + r); quadraticTo(cx, cy, cx - r, cy); quadraticTo(cx, cy, cx, cy - r); close()
    }
    drawPath(p, color)
}

private fun DrawScope.star(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 0) r else r * 0.45f
        val a = Math.toRadians((i * 36 - 90).toDouble())
        val px = cx + (rr * Math.cos(a)).toFloat(); val py = cy + (rr * Math.sin(a)).toFloat()
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
    drawPath(p, color)
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
