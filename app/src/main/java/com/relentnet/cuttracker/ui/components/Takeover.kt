package com.relentnet.cuttracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.Takeover
import com.relentnet.cuttracker.ui.theme.ArchivoTakeover
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.TNUM
import com.relentnet.cuttracker.ui.theme.cutStyle
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Full-screen Send / Level up takeover. Covers everything including the nav. */
@Composable
fun TakeoverOverlay(t: Takeover, confettiOn: Boolean, onDismiss: () -> Unit) {
    val c = CutTheme.colors
    val haptics = LocalHaptics.current
    val slam = remember(t) { Animatable(1f) }

    LaunchedEffect(t) {
        when (t) {
            is Takeover.Send -> {
                haptics.pattern(0, 30, 40, 60)
                slam.snapTo(1.4f)
                slam.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
                haptics.heavy()
            }
            is Takeover.LevelUp -> {
                haptics.pattern(0, 40, 60, 80)
                delay(1500)
                onDismiss()
            }
        }
    }

    androidx.activity.compose.BackHandler { onDismiss() }
    // Rises from the wall rather than cutting in.
    var up by remember(t) { mutableStateOf(false) }
    LaunchedEffect(t) { up = true }
    val rise by animateFloatAsState(if (up) 0f else 1f, spring(dampingRatio = 0.82f, stiffness = 380f), label = "rise")
    val veil by animateFloatAsState(if (up) 1f else 0f, tween(200), label = "veil")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = veil; translationY = rise * size.height * 0.12f }
            .background(c.ground)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (t is Takeover.LevelUp) onDismiss() },
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Eyebrow(if (t is Takeover.Send) "Send" else "Level up", color = c.accent)
            Text(
                if (t is Takeover.Send) "Sent" else "V${t.levelOrZero()}",
                color = c.accent,
                style = cutStyle(if (t is Takeover.Send) 96 else 120, 900, 0.9f, -0.05f).copy(fontFamily = ArchivoTakeover),
                textAlign = TextAlign.Center, modifier = Modifier.scale(slam.value),
            )
            Eyebrow(if (t is Takeover.Send) "+${ProgramSpec.XP_SEND} xp" else "New grade")
            Text(
                if (t is Takeover.Send) "Every item ticked. Today's hold just went green." else "New grade. The bar starts over, the chain does not.",
                color = c.text2, style = cutStyle(13, 500, 1.4f), textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp),
            )
            BigButton("Nice", onClick = onDismiss, modifier = Modifier.widthIn(max = 220.dp).padding(top = 10.dp).width(220.dp))
        }
        if (confettiOn) Confetti(colors = listOf(c.accent, c.good))
    }
}

private fun Takeover.levelOrZero() = (this as? Takeover.LevelUp)?.level ?: 0


private class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, val s: Float, val color: Color, var a: Float, val va: Float)

/** Accent and success confetti launched upward from the nav bar; about 1.2s. */
@Composable
fun Confetti(colors: List<Color>, durationFrames: Int = 70) {
    var frame by remember { mutableIntStateOf(0) }
    var size by remember { mutableStateOf(Size.Zero) }
    val particles = remember { mutableListOf<Particle>() }
    LaunchedEffect(size) {
        if (size == Size.Zero) return@LaunchedEffect
        particles.clear()
        val d = size.width / 390f
        repeat(110) { i ->
            particles += Particle(
                x = size.width / 2 + (Random.nextFloat() - 0.5f) * 80 * d, y = size.height - 70 * d,
                vx = (Random.nextFloat() - 0.5f) * 9 * d, vy = (-9 - Random.nextFloat() * 8) * d,
                s = (4 + Random.nextFloat() * 5) * d, color = colors[i % colors.size],
                a = Random.nextFloat() * 6, va = (Random.nextFloat() - 0.5f) * 0.3f,
            )
        }
        frame = 0
        while (frame < durationFrames) {
            withFrameNanos { }
            for (p in particles) { p.x += p.vx; p.y += p.vy; p.vy += 0.35f * d; p.a += p.va }
            frame++
        }
    }
    Canvas(Modifier.fillMaxSize().onSizeChanged { size = it.toSize() }) {
        if (frame >= durationFrames) return@Canvas
        for (p in particles) {
            rotate(p.a * 57.3f, Offset(p.x, p.y)) {
                drawRect(p.color, Offset(p.x - p.s / 2, p.y - p.s * 0.3f), Size(p.s, p.s * 0.6f))
            }
        }
    }
}

/** The prototype's toast pill: text-colored background, bottom of the screen, with an optional [action] like Undo. */
@Composable
fun ToastPill(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val c = CutTheme.colors
    androidx.compose.foundation.layout.Row(
        modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(999.dp)).background(c.text).padding(start = 18.dp, end = if (action != null) 6.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = c.ground, style = cutStyle(13, 700, 1.2f), maxLines = 2, modifier = Modifier.padding(vertical = 10.dp).weight(1f, fill = false))
        if (action != null) Text(
            action.uppercase(), color = c.accent, style = cutStyle(12, 800, 1f, 0.08f),
            modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(999.dp)).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onAction).padding(horizontal = 12.dp, vertical = 14.dp),
        )
    }
}
