package com.relentnet.cuttracker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.relentnet.cuttracker.R

private fun archivo(weight: Int, width: Float = 100f) = Font(
    R.font.archivo,
    FontWeight(weight),
    FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

/** Archivo at normal width, all weights the UI uses. */
val Archivo = FontFamily(archivo(400), archivo(500), archivo(600), archivo(700), archivo(800), archivo(900))

/** Archivo at 90% width: the prototype's `font-stretch:90%` headline face. */
val ArchivoDisplay = FontFamily(archivo(700, 90f), archivo(800, 90f), archivo(900, 90f))

/** Archivo at 85% width: the takeover's 120sp number. */
val ArchivoTakeover = FontFamily(archivo(900, 85f))

/** Tabular figures so numbers tick in place instead of jittering. */
const val TNUM = "tnum"

private val noPad = PlatformTextStyle(includeFontPadding = false)
private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

fun cutStyle(
    size: Int,
    weight: Int,
    lineHeight: Float = 1.2f,
    tracking: Float = 0f,
    tabular: Boolean = false,
    display: Boolean = false,
) = TextStyle(
    fontFamily = if (display) ArchivoDisplay else Archivo,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = if (tabular) TNUM else null,
    platformStyle = noPad,
    lineHeightStyle = tight,
)

val CutTypography = Typography(
    displayLarge = cutStyle(44, 800, 1f, -0.02f, display = true),
    headlineMedium = cutStyle(22, 800, 1f, -0.02f, display = true),
    titleMedium = cutStyle(16, 600),
    bodyLarge = cutStyle(15, 500, 1.45f),
    bodyMedium = cutStyle(13, 500, 1.4f),
    bodySmall = cutStyle(12, 500, 1.3f),
    labelLarge = cutStyle(15, 800, 1f, 0.06f),
    labelMedium = cutStyle(12, 600, 1f, 0.12f),
    labelSmall = cutStyle(11, 600, 1f, 0.1f),
)
