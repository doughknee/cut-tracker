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
    DayResult.FREEZE -> c.freeze
    DayResult.MISSED -> c.miss
    DayResult.TODAY -> c.surface
    DayResult.FUTURE, DayResult.BEFORE -> c.future
}

fun holdNumberColor(r: DayResult, c: CutColors): Color = when (r) {
    DayResult.SEND, DayResult.FREEZE -> c.ground
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

/** A hold with optional centered content (the day number). */
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
    Box(
        modifier.size(size).drawBehind { drawHold(shape, fill, stroke, strokeWidth.toPx()) },
        contentAlignment = Alignment.Center,
    ) { content() }
}
