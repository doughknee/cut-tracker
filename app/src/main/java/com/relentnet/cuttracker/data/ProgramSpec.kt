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
data class LibMeal(
    val id: String, val name: String, val protein: Int, val calories: Int, val recipe: String = "",
    /** The slot (meal kind) this belongs to: a [Meal.id]. Blank means Other. */
    val kind: String = "",
    /** How the numbers were built, when they were: one line per food. Empty means typed by hand. */
    val ingredients: List<Ingredient> = emptyList(),
)

/** One line of a meal: a food by name, how much of it, and its nutrition per 100 g, so the meal needs no table to add up. */
@Serializable
data class Ingredient(val name: String, val grams: Double, val protein100: Double, val kcal100: Double) {
    val protein: Double get() = protein100 * grams / 100
    val kcal: Double get() = kcal100 * grams / 100
}

fun List<Ingredient>.proteinSum(): Int = Math.round(sumOf { it.protein }).toInt()
fun List<Ingredient>.kcalSum(): Int = Math.round(sumOf { it.kcal }).toInt()

/** One non-meal line of a day's script, like the climbing session. Weigh-in is always there and is not a PlanItem. */
@Serializable
data class PlanItem(val id: String, val label: String, val sub: String = "", val time: String = "", val xp: Int = 10, val on: Boolean = true)

