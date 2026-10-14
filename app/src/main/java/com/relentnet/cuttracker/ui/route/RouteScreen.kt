package com.relentnet.cuttracker.ui.route

import androidx.compose.runtime.Composable
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.Header
import com.relentnet.cuttracker.ui.components.Screen

@Composable
fun RouteScreen(vm: AppViewModel) {
    Screen { Header(eyebrow = "Day 1 of 84 · 1%", title = "The Route") }
}
