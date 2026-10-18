package com.relentnet.cuttracker.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

enum class DayMode { CLIMB, OFF, REST }

enum class Block { CLIMB, OFF }

@Serializable
data class Meal(
    val id: String,
    val name: String,
    val sub: String,
    val time: String,
    val protein: Int,
    val calories: Int,
    val xp: Int = 10,
    /** Legacy flag from v1; every slot has a picker now. */
    val pick: Boolean = false,
    /** Library meal this slot logs by default; null falls back to the slot's own numbers. */
    val defaultLib: String? = null,
)

/** A named meal in the library: what a slot can log, with an optional recipe or notes. */
@Serializable
data class LibMeal(val id: String, val name: String, val protein: Int, val calories: Int, val recipe: String = "")

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val block: Block,
    val sets: Int,
    val repTarget: String,
    /** Weight step in lb per + tap. 0 means bodyweight: [bwLabel] shows in the weight slot and the stepper hides. */
    val step: Double,
    val unit: String,
    val bwLabel: String? = null,
    /** Phase 2 overrides sets for some lifts; null means same as phase 1. */
    val setsPhase2: Int? = null,
    val phase2Only: Boolean = false,
)

@Serializable
data class Checkpoint(
    val date: String,
    val target: Double? = null,
    val free: Boolean = false,
    val days: Int = 1,
    val label: String? = null,
    val note: String,
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
    val lastDate: LocalDate get() = localDate.plusDays((days - 1).toLong())
    fun covers(d: LocalDate) = !d.isBefore(localDate) && !d.isAfter(lastDate)
}

@Serializable
data class ProjectSpec(val id: String, val title: String, val badgeId: String, val badgeName: String, val target: Int)

/** Program defaults. Settings overrides persist in DataStore; this object is what Reset program returns to. */
object ProgramSpec {
    val PHASE_1_START: LocalDate = LocalDate.of(2026, 10, 14)
    const val LENGTH_WEEKS = 12
    const val START_WEIGHT = 181.8
    const val TARGET_WEIGHT = 170.0
    const val CALORIES_P1 = 2250
    const val CALORIES_P2 = 2700
    const val PROTEIN_G = 165
    const val DAY_RESET_HOUR = 4
    const val PHOTO_INTERVAL_DAYS = 14
    const val PACE_TOLERANCE_LB = 0.3
    const val FREEZES_PER_MONTH = 2
    const val REST_SECONDS = 90
    val WEIGH_IN_TIME: LocalTime = LocalTime.of(8, 30)
    val PICK_TIME: LocalTime = LocalTime.of(19, 0)

    val LEVELS = intArrayOf(0, 300, 700, 1200, 1800, 2500, 3300, 4200, 5200, 6300, 7500)
    val CHAIN_BADGES = listOf(7, 14, 30, 60, 84)

    // Script item ids. Meals use their meal id; these are the fixed non-meal rows.
    const val ITEM_WEIGH = "w"
    const val ITEM_CLIMB = "c"
    const val ITEM_PUSH = "p"
    const val ITEM_CORE = "k"
    const val XP_WEIGH = 10
    const val XP_CLIMB = 25
    const val XP_PUSH = 30
    const val XP_CORE = 30
    const val XP_LIFT_SESSION = 20
    const val XP_LIFT_BEAT = 15
    const val XP_WEEK_WON = 50
    const val XP_PHOTO = 40
    const val XP_SEND = 50
    const val XP_MIN = 20
    const val XP_PROJECT = 150
    const val XP_WORKOUT_TARGET = 40
    const val WORKOUTS_PER_WEEK = 3

    val meals = listOf(
        Meal("b", "Breakfast", "Greek yogurt + whey + banana", "9:00", 48, 430, xp = 10, defaultLib = "lib_b"),
        Meal("l", "Lunch", "Chicken + rice + veg", "1:00", 55, 620, xp = 10, defaultLib = "lib_l"),
        Meal("s", "Snack", "String cheese + apple", "4:00", 20, 250, xp = 5, defaultLib = "lib_s"),
        Meal("d", "Dinner", "Burrito bowl", "7:30", 46, 820, xp = 10, pick = true, defaultLib = "lib_d1"),
    )

    val library = listOf(
        LibMeal("lib_b", "Greek yogurt + whey + banana", 48, 430),
        LibMeal("lib_l", "Chicken or turkey + rice + veg", 55, 620),
        LibMeal("lib_s", "String cheese + apple or shake", 20, 250),
        LibMeal("lib_d1", "Burrito bowl", 46, 820),
        LibMeal("lib_d2", "Salmon + potatoes", 44, 760),
        LibMeal("lib_d3", "Steak + salad", 48, 900),
    )

    val exercises = listOf(
        Exercise("dips", "Dips", Block.CLIMB, 3, "6 to 10", 5.0, "Lb added", setsPhase2 = 4),
        Exercise("pushups", "Push-ups", Block.CLIMB, 2, "to 2 shy of failure", 0.0, "", bwLabel = "Body"),
        Exercise("roller", "Ab roller", Block.OFF, 3, "12 to 15", 0.0, "Reach", bwLabel = "Knees", setsPhase2 = 1),
        Exercise("legraise", "Lying leg raise", Block.OFF, 3, "15", 0.0, "Tempo", bwLabel = "Slow"),
        Exercise("floorpress", "DB floor press", Block.OFF, 3, "8 to 12", 5.0, "Lb / side", setsPhase2 = 4),
        Exercise("inclinepress", "Incline DB press", Block.OFF, 3, "8 to 12", 5.0, "Lb / side", phase2Only = true),
        Exercise("pullover", "DB pullover", Block.OFF, 2, "12", 5.0, "Lb"),
    )

    val checkpoints = listOf(
        Checkpoint("2026-10-28", 179.5, note = "Stop tracking food. You know the portions now."),
        Checkpoint("2026-11-11", 177.5, note = "Upper abs clear in bathroom light. Dips +10 lb or 15 bodyweight reps. Behind? Cut 200 cal."),
        Checkpoint("2026-11-26", free = true, label = "Thanksgiving", note = "Eat whatever. One day. Script resumes the 27th."),
        Checkpoint("2026-12-09", 174.0, note = "4-pack standing relaxed. Floor press up 20+ lb."),
        Checkpoint("2026-12-24", free = true, days = 2, label = "Christmas", note = "Two days. Then back."),
        Checkpoint("2027-01-06", 170.0, note = "6-pack or close. Cut ends regardless. Phase 2 unlocks."),
    )

    val projects = listOf(
        ProjectSpec("dips3", "Beat last week's dips three times", "overhang", "Overhang", 3),
        ProjectSpec("weigh7", "Log weight all 7 days", "crimp", "Crimp", 7),
        ProjectSpec("sends3", "Three Sends in a row", "dyno", "Dyno", 3),
        ProjectSpec("push2", "Two push blocks on climb days", "heelhook", "Heel Hook", 2),
        ProjectSpec("protein6", "Hit protein 6 of 7 days", "mantle", "Mantle", 6),
        ProjectSpec("offnoon3", "One full Off-day block before noon, three times", "topout", "Top Out", 3),
        // Target reads Settings.workoutsPerWeek at runtime; 3 here is only the default.
        ProjectSpec("workouts", "Hit the workout target", "jug", "Jug", WORKOUTS_PER_WEEK),
    )
}
