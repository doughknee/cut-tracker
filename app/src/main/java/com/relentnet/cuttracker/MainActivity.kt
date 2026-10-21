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
import com.relentnet.cuttracker.data.vocab
import androidx.compose.ui.unit.dp

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
            val p = st.settings.prefs
            val palette = Palettes.of(st.settings, now.toLocalDate()).let { if (p.wallDecor) it else it.copy(decor = com.relentnet.cuttracker.ui.theme.Decor.NONE) }
            // The phone's "Remove animations" switch counts as calm unless Settings says always.
            val phoneCalm = remember { android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            val density = androidx.compose.ui.platform.LocalDensity.current
            com.relentnet.cuttracker.ui.theme.boldBump = if (p.boldText) 200 else 0
            // Block screenshots (Settings, Data): the phone blanks the app in Recents and refuses captures.
            androidx.compose.runtime.LaunchedEffect(p.secureScreen) {
                if (p.secureScreen) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
            CutTheme(mode, palette, now.toLocalTime(), highContrast = p.highContrast) {
                CompositionLocalProvider(
                    LocalHaptics provides remember { haptics },
                    com.relentnet.cuttracker.ui.theme.LocalVocab provides st.settings.vocab,
                    com.relentnet.cuttracker.ui.theme.LocalPrefs provides p,
                    com.relentnet.cuttracker.ui.theme.LocalCalm provides (p.reduceMotion == "on" || phoneCalm),
                    // Text size on top of the phone's own.
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, density.fontScale * p.textScale),
                    // Bigger tap targets: every control that asks for the minimum gets 56dp instead of 48.
                    androidx.compose.material3.LocalMinimumInteractiveComponentSize provides if (p.bigTargets) 56.dp else 48.dp,
                ) { App(vm) }
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
        // Onboarding asks at its reminder step. This catches a phone that was never asked (an update, a restore), only
        // while a reminder is on, and never after a no: that answer stands until You > Nudges.
        val s = vm.state.value.settings
        if (Build.VERSION.SDK_INT >= 33 && !asked && s.onboarded && (s.weighReminder || s.pickReminder) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            asked = true
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