/** What one kind of day asks for: which meal slots, which lifts make its block, and the rest of the script. */
@Serializable
data class DayPlan(val slots: List<String> = emptyList(), val lifts: List<String> = emptyList(), val items: List<PlanItem> = emptyList())

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
    /** Push, Pull, Legs, Core or a kind of your own. Blank means Other. */
    val kind: String = "",
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
    const val XP_WORKOUT_TARGET = 40
    const val WORKOUTS_PER_WEEK = 3

    val meals = listOf(
        Meal("b", "Breakfast", "Greek yogurt + whey + banana", "9:00", 48, 430, xp = 10, defaultLib = "lib_b"),
        Meal("l", "Lunch", "Chicken + rice + veg", "1:00", 55, 620, xp = 10, defaultLib = "lib_l"),
        Meal("s", "Snack", "String cheese + apple", "4:00", 20, 250, xp = 5, defaultLib = "lib_s"),
        Meal("d", "Dinner", "Burrito bowl", "7:30", 46, 820, xp = 10, pick = true, defaultLib = "lib_d1"),
    )

    val library = listOf(
        LibMeal("lib_b", "Greek yogurt + whey + banana", 48, 430, kind = "b"),
        LibMeal("lib_l", "Chicken or turkey + rice + veg", 55, 620, kind = "l"),
        LibMeal("lib_s", "String cheese + apple or shake", 20, 250, kind = "s"),
        LibMeal("lib_d1", "Burrito bowl", 46, 820, kind = "d"),
        LibMeal("lib_d2", "Salmon + potatoes", 44, 760, kind = "d"),
        LibMeal("lib_d3", "Steak + salad", 48, 900, kind = "d"),
    )

    /** A library meal saved before kinds existed gets one from its id, then from the slot that defaults to it. */
    fun inferMealKind(m: LibMeal, slots: List<Meal>): String = when {
        m.kind.isNotBlank() -> m.kind
        m.id.startsWith("lib_d") -> "d"
        m.id.startsWith("lib_b") -> "b"
        m.id.startsWith("lib_l") -> "l"
        m.id.startsWith("lib_s") -> "s"
        else -> slots.firstOrNull { it.defaultLib == m.id }?.id ?: ""
    }

    val LIFT_KINDS = listOf("Push", "Pull", "Legs", "Core")

    val exercises = listOf(
        Exercise("dips", "Dips", Block.CLIMB, 3, "6 to 10", 5.0, "Lb added", setsPhase2 = 4, kind = "Push"),
        Exercise("pushups", "Push-ups", Block.CLIMB, 2, "to 2 shy of failure", 0.0, "", bwLabel = "Body", kind = "Push"),
        Exercise("roller", "Ab roller", Block.OFF, 3, "12 to 15", 0.0, "Reach", bwLabel = "Knees", setsPhase2 = 1, kind = "Core"),
        Exercise("legraise", "Lying leg raise", Block.OFF, 3, "15", 0.0, "Tempo", bwLabel = "Slow", kind = "Core"),
        Exercise("floorpress", "DB floor press", Block.OFF, 3, "8 to 12", 5.0, "Lb / side", setsPhase2 = 4, kind = "Push"),
        Exercise("inclinepress", "Incline DB press", Block.OFF, 3, "8 to 12", 5.0, "Lb / side", phase2Only = true, kind = "Push"),
        Exercise("pullover", "DB pullover", Block.OFF, 2, "12", 5.0, "Lb", kind = "Pull"),
    )

    fun inferLiftKind(e: Exercise): String = if (e.kind.isNotBlank()) e.kind else exercises.firstOrNull { it.id == e.id }?.kind ?: ""

    /** The plans a fresh install starts with, and what an older install gets from its exercises' blocks. */
    fun defaultPlans(meals: List<Meal> = this.meals, exercises: List<Exercise> = this.exercises): Map<DayMode, DayPlan> {
        val slots = meals.map { it.id }
        fun block(b: Block) = exercises.filter { it.block == b && !it.phase2Only }.map { it.id }
        return mapOf(
            DayMode.CLIMB to DayPlan(slots, block(Block.CLIMB), listOf(PlanItem(ITEM_CLIMB, "Climb", "Climbing session. Before lunch.", "11:00", XP_CLIMB))),
            DayMode.OFF to DayPlan(slots, block(Block.OFF)),
            DayMode.REST to DayPlan(slots),
        )
    }

    /** Checkpoints are generated from the program: one every two weeks on the pace line, the last one at the target. */
    fun defaultCheckpoints(start: LocalDate, startWeight: Double, targetWeight: Double, weeks: Int): List<Checkpoint> {
        val every = if (weeks >= 6) 2 else 1
        val marks = (every..weeks step every).toMutableList().apply { if (lastOrNull() != weeks) add(weeks) }
        return marks.map { w ->
            val target = startWeight - (startWeight - targetWeight) * w / weeks
            val rounded = kotlin.math.round(target * 2) / 2
            Checkpoint(
                start.plusDays(7L * w).toString(), rounded,
                note = if (w == weeks) "Last checkpoint. Top out." else "Pace check. Photo day.",
            )
        }
    }

    /**
     * When the route's start, goal, weeks or start date change and its flags are still the ones the old numbers made,
     * the flags follow the new numbers. Flags edited by hand, and planned skips, stay where they are.
     */
    fun followFlags(old: Settings, new: Settings): Settings {
        if (old.startDate == new.startDate && old.startWeight == new.startWeight && old.targetWeight == new.targetWeight && old.lengthWeeks == new.lengthWeeks) return new
        if (!flagsAreDefault(old)) return new
        val skips = new.checkpoints.filter { it.free }
        return new.copy(checkpoints = (skips + defaultCheckpoints(new.startDate, new.startWeight, new.targetWeight, new.lengthWeeks)).sortedBy { it.date })
    }

    /** True while the route's flags are exactly the defaults its numbers make (by date and target). */
    fun flagsAreDefault(s: Settings): Boolean =
        s.checkpoints.filter { !it.free }.map { it.date to it.target } ==
            defaultCheckpoints(s.startDate, s.startWeight, s.targetWeight, s.lengthWeeks).map { it.date to it.target }

    /** The defaults before onboarding; onboarding replaces them with the user's own. */
    val checkpoints: List<Checkpoint> get() = defaultCheckpoints(PHASE_1_START, START_WEIGHT, TARGET_WEIGHT, LENGTH_WEEKS)
}
