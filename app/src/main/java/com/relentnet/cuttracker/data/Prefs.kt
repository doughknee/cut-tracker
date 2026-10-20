package com.relentnet.cuttracker.data

import kotlinx.serialization.Serializable

/**
 * How the app shows and feels, as opposed to the route itself. Every default is how the app behaved before the option
 * existed, so an upgrade changes nothing until the user does. Stored as one JSON value in the settings store and carried
 * whole in a backup; a key this build does not know is ignored, a missing one takes its default.
 */
@Serializable
data class Prefs(
    // Today's wall
    /** The orange week tape on the wall; its arrows step the week. Off, a sideways swipe still does. */
    val showTape: Boolean = true,
    /** The rope that joins the days of the chain. */
    val showRope: Boolean = true,
    /** Holiday decor on the wall and holds, when the calendar look brings some. */
    val wallDecor: Boolean = true,

    // Today's script
    val showXp: Boolean = true,
    val showTimes: Boolean = true,
    /** Calories beside protein in the day's totals. */
    val showCalories: Boolean = true,
    /** A swipe to the right ticks a row. Off, rows tick by tap only. */
    val swipeToTick: Boolean = true,
    /** The card that guesses Rest day once the pick-a-day time has passed. */
    val eveningCard: Boolean = true,
    /** The weigh-in nudge buttons, in the unit shown: 0.1, 0.2 or 0.5. Today and the widget. */
    val weighStep: Double = 0.2,
    /** The tab the app opens on: today, beta or route. */
    val startTab: String = "today",

    // Feel and access
    /** big: full screen with confetti; quiet: a toast; off: nothing. For a Send and a new grade. */
    val celebrate: String = "big",
    /** phone: follow the phone's animation setting; on: always calm the motion. */
    val reduceMotion: String = "phone",
    /** On top of the phone's own text size: 1, 1.15 or 1.3. */
    val textScale: Float = 1f,
    /** Brighter secondary text and borders. */
    val highContrast: Boolean = false,

    // Words and units
    val weekStartsSunday: Boolean = false,
    /** phone, 12 or 24. */
    val clock: String = "phone",

    // Reminders, widget, updates
    /** On a checkpoint day the weigh-in reminder says so and asks for the photo. */
    val checkpointNote: Boolean = true,
    /** The widget shows dots instead of your weight until you tap it. */
    val widgetHideWeight: Boolean = false,
    /** The short note on what changed, the first time an updated app opens. */
    val whatsNewNote: Boolean = true,
) {
    val weekStart: java.time.DayOfWeek get() = if (weekStartsSunday) java.time.DayOfWeek.SUNDAY else java.time.DayOfWeek.MONDAY
}

/**
 * 12 or 24-hour times: the phone's choice unless Settings overrides it. Set by Repo with the rest of the state.
 * ponytail: process-wide, like ProgramEngine.firstDay.
 */
object Clock {
    @Volatile var mode: String = "phone"
    fun is24(context: android.content.Context): Boolean = when (mode) {
        "24" -> true
        "12" -> false
        else -> android.text.format.DateFormat.is24HourFormat(context)
    }
    fun text(context: android.content.Context, t: java.time.LocalTime): String =
        t.format(java.time.format.DateTimeFormatter.ofPattern(if (is24(context)) "H:mm" else "h:mm a", java.util.Locale.US))
}
