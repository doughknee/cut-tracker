package com.relentnet.cuttracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.relentnet.cuttracker.ui.LocalHaptics

/**
 * The one press feel for the app: sinks to 98% while held, springs back on release, ticks the haptic. Use it in place
 * of clickable on anything card-sized; small controls keep the plain ripple.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    haptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "press")
    val haptics = LocalHaptics.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role) {
            if (haptic) haptics.tick()
            onClick()
        }
}
