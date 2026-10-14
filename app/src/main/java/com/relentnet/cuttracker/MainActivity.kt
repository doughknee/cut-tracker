package com.relentnet.cuttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.ui.App
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.Haptics
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels { AppViewModel.factory(App.of(this).repo) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val haptics = Haptics(this) { vm.state.value.settings.haptics }
        setContent {
            val st by vm.state.collectAsStateWithLifecycle()
            val mode = when (st.settings.theme) {
                "dark" -> ThemeMode.DARK
                "light" -> ThemeMode.LIGHT
                else -> ThemeMode.SYSTEM
            }
            CutTheme(mode) {
                CompositionLocalProvider(LocalHaptics provides remember { haptics }) { App(vm) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.opened()
    }
}
