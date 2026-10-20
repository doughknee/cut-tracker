package com.relentnet.cuttracker.data

import kotlin.math.roundToInt

/**
 * Weights are stored in lb everywhere (Room rows, Settings, backups) and converted only where a person reads or types
 * one. [Settings.kg] picks the unit shown.
 */
object Units {
    const val KG_PER_LB = 0.45359237

    /** One decimal, "182.4", "80.0". */
    fun fmt1(v: Double) = ((v * 10).roundToInt() / 10.0).toString().let { if ('.' in it) it else "$it.0" }
}

val Settings.unit: String get() = if (kg) "kg" else "lb"

/** A weight in lb as the user reads it: the number in their unit. */
fun Settings.shown(lb: Double): Double = if (kg) lb * Units.KG_PER_LB else lb

/** A number the user typed in their unit, back to lb. */
fun Settings.toLb(v: Double): Double = if (kg) v / Units.KG_PER_LB else v

/** "182.4" or "82.7": a stored weight in the user's unit, one decimal, no unit. */
fun Settings.w(lb: Double): String = Units.fmt1(shown(lb))

/** "182.4 lb" or "82.7 kg". */
fun Settings.wu(lb: Double): String = w(lb) + " " + unit

/** A lift weight the way the gym says it: "135", "62.5". */
fun Settings.liftW(lb: Double): String = Units.fmt1(shown(lb)).removeSuffix(".0")

/**
 * A stepper's step, [dir] +1 or -1: half a pound as always, or to the next half kilo that way, so 82.7 kg goes to
 * 83.0 or 82.5 and the shown number stays on a half.
 */
fun Settings.stepped(lb: Double, dir: Int): Double {
    if (!kg) return lb + 0.5 * dir
    val halves = shown(lb) * 2
    return toLb((if (dir > 0) kotlin.math.floor(halves + 1e-6) + 1 else kotlin.math.ceil(halves - 1e-6) - 1) / 2.0)
}

/** A summary weight (an average, a pace, a projection) with the decimals Settings, Weight asks for: "182.4" or "182". */
fun Settings.wr(lb: Double): String = if (prefs.weightDecimals == 0) shown(lb).roundToInt().toString() else w(lb)

/** [wr] with the unit. */
fun Settings.wru(lb: Double): String = wr(lb) + " " + unit
