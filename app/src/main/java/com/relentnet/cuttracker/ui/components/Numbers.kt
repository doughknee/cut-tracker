package com.relentnet.cuttracker.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State

/** Numbers never swap, they count: 350ms ease-out from the old value to the new one. */
@Composable
fun tickInt(target: Int): State<Int> = animateIntAsState(target, tween(350, easing = EaseOutCubic), label = "tick")

@Composable
fun tickFloat(target: Float): State<Float> = animateFloatAsState(target, tween(350, easing = EaseOutCubic), label = "tickf")

@Composable
fun tickedInt(target: Int): Int { val v by tickInt(target); return v }

@Composable
fun tickedFloat(target: Float): Float { val v by tickFloat(target); return v }
