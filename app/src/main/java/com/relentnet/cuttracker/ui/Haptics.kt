package com.relentnet.cuttracker.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf

/** One haptics entry point so the Settings "Haptics and confetti" toggle gates every buzz. */
class Haptics(context: Context, var enabled: () -> Boolean = { true }) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun run(effect: VibrationEffect) {
        if (!enabled()) return
        runCatching { vibrator?.vibrate(effect) }
    }

    /** Checking an item: CONFIRM-class tick. */
    fun tap() = run(
        if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        else VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE),
    )

    /** Stepper and chip taps. */
    fun tick() = run(
        if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        else VibrationEffect.createOneShot(6, VibrationEffect.DEFAULT_AMPLITUDE),
    )

    /** Send and the bonus slam. */
    fun heavy() = run(
        if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        else VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE),
    )

    /** Level-up and the rest timer: off/on pairs in ms. */
    fun pattern(vararg timings: Long) = run(VibrationEffect.createWaveform(timings, -1))
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }
