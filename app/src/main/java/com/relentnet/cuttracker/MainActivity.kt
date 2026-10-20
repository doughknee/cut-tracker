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
import com.relentnet.cuttracker.ui.theme.Palettes
import androidx.compose.runtime.produceState

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels { AppViewModel.factory(App.of(this).repo) }
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var asked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val haptics = Haptics(this) { vm.state.value.settings.haptics }
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val st by vm.state.collectAsStateWithLifecycle()
            val mode = ThemeMode.of(st.settings.theme)
            // A minute tick so a scheduled dark and a calendar look change while the app is open.
            val now by produceState(java.time.LocalDateTime.now()) { while (true) { kotlinx.coroutines.delay(60_000); value = java.time.LocalDateTime.now() } }
            val palette = Palettes.of(st.settings, now.toLocalDate())
            CutTheme(mode, palette, now.toLocalTime()) {
                CompositionLocalProvider(LocalHaptics provides remember { haptics }) { App(vm) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // adb shell am start -n com.relentnet.cuttracker/.MainActivity --ez seed true: a sample week, debug builds only.
        val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent?.getBooleanExtra("seed", false) == true) { vm.seedSample(); return }
        if (intent?.action == Intent.ACTION_SEND) {
            // Shared text or link: Beta > Meals opens New meal with it. The subject is a browser's page title, a fallback name.
            val text = listOfNotNull(intent.getStringExtra(Intent.EXTRA_TEXT), intent.getStringExtra(Intent.EXTRA_SUBJECT)).joinToString("\n").trim()
            if (text.isNotEmpty()) { vm.pendingShare = text; vm.requestTab(com.relentnet.cuttracker.ui.Routes.BETA_MEALS) }
            return
        }
        intent?.getStringExtra(Reminders.EXTRA_TAB)?.let { vm.requestTab(it) }
        // The weigh-in field lives on Today, so the flag lands there from any page (the widget sends no tab).
        if (intent?.getBooleanExtra(Reminders.EXTRA_WEIGH, false) == true) { vm.pendingWeigh = true; vm.requestTab(com.relentnet.cuttracker.ui.Routes.TODAY) }
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
