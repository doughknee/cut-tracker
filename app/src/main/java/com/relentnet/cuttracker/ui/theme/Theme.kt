package com.relentnet.cuttracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Design tokens from the prototype's CSS variables. Never hardcode a color in a composable; read these. */
@Immutable
data class CutColors(
    val ground: Color,
    val surface: Color,
    val hairline: Color,
    val mutedBorder: Color,
    val text: Color,
    val text2: Color,
    val dim: Color,
    val accent: Color,
    val onAccent: Color,
    val good: Color,
    val goodDim: Color,
    val freeze: Color,
    val miss: Color,
    val future: Color,
    /** The climbing wall behind the week on Today. */
    val wall: Color,
    val isDark: Boolean,
)

val DarkCutColors = CutColors(
    ground = Color(0xFF0C0D0E), surface = Color(0xFF15171A), hairline = Color(0xFF23252A), mutedBorder = Color(0xFF3A3D44),
    text = Color(0xFFF2EEE6), text2 = Color(0xFF8A8780), dim = Color(0xFF5A5D64),
    accent = Color(0xFFFF5A1F), onAccent = Color(0xFF0C0D0E), good = Color(0xFFC8F55A), goodDim = Color(0xFF4F5A3A),
    freeze = Color(0xFF5AA9FF), miss = Color(0xFF2A2C31), future = Color(0xFF1C1E22), wall = Color(0xFF121417), isDark = true,
)

val LightCutColors = CutColors(
    ground = Color(0xFFF3F1EC), surface = Color(0xFFFFFFFF), hairline = Color(0xFFE3E0D8), mutedBorder = Color(0xFFC9C5BB),
    text = Color(0xFF141413), text2 = Color(0xFF6B685F), dim = Color(0xFFA09C92),
    accent = Color(0xFFE8450A), onAccent = Color(0xFFFFFFFF), good = Color(0xFF4F8A0E), goodDim = Color(0xFFC9DDB0),
    freeze = Color(0xFF2F7FD6), miss = Color(0xFFD8D4CB), future = Color(0xFFE9E6DF), wall = Color(0xFFE6E3DC), isDark = false,
)

val LocalCutColors = staticCompositionLocalOf { DarkCutColors }

/** What the current look pins to the holds. Holds read it so every hold in the app dresses up at once. */
val LocalDecor = staticCompositionLocalOf { Decor.NONE }

/** The words the screens speak: climbing, or plain for people who do not climb. */
val LocalVocab = staticCompositionLocalOf { com.relentnet.cuttracker.data.Vocab.CLIMBING }
val LocalPrefs = staticCompositionLocalOf { com.relentnet.cuttracker.data.Prefs() }
/** Calm motion: no confetti, no slam, fades instead of slides. Settings, or the phone's own animation switch. */
val LocalCalm = staticCompositionLocalOf { false }

enum class ThemeMode { SYSTEM, DARK, LIGHT, SCHEDULE;
    companion object { fun of(s: String) = when (s) { "dark" -> DARK; "light" -> LIGHT; "schedule" -> SCHEDULE; else -> SYSTEM } }
}

object CutTheme {
    val colors: CutColors
        @Composable get() = LocalCutColors.current
    val vocab: com.relentnet.cuttracker.data.Vocab
        @Composable get() = LocalVocab.current
    val prefs: com.relentnet.cuttracker.data.Prefs
        @Composable get() = LocalPrefs.current
    val calm: Boolean
        @Composable get() = LocalCalm.current
}

/** Higher contrast: secondary text, dim text and borders move toward the main text color. */
private fun CutColors.contrasted(): CutColors = copy(
    text2 = androidx.compose.ui.graphics.lerp(text2, text, 0.45f),
    dim = androidx.compose.ui.graphics.lerp(dim, text2, 0.6f),
    mutedBorder = androidx.compose.ui.graphics.lerp(mutedBorder, text2, 0.4f),
    hairline = androidx.compose.ui.graphics.lerp(hairline, mutedBorder, 0.6f),
)

/** Whether [mode] means dark right now; [now] matters only for the schedule. */
@Composable
fun isDarkFor(mode: ThemeMode, now: java.time.LocalTime): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SCHEDULE -> Palettes.scheduledDark(now)
}

/** The swap is the show: each token slides to the new look instead of snapping. */
@Composable
private fun CutColors.eased(): CutColors = copy(
    ground = ground.eased("ground"), surface = surface.eased("surface"), hairline = hairline.eased("hairline"), mutedBorder = mutedBorder.eased("muted"),
    text = text.eased("text"), text2 = text2.eased("text2"), dim = dim.eased("dim"), accent = accent.eased("accent"), onAccent = onAccent.eased("onAccent"),
    good = good.eased("good"), goodDim = goodDim.eased("goodDim"), freeze = freeze.eased("freeze"), miss = miss.eased("miss"), future = future.eased("future"),
    wall = wall.eased("wall"),
)

@Composable
private fun Color.eased(label: String): Color = animateColorAsState(this, tween(400), label = label).value

@Composable
fun CutTheme(mode: ThemeMode, palette: Palette = Palettes.SANDSTONE, now: java.time.LocalTime = java.time.LocalTime.now(), highContrast: Boolean = false, content: @Composable () -> Unit) {
    val dark = isDarkFor(mode, now)
    // Keyed on dark so a mode flip snaps (the states are rebuilt at the new colors) while a palette change eases.
    val c = key(dark) { palette.colors(dark).let { if (highContrast) it.contrasted() else it }.eased() }
    val scheme = if (dark) darkColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.ground, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surface, onSurfaceVariant = c.text2,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface, surfaceContainerLow = c.ground,
        outline = c.mutedBorder, outlineVariant = c.hairline, tertiary = c.good, onTertiary = c.ground,
        secondary = c.freeze, onSecondary = c.ground, error = c.accent, onError = c.onAccent,
    ) else lightColorScheme(
        primary = c.accent, onPrimary = c.onAccent, background = c.ground, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surface, onSurfaceVariant = c.text2,
        surfaceContainer = c.surface, surfaceContainerHigh = c.surface, surfaceContainerLow = c.ground,
        outline = c.mutedBorder, outlineVariant = c.hairline, tertiary = c.good, onTertiary = c.surface,
        secondary = c.freeze, onSecondary = c.surface, error = c.accent, onError = c.onAccent,
    )
    // Status and nav bar icons follow the app theme, not the system one.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
    }
    CompositionLocalProvider(LocalCutColors provides c, LocalDecor provides palette.decor) {
        MaterialTheme(colorScheme = scheme, typography = CutTypography, content = content)
    }
}
