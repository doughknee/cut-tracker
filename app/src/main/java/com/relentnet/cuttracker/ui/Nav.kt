package com.relentnet.cuttracker.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.relentnet.cuttracker.ui.components.TakeoverOverlay
import com.relentnet.cuttracker.ui.components.ToastPill
import com.relentnet.cuttracker.ui.route.CameraScreen
import com.relentnet.cuttracker.ui.route.CheckpointsScreen
import com.relentnet.cuttracker.ui.route.RouteScreen
import com.relentnet.cuttracker.ui.beta.BetaScreen
import com.relentnet.cuttracker.ui.onboarding.OnboardingScreen
import com.relentnet.cuttracker.ui.settings.CheckpointsEditor
import com.relentnet.cuttracker.ui.settings.SettingsScreen
import com.relentnet.cuttracker.ui.theme.CutIcons
import com.relentnet.cuttracker.ui.theme.CutTheme
import com.relentnet.cuttracker.ui.theme.cutStyle
import com.relentnet.cuttracker.ui.today.TodayScreen
import com.relentnet.cuttracker.ui.weight.WeightScreen

object Routes {
    const val TODAY = "today"
    const val WEIGHT = "weight"
    const val BETA = "beta"
    const val ROUTE = "route"
    const val SETTINGS = "settings"
    const val CHECKPOINTS = "checkpoints"
    const val CAMERA = "camera"
    const val ONBOARDING = "onboarding"
    const val CHECKPOINT_EDITOR = "settings/checkpoints"
    val tabs = listOf(TODAY, BETA, ROUTE)
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.TODAY, "Today", CutIcons.Today),
    Tab(Routes.BETA, "Beta", CutIcons.Beta),
    Tab(Routes.ROUTE, "Route", CutIcons.Route),
)

@Composable
fun App(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val st by vm.state.collectAsStateWithLifecycle()
    val c = CutTheme.colors
    if (!st.loaded) { Box(Modifier.fillMaxSize().background(c.ground)); return }
    LaunchedEffect(vm.pendingTab, st.settings.onboarded) {
        val t = vm.pendingTab
        if (t != null && st.settings.onboarded) { vm.pendingTab = null; if (t == Routes.WEIGHT) nav.navigate(Routes.WEIGHT) else nav.goTab(t) }
    }
    LaunchedEffect(st.settings.onboarded) {
        if (!st.settings.onboarded && route != null && route != Routes.ONBOARDING) nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
    }
    Box(Modifier.fillMaxSize().background(c.ground)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavHost(nav, startDestination = if (st.settings.onboarded) Routes.TODAY else Routes.ONBOARDING) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(vm, onDone = { nav.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
                    }
                    composable(Routes.TODAY) {
                        TodayScreen(vm, onSettings = { nav.navigate(Routes.SETTINGS) }, onWeight = { nav.navigate(Routes.WEIGHT) })
                    }
                    composable(Routes.WEIGHT) { WeightScreen(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.BETA) { BetaScreen(vm) }
                    composable(Routes.ROUTE) { RouteScreen(vm, onCheckpoints = { nav.navigate(Routes.CHECKPOINTS) }, onEditDay = { vm.viewDate = it; nav.goTab(Routes.TODAY) }) }
                    composable(Routes.CHECKPOINTS) { CheckpointsScreen(vm, onBack = { nav.popBackStack() }, onShoot = { nav.navigate(Routes.CAMERA) }) }
                    composable(Routes.CAMERA) { CameraScreen(vm, onDone = { nav.popBackStack() }) }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(vm, onBack = { nav.popBackStack() }, onCheckpoints = { nav.navigate(Routes.CHECKPOINT_EDITOR) })
                    }
                    composable(Routes.CHECKPOINT_EDITOR) { CheckpointsEditor(vm, onBack = { nav.popBackStack() }) }
                }
            }
            if (route in Routes.tabs) {
                val total = st.items.size
                SendBar(progress = if (total == 0) 0f else st.totals.done.toFloat() / total, full = st.isSend)
                BottomNav(current = route, onGo = { nav.goTab(it) })
            }
        }
        vm.toast?.let { ToastPill(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp).windowInsetsPadding(WindowInsets.navigationBars)) }
        vm.takeover?.let { TakeoverOverlay(it, confettiOn = st.settings.haptics, onDismiss = { vm.dismissTakeover() }) }
    }
}

fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** The 3dp Send progress bar that sits above the nav on every tab. */
@Composable
fun SendBar(progress: Float, full: Boolean) {
    val c = CutTheme.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(400), label = "send")
    Box(Modifier.fillMaxWidth().height(3.dp).background(c.hairline)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).background(if (full) c.good else c.accent))
    }
}

@Composable
fun BottomNav(current: String?, onGo: (String) -> Unit) {
    val c = CutTheme.colors
    Column(Modifier.fillMaxWidth().background(c.ground)) {
        Row(Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
            for (t in TABS) {
                val on = t.route == current
                val tint = if (on) c.accent else c.text2
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null,
                            role = Role.Tab, onClick = { onGo(t.route) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(t.icon, contentDescription = t.label, Modifier.size(22.dp), tint = tint)
                    Text(t.label.uppercase(), color = tint, style = cutStyle(11, 700, 1f, 0.06f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
    }
}
