package com.relentnet.cuttracker.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Header
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme

@Composable
fun TodayScreen(onSettings: () -> Unit) {
    val c = CutTheme.colors
    Screen {
        Header(eyebrow = "Wed Oct 14 · Day 1 of 84", title = "Pick a day") {
            Row(
                Modifier.clickable(onClick = onSettings, role = Role.Button),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Eyebrow("Phase 1 Cut", color = c.accent)
                Icon(CutIcons.Gear, contentDescription = "Open settings", Modifier.size(16.dp), tint = c.accent)
            }
        }
    }
}
