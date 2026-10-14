package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.SettingsRow
import com.relentnet.cuttracker.ui.theme.ThemeMode

@Composable
fun SettingsScreen(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit, onBack: () -> Unit) {
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Today", title = "Settings", onBack = onBack)
        Column {
            Eyebrow("Appearance", Modifier.padding(bottom = 8.dp))
            Card {
                SettingsRow("Theme", last = true) {
                    Pill("System", themeMode == ThemeMode.SYSTEM, { onThemeMode(ThemeMode.SYSTEM) })
                    Pill("Dark", themeMode == ThemeMode.DARK, { onThemeMode(ThemeMode.DARK) })
                    Pill("Light", themeMode == ThemeMode.LIGHT, { onThemeMode(ThemeMode.LIGHT) })
                }
            }
        }
        BigButton("Save", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
