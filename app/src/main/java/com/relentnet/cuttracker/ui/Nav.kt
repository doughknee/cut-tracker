package com.relentnet.cuttracker.ui

import androidx.compose.foundation.selection.selectable
import androidx.activity.compose.BackHandler
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
import com.relentnet.cuttracker.ui.beta.DayPlanPage
import com.relentnet.cuttracker.ui.beta.TargetsPage
import com.relentnet.cuttracker.ui.beta.WeekPage
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
    /** Developer options: the first run as a new user, saving nothing. */
    const val ONBOARDING_PREVIEW = "onboarding-preview"
    const val BETA_MEALS = "beta/meals"
    const val BETA_LIFTS = "beta/lifts"
    const val BETA_DAY = "beta/day/{mode}"
    const val BETA_TARGETS = "beta/targets"
    const val BETA_WEEK = "beta/week"
    fun betaDay(mode: com.relentnet.cuttracker.data.DayMode) = "beta/day/${mode.name}"
    const val SETTINGS_SECTION = "settings/{section}/{focus}"
    fun settingsSection(key: String) = "settings/" + (if ('/' in key) key else "$key/-")
    val tabs = listOf(TODAY, BETA, ROUTE, SETTINGS)
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.TODAY, "Today", CutIcons.Today),
    Tab(Routes.BETA, "Beta", CutIcons.Beta),
    Tab(Routes.ROUTE, "Route", CutIcons.Route),
    Tab(Routes.SETTINGS, "You", CutIcons.You),
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
        if (t != null && st.settings.onboarded) {
            vm.pendingTab = null
            when (t) {
                Routes.WEIGHT -> nav.navigate(Routes.WEIGHT)
                Routes.BETA_MEALS -> { nav.goTab(Routes.BETA); nav.navigate(Routes.BETA_MEALS) { launchSingleTop = true } }
                else -> nav.goTab(t)
            }
        }
    }
    LaunchedEffect(st.settings.onboarded) {
        if (!st.settings.onboarded && route != null && route != Routes.ONBOARDING) nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
    }
    // The stack holds one tab at a time, so back from Beta or Route is ours to handle: it goes to Today.
    BackHandler(enabled = route in Routes.tabs && route != Routes.TODAY) { nav.goTab(Routes.TODAY) }
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
                        OnboardingScreen(vm, onDone = { nav.navigate(Routes.TODAY) { popUpTo(0) { inclusive = true }; launchSingleTop = true } })
                    }
                    composable(Routes.TODAY) { TabRoom {
                        TodayScreen(vm, onWeight = { nav.navigate(Routes.WEIGHT) }, onRoute = { nav.goTab(Routes.ROUTE) })
                    } }
                    composable(Routes.WEIGHT) { WeightScreen(vm, onBack = { nav.popBackStack() }, onCheckpoints = { nav.navigate(Routes.CHECKPOINTS) }) }
                    composable(Routes.BETA) { TabRoom {
                        BetaScreen(
                            vm, onMeals = { nav.navigate(Routes.BETA_MEALS) }, onLifts = { nav.navigate(Routes.BETA_LIFTS) }, onDay = { nav.navigate(Routes.betaDay(it)) },
                            onCheckpoints = { nav.navigate(Routes.CHECKPOINTS) }, onTargets = { nav.navigate(Routes.BETA_TARGETS) }, onWeek = { nav.navigate(Routes.BETA_WEEK) },
                        )
                    } }
                    composable(Routes.BETA_MEALS) { MealsPage(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.BETA_LIFTS) { LiftsPage(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.BETA_DAY) { entry ->
                        val mode = runCatching { com.relentnet.cuttracker.data.DayMode.valueOf(entry.arguments?.getString("mode") ?: "") }.getOrDefault(com.relentnet.cuttracker.data.DayMode.CLIMB)
                        DayPlanPage(vm, mode, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.BETA_TARGETS) { TargetsPage(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.BETA_WEEK) { WeekPage(vm, onBack = { nav.popBackStack() }) }
                    composable(Routes.SETTINGS_SECTION) { entry ->
                        val focus = entry.arguments?.getString("focus")?.takeIf { it != "-" }
                        SettingsSectionScreen(
                            vm, section = (entry.arguments?.getString("section") ?: "data") + (if (focus != null) "/$focus" else ""), onBack = { nav.popBackStack() },
                            onPreviewOnboarding = { nav.navigate(Routes.ONBOARDING_PREVIEW) },
                        )
                    }
                    composable(Routes.ONBOARDING_PREVIEW) {
                        OnboardingScreen(vm, preview = true, onDone = { nav.popBackStack() })
                    }
                    composable(Routes.ROUTE) { TabRoom { RouteScreen(vm, onCheckpoints = { nav.navigate(Routes.CHECKPOINTS) }, onEditDay = { vm.viewDate = it; nav.goTab(Routes.TODAY) }) } }
                    composable(Routes.CHECKPOINTS) { CheckpointsScreen(vm, onBack = { nav.popBackStack() }, onShoot = { nav.navigate(Routes.CAMERA) }, onRoute = { nav.goTab(Routes.ROUTE) }) }
                    composable(Routes.CAMERA) { CameraScreen(vm, onDone = { nav.popBackStack() }) }
                    composable(Routes.SETTINGS) { TabRoom { SettingsScreen(vm, onSection = { nav.navigate(Routes.settingsSection(it)) }) } }
                }
            }
        }
        // Over the pages, not beside them: a page opening from a tab keeps its size while the bar slides away.
        androidx.compose.animation.AnimatedVisibility(
            route in Routes.tabs, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(220)) { it }, exit = slideOutVertically(tween(220)) { it },
        ) {
            Column {
                val total = st.items.size
                SendBar(progress = if (total == 0) 0f else st.totals.done.toFloat() / total, full = st.isSend)
                BottomNav(current = route, onGo = { nav.goTab(it) })
            }
        }
        Splash(true)
        vm.toast?.let { t -> ToastPill(t.text, Modifier.align(Alignment.BottomCenter).padding(bottom = 92.dp).windowInsetsPadding(WindowInsets.navigationBars), t.action, onAction = { t.onAction?.invoke(); vm.dismissToast() }) }
        vm.takeover?.let { TakeoverOverlay(it, confettiOn = st.settings.haptics, onDismiss = { vm.dismissTakeover() }) }
    }
}

