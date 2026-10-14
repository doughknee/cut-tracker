package com.relentnet.cuttracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.components.BackHeader
import com.relentnet.cuttracker.ui.components.BigButton
import com.relentnet.cuttracker.ui.components.Card
import com.relentnet.cuttracker.ui.components.Eyebrow
import com.relentnet.cuttracker.ui.components.Pill
import com.relentnet.cuttracker.ui.components.Screen
import com.relentnet.cuttracker.ui.components.SettingsRow
import com.relentnet.cuttracker.ui.components.Switch

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val st by vm.state.collectAsStateWithLifecycle()
    val s = st.settings
    Screen(bottomInset = true) {
        BackHeader(backLabel = "Today", title = "Settings", onBack = onBack)
        Column {
            Eyebrow("Appearance", Modifier.padding(bottom = 8.dp))
            Card {
                SettingsRow("Theme") {
                    Pill("System", s.theme == "system", { vm.saveSettings(s.copy(theme = "system")) })
                    Pill("Dark", s.theme == "dark", { vm.saveSettings(s.copy(theme = "dark")) })
                    Pill("Light", s.theme == "light", { vm.saveSettings(s.copy(theme = "light")) })
                }
                SettingsRow("Haptics and confetti", last = true) {
                    Switch(s.haptics) { vm.saveSettings(s.copy(haptics = !s.haptics)) }
                }
            }
        }
        BigButton("Save", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
