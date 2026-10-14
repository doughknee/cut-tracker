package com.relentnet.cuttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
    val isDark: Boolean,
)

val DarkCutColors = CutColors(
    ground = Color(0xFF0C0D0E), surface = Color(0xFF15171A), hairline = Color(0xFF23252A), mutedBorder = Color(0xFF3A3D44),
    text = Color(0xFFF2EEE6), text2 = Color(0xFF8A8780), dim = Color(0xFF5A5D64),
    accent = Color(0xFFFF5A1F), onAccent = Color(0xFF0C0D0E), good = Color(0xFFC8F55A), goodDim = Color(0xFF4F5A3A),
    freeze = Color(0xFF5AA9FF), miss = Color(0xFF2A2C31), future = Color(0xFF1C1E22), isDark = true,
)

val LightCutColors = CutColors(
    ground = Color(0xFFF3F1EC), surface = Color(0xFFFFFFFF), hairline = Color(0xFFE3E0D8), mutedBorder = Color(0xFFC9C5BB),
    text = Color(0xFF141413), text2 = Color(0xFF6B685F), dim = Color(0xFFA09C92),
    accent = Color(0xFFE8450A), onAccent = Color(0xFFFFFFFF), good = Color(0xFF4F8A0E), goodDim = Color(0xFFC9DDB0),
    freeze = Color(0xFF2F7FD6), miss = Color(0xFFD8D4CB), future = Color(0xFFE9E6DF), isDark = false,
)

val LocalCutColors = staticCompositionLocalOf { DarkCutColors }

enum class ThemeMode { SYSTEM, DARK, LIGHT }

object CutTheme {
    val colors: CutColors
        @Composable get() = LocalCutColors.current
}

@Composable
fun CutTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val c = if (dark) DarkCutColors else LightCutColors
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
    CompositionLocalProvider(LocalCutColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = CutTypography, content = content)
    }
}
