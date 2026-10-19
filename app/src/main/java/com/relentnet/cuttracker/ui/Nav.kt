package com.relentnet.cuttracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.relentnet.cuttracker.ui.components.Hold
import com.relentnet.cuttracker.ui.components.HoldShape
import kotlinx.coroutines.delay
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
import com.relentnet.cuttracker.ui.beta.LiftsPage
import com.relentnet.cuttracker.ui.beta.MealsPage
import com.relentnet.cuttracker.ui.onboarding.OnboardingScreen
import com.relentnet.cuttracker.ui.settings.SettingsScreen
import com.relentnet.cuttracker.ui.settings.SettingsSectionScreen
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
    const val BETA_MEALS = "beta/meals"
    const val BETA_LIFTS = "beta/lifts"
    const val SETTINGS_SECTION = "settings/{section}"
    fun settingsSection(key: String) = "settings/$key"
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
    if (!st.loaded) { Splash(false); return }
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
                NavHost(
                    nav, startDestination = if (st.settings.onboarded) Routes.TODAY else Routes.ONBOARDING,
                    enterTransition = { enter(initialState.destination.route, targetState.destination.route) },
                    exitTransition = { exit(initialState.destination.route, targetState.destination.route) },
                    popEnterTransition = { popEnter(initialState.destination.route, targetState.destination.route) },
                    popExitTransition = { popExit(initialState.destination.route, targetState.destination.route) },
                    // The back gesture seeks these two, not the pop transitions above; without them it plays the library default.
                    predictivePopEnterTransition = { popEnter(initialState.destination.route, targetState.destination.route) },
                    predictivePopExitTransition = { popExit(initialState.destination.route, targetState.destination.route) },
                ) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(vm, onDone = { nav.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
                    }
                    composable(Routes.TODAY) {
                        TodayScreen(vm, onSettings = { nav.navigate(Routes.SETTINGS) }, onWeight = { nav.navigate(Routes.WEIGHT) })
                    }
                    composable(Routes.WEIGHT) { WeightScreen(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.BETA) {
                        BetaScreen(vm, onMeals = { nav.navigate(Routes.BETA_MEALS) }, onLifts = { nav.navigate(Routes.BETA_LIFTS) }, onCheckpoints = { nav.navigate(Routes.CHECKPOINTS) })
                    }
                    composable(Routes.BETA_MEALS) { MealsPage(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.BETA_LIFTS) { LiftsPage(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.SETTINGS_SECTION) { entry ->
                        SettingsSectionScreen(vm, section = entry.arguments?.getString("section") ?: "program", onBack = { nav.popBackStack() })
                    }
                    composable(Routes.ROUTE) { RouteScreen(vm, onCheckpoints = { nav.navigate(Routes.CHECKPOINTS) }, onEditDay = { vm.viewDate = it; nav.goTab(Routes.TODAY) }) }
                    composable(Routes.CHECKPOINTS) { CheckpointsScreen(vm, onBack = { nav.popBackStack() }, onShoot = { nav.navigate(Routes.CAMERA) }) }
                    composable(Routes.CAMERA) { CameraScreen(vm, onDone = { nav.popBackStack() }) }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(vm, onBack = { nav.popBackStack() }, onSection = { nav.navigate(Routes.settingsSection(it)) })
                    }
                }
            }
            if (route in Routes.tabs) {
                val total = st.items.size
                SendBar(progress = if (total == 0) 0f else st.totals.done.toFloat() / total, full = st.isSend)
                BottomNav(current = route, onGo = { nav.goTab(it) })
            }
        }
        Splash(true)
        vm.toast?.let { ToastPill(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp).windowInsetsPadding(WindowInsets.navigationBars)) }
        vm.takeover?.let { TakeoverOverlay(it, confettiOn = st.settings.haptics, onDismiss = { vm.dismissTakeover() }) }
    }
}

// ---- Motion. Tabs slide sideways in the direction you moved; pushed pages slide up from the right and settle on a
// spring; the Weight sheet rises from the bottom. Pops reverse. Nothing fades in from nowhere.

private val slideSpec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 420f, visibilityThreshold = IntOffset.VisibilityThreshold)
private val fadeSpec = tween<Float>(180)

private fun tabIndex(route: String?) = Routes.tabs.indexOf(route)

private fun enter(from: String?, to: String?): EnterTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> slideInHorizontally(slideSpec) { if (b > a) it / 2 else -it / 2 } + fadeIn(fadeSpec)
        to == Routes.WEIGHT -> slideInVertically(slideSpec) { it / 3 } + fadeIn(fadeSpec)
        else -> slideInHorizontally(slideSpec) { it / 3 } + fadeIn(fadeSpec)
    }
}

private fun exit(from: String?, to: String?): ExitTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> slideOutHorizontally(slideSpec) { if (b > a) -it / 4 else it / 4 } + fadeOut(fadeSpec)
        else -> slideOutHorizontally(slideSpec) { -it / 4 } + fadeOut(fadeSpec)
    }
}

// Pops mirror the pushes. The back gesture seeks these, so they are fixed-duration slides with no fade on the page
// that leaves: a spring plus a short fade reads as a fade when scrubbed, because the fade finishes long before the slide.
private val popSpec = tween<IntOffset>(320, easing = FastOutSlowInEasing)

private fun popEnter(from: String?, to: String?): EnterTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> slideInHorizontally(popSpec) { if (b > a) it / 2 else -it / 2 } + fadeIn(tween(320), initialAlpha = 0.55f)
        else -> slideInHorizontally(popSpec) { -it / 4 } + fadeIn(tween(320), initialAlpha = 0.55f)
    }
}

private fun popExit(from: String?, to: String?): ExitTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        // Today is the start destination, so any tab -> Today is a pop. No fade here, so the leaving tab must travel the
        // whole width or it stops a quarter of the way and vanishes.
        a >= 0 && b >= 0 -> slideOutHorizontally(popSpec) { if (b > a) -it else it }
        from == Routes.WEIGHT -> slideOutVertically(popSpec) { it }
        else -> slideOutHorizontally(popSpec) { it }
    }
}

/** Cold start: a jug on the ground that fades out once the state is loaded. Sits above the NavHost in the Box. */
@Composable
private fun Splash(loaded: Boolean) {
    val c = CutTheme.colors
    var shown by remember { mutableStateOf(true) }
    LaunchedEffect(loaded) { if (loaded) { delay(250); shown = false } }
    if (!shown) return
    val alpha by animateFloatAsState(if (loaded) 0f else 1f, tween(220), label = "splashAlpha")
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(c.ground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Hold(HoldShape.JUG, c.accent, 56.dp)
            Text("SENDLINE", color = c.text2, style = cutStyle(12, 800, 1f, 0.16f))
        }
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
    val haptics = LocalHaptics.current
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
                            role = Role.Tab, onClick = { if (!on) haptics.tick(); onGo(t.route) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val lift by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), label = "tab")
                    Icon(t.icon, contentDescription = t.label, Modifier.size(22.dp).scale(lift), tint = tint)
                    Text(t.label.uppercase(), color = tint, style = cutStyle(11, 700, 1f, 0.06f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
    }
}