// ---- Motion. Tabs slide sideways in the direction you moved; pushed pages slide up from the right and settle on a
// spring; the Weight sheet rises from the bottom. Pops reverse. Nothing fades in from nowhere.

private val slideSpec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 420f, visibilityThreshold = IntOffset.VisibilityThreshold)
private val fadeSpec = tween<Float>(180)

private fun tabIndex(route: String?) = Routes.tabs.indexOf(route)

// One tab move. Whether the nav stack calls it a push or a pop (Today is the start destination, so anything -> Today
// is a pop), the three tabs always slide the same way: leaving tab eases out a quarter width and fades, arriving tab
// springs in from half a width.
private fun tabEnter(a: Int, b: Int): EnterTransition = slideInHorizontally(slideSpec) { if (b > a) it / 2 else -it / 2 } + fadeIn(fadeSpec)
private fun tabExit(a: Int, b: Int): ExitTransition = slideOutHorizontally(slideSpec) { if (b > a) -it / 4 else it / 4 } + fadeOut(fadeSpec)

private fun enter(from: String?, to: String?): EnterTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> tabEnter(a, b)
        to == Routes.WEIGHT -> slideInVertically(slideSpec) { it / 3 } + fadeIn(fadeSpec)
        else -> slideInHorizontally(slideSpec) { it / 3 } + fadeIn(fadeSpec)
    }
}

private fun exit(from: String?, to: String?): ExitTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> tabExit(a, b)
        else -> slideOutHorizontally(slideSpec) { -it / 4 } + fadeOut(fadeSpec)
    }
}

// Pops of pushed pages mirror their pushes. The back gesture seeks these, so they are fixed-duration slides with no
// fade on the page that leaves; a spring plus a short fade reads as a fade when scrubbed.
private val popSpec = tween<IntOffset>(320, easing = FastOutSlowInEasing)

private fun popEnter(from: String?, to: String?): EnterTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> tabEnter(a, b)
        else -> slideInHorizontally(popSpec) { -it / 4 } + fadeIn(tween(320), initialAlpha = 0.55f)
    }
}

private fun popExit(from: String?, to: String?): ExitTransition {
    val a = tabIndex(from); val b = tabIndex(to)
    return when {
        a >= 0 && b >= 0 -> tabExit(a, b)
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

/**
 * Every tab switch is a push: the stack is cleared (state saved) and the tab is pushed fresh (state restored). With
 * the usual popUpTo-start recipe, going to Today was a pop, and pops run through the seekable predictive-back path,
 * which is a different animation from the push Beta <-> Route use and stutters when you hammer it.
 */
fun NavHostController.goTab(route: String) = navigate(route) {
    // The whole graph, not the start destination: once Today is off the stack, popUpTo(Today) is a no-op and the
    // stack grows one tab per switch.
    popUpTo(graph.id) { inclusive = true; saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** The 3dp Send progress bar that sits above the nav on every tab. */
/** The Send bar (3dp) and the tab row (66dp) above the gesture bar. */
private val TabBarHeight = 69.dp

/** A tab page leaves the bar's room at the bottom, so it is the same size whether the bar is showing or sliding away. */
@Composable
private fun TabRoom(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = TabBarHeight)) { content() }
}

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
                        .selectable(
                            selected = on, interactionSource = remember { MutableInteractionSource() }, indication = null,
                            role = Role.Tab, onClick = { if (!on) haptics.tick(); onGo(t.route) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val lift by animateFloatAsState(if (on) 1.12f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow), label = "tab")
                    Icon(t.icon, contentDescription = null, Modifier.size(22.dp).scale(lift), tint = tint)
                    Text((if (t.route == Routes.BETA) CutTheme.vocab.beta else t.label).uppercase(), color = tint, style = cutStyle(11, 700, 1f, 0.06f), modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
    }
}
