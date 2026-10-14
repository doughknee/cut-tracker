package com.relentnet.cuttracker.ui.weight

import androidx.compose.runtime.Composable
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.Header
import com.relentnet.cuttracker.ui.components.Screen

@Composable
fun WeightScreen(vm: AppViewModel) {
    Screen { Header(eyebrow = "Weigh-in · Week 1", title = "Weight") }
}
