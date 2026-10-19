package com.relentnet.cuttracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.relentnet.cuttracker.notify.Reminders
import com.relentnet.cuttracker.ui.App
import com.relentnet.cuttracker.ui.AppViewModel
import com.relentnet.cuttracker.ui.Haptics
import com.relentnet.cuttracker.ui.LocalHaptics
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels { AppViewModel.factory(App.of(this).repo) }
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var asked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val haptics = Haptics(this) { vm.state.value.settings.haptics }
        handleIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(Reminders.EXTRA_TAB)?.let { vm.requestTab(it) }
        intent?.getStringExtra(EXTRA_DATE)?.let { d ->
            runCatching { java.time.LocalDate.parse(d) }.getOrNull()?.let { vm.viewDate = it; vm.requestTab(com.relentnet.cuttracker.ui.Routes.TODAY) }
        }
    }

    companion object {
        /** Intent extra from the widget: ISO date of the hold that was tapped. */
        const val EXTRA_DATE = "date"
    }

    override fun onResume() {
        super.onResume()
        vm.opened()
        if (Build.VERSION.SDK_INT >= 33 && !asked && vm.state.value.settings.onboarded &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            asked = true
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
