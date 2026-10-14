package com.relentnet.cuttracker.ui.lift

import androidx.compose.runtime.Composable
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.Header
import com.relentnet.cuttracker.ui.components.Screen

@Composable
fun LiftScreen(vm: AppViewModel) {
    Screen { Header(eyebrow = "Block · Week 1", title = "Lift") }
}
