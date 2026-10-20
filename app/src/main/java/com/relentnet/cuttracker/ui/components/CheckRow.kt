package com.relentnet.cuttracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** The 26dp check box: accent fill with a 150ms spring pop when it turns on. */
@Composable
fun CheckBox(done: Boolean, size: Int = 26, iconSize: Int = 14) {
    val c = CutTheme.colors
    val scale = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done) {
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val bg by animateColorAsState(if (done) c.accent else Color.Transparent, tween(150), label = "box")
    val bd by animateColorAsState(if (done) c.accent else c.mutedBorder, tween(150), label = "boxborder")
    val shape = RoundedCornerShape((size * 0.27f).dp)
    Box(
        Modifier.size(size.dp).scale(scale.value).clip(shape).background(bg, shape).border(2.dp, bd, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(CutIcons.Check, null, Modifier.size(iconSize.dp), tint = c.onAccent)
    }
}

/**
 * A script row. Swipes right over a green "Done" underlay (threshold 80dp, travel 140dp) when [onSwipeCheck] is set;
 * tapping calls [onClick]. [next] draws the accent "Up next" eyebrow and the stronger border.
 */
@Composable
fun CheckRow(
    label: String,
    sub: String?,
    time: String?,
    xp: Int,
    done: Boolean,
    next: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
    onSwipeCheck: (() -> Unit)? = null,
    tag: String? = null,
    /** When set, the box ticks the row on its own while the row body still calls [onClick] (a picker, an inline panel). */
    onCheck: (() -> Unit)? = null,
) {
    val c = CutTheme.colors
    val density = LocalDensity.current
    val threshold = with(density) { 80.dp.toPx() }
    val maxTravel = with(density) { 140.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var swiped by remember { mutableFloatStateOf(0f) }
    val swipeCb by androidx.compose.runtime.rememberUpdatedState(onSwipeCheck)
    val shape = RoundedCornerShape(10.dp)

    Box(Modifier.fillMaxWidth().clip(shape)) {
        if (onSwipeCheck != null && !done) {
            Box(
                Modifier.matchParentSize().alpha((offset.value / threshold).coerceIn(0f, 1f)).background(c.good),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text("DONE", Modifier.padding(start = 18.dp), color = c.ground, style = cutStyle(12, 800, 1f, 0.1f))
            }
        }
        Row(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .then(
                    if (onSwipeCheck != null && !done) Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val past = offset.value > threshold
                                scope.launch {
                                    if (past) { swipeCb?.invoke(); offset.snapTo(0f) } else offset.animateTo(0f, tween(200))
                                }
                            },
                            onDragCancel = { scope.launch { offset.animateTo(0f, tween(200)) } },
                        ) { change, dx ->
                            change.consume()
                            scope.launch { offset.snapTo((offset.value + dx).coerceIn(0f, maxTravel)) }
                        }
                    } else Modifier,
                )
                .clip(shape)
                .background(if (done) c.surface.copy(alpha = if (compact) 0f else 0.7f) else c.surface, shape)
                .then(
                    // Up next is the raised row: accent edge. The rest sit flat with no edge at all.
                    if (compact || !next) Modifier
                    else Modifier.border(1.5.dp, c.accent.copy(alpha = 0.6f), shape),
                )
                .clickable(onClick = onClick, role = Role.Checkbox)
                .semantics { stateDescription = if (done) "Done" else "Not done" }
                .heightIn(min = if (compact) 40.dp else 56.dp)
                .padding(start = if (compact) 12.dp else 7.dp, end = if (compact) 12.dp else 14.dp, top = if (compact) 6.dp else 8.dp, bottom = if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            // A 40dp hit area around the box, so the box is a target of its own: it ticks, or on a ticked row unticks.
            Box(
                Modifier.size(if (compact) 32.dp else 40.dp).clip(RoundedCornerShape(12.dp))
                    .then(if (onCheck != null) Modifier.clickable(onClick = onCheck, onClickLabel = if (done) "Untick" else "Tick", role = Role.Checkbox) else Modifier),
                contentAlignment = Alignment.Center,
            ) { if (compact) CheckBox(done, size = 20, iconSize = 11) else CheckBox(done) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (next && !done) Text("UP NEXT", color = c.accent, style = cutStyle(10, 800, 1f, 0.1f))
                Text(
                    label,
                    color = if (done) c.text2 else c.text,
                    style = cutStyle(if (compact) 14 else 16, 600, 1.2f),
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
                if (!compact && sub != null) Text(sub, color = c.text2, style = cutStyle(12, 500, 1.3f))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (!compact && tag != null) Text(tag.uppercase(), color = c.good, style = cutStyle(10, 800, 1f, 0.08f))
                else if (!compact && time != null) Text(time, color = c.text2, style = cutStyle(12, 700, 1f, tabular = true))
                val xpColor by animateColorAsState(if (done) c.good else c.dim, tween(200), label = "xp")
                Text("+$xp", color = xpColor, style = cutStyle(11, 800, 1f, tabular = true))
            }
        }
    }
}
