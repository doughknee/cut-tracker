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

    // Today, more
    /** Bigger holds on Today's wall. */
    val bigHolds: Boolean = false,
    /** MON, TUE under each hold. */
    val weekdayLabels: Boolean = true,
    /** The chain count in Today's header. */
    val showChain: Boolean = true,
    /** UP NEXT over the next row, and its accent edge. */
    val showUpNext: Boolean = true,
    /** The line under each item: the plan's note, the last weigh-in. */
    val showSubs: Boolean = true,
    /** A line through ticked items. */
    val strikeDone: Boolean = true,
    /** Protein beside the kind of day. */
    val showProtein: Boolean = true,
    /** With Hide completed, the Done list starts open. */
    val doneOpen: Boolean = false,
    /** The 7-day average tile at the foot of Today. */
    val showWeightTile: Boolean = true,

    // Weight
    /** The big number on Weight and Today's tile: average or latest (the last reading). */
    val weightHero: String = "average",
    /** Decimals on the average and pace: 1 or 0. */
    val weightDecimals: Int = 1,
    /** Weeks the chart shows, ending today; 0 is the whole route. */
    val chartWeeks: Int = 0,
    /** Every reading on the chart as a dot, not only while it is held. */
    val chartDots: Boolean = false,
    /** The dashed line from the average to where the current rate lands. */
    val chartRate: Boolean = true,
    /** Checkpoint targets as rings on the chart. */
    val chartCheckpoints: Boolean = true,

    // Nudges, more
    /** Your own words for the weigh-in reminder; blank keeps the app's. */
    val weighText: String = "",
    val pickText: String = "",
    /** Minutes a Snooze button on each reminder waits; 0 is no button. */
    val snoozeMinutes: Int = 0,
    /** On the lock screen a reminder says only "Sendline reminder". */
    val lockScreenHide: Boolean = false,

    // Widget, more
    /** phone, dark or light. */
    val widgetTheme: String = "phone",
    /** The widget's background lets the wallpaper through. */
    val widgetSeeThrough: Boolean = false,
    /** The xp on the widget's buttons and rows. */
    val widgetXp: Boolean = true,

    // Feel and access, more
    /** Heavier type everywhere. */
    val boldText: Boolean = false,
    /** Taller chips and a 56dp minimum for every tap target. */
    val bigTargets: Boolean = false,
    /** A mark on each hold for how the day went, so the color is never the only cue. */
    val holdMarks: Boolean = false,
    /** No dots, stripes or chalk glow behind the holds. */
    val plainWall: Boolean = false,

    // Words and units, more
    /** 20 Oct instead of Oct 20. */
    val dayFirst: Boolean = false,

    // Privacy
    /** The phone blocks screenshots and blanks the app in Recents. */
    val secureScreen: Boolean = false,

    // New meals and lifts
    val liftSets: Int = 3,
    val liftReps: String = "8 to 12",
    /** Times the usual weight step (5 lb or 2.5 kg) a new lift starts with: 0.5, 1 or 2. */
    val liftStepScale: Double = 1.0,
    /** A food added to a meal starts at its first portion; off, at 100 g. */
    val foodPortion: Boolean = true,
    /** The screen stays on while the lifts are open on Today. */
    val keepScreenOn: Boolean = false,
) {
    val weekStart: java.time.DayOfWeek get() = if (weekStartsSunday) java.time.DayOfWeek.SUNDAY else java.time.DayOfWeek.MONDAY
}

/**
 * Dates the way the user reads them: Oct 20 or 20 Oct. Set by Repo with the rest of the state.
 * ponytail: process-wide, like [Clock]; a static LocalPrefs change recomposes every screen that formats one.
 */
object Dates {
    @Volatile var dayFirst: Boolean = false
    private fun f(us: String, uk: String) = java.time.format.DateTimeFormatter.ofPattern(if (dayFirst) uk else us, java.util.Locale.US)
    /** Oct 20 / 20 Oct. */
    fun md(d: java.time.LocalDate): String = d.format(f("MMM d", "d MMM"))
    /** Tue Oct 20 / Tue 20 Oct. */
    fun wmd(d: java.time.LocalDate): String = d.format(f("EEE MMM d", "EEE d MMM"))
    /** Tuesday Oct 20 / Tuesday 20 Oct. */
    fun wwmd(d: java.time.LocalDate): String = d.format(f("EEEE MMM d", "EEEE d MMM"))
    /** Tuesday October 20 / Tuesday 20 October. */
    fun long(d: java.time.LocalDate): String = d.format(f("EEEE MMMM d", "EEEE d MMMM"))
    /** Oct 19 to 25 / 19 to 25 Oct; across a month, Oct 30 to Nov 2 / 30 Oct to 2 Nov. */
    fun span(a: java.time.LocalDate, b: java.time.LocalDate): String = when {
        a.month != b.month -> "${md(a)} to ${md(b)}"
        dayFirst -> "${a.dayOfMonth} to ${md(b)}"
        else -> "${md(a)} to ${b.dayOfMonth}"
    }
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
