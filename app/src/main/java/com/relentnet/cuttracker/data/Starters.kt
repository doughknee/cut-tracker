package com.relentnet.cuttracker.data

import kotlin.math.abs

/**
 * What onboarding offers a new user so the first script is theirs, not the sample: a few common high-protein meals per
 * slot (numbers are typical portions, rounded) and four places to lift. Everything lands in Settings as ordinary
 * library meals, slots and plans, editable on Beta like anything else.
 */
object Starters {
    data class Pick(val meal: LibMeal, val veg: Boolean)

    /** Per slot id, the sample meal first, so a tie keeps the sample. */
    val meals: Map<String, List<Pick>> = mapOf(
        "b" to listOf(
            Pick(ProgramSpec.library[0], veg = true),
            Pick(LibMeal("st_b_eggs", "Eggs + egg whites on toast", 43, 450, kind = "b"), veg = true),
            Pick(LibMeal("st_b_oats", "Protein oats with milk", 40, 475, kind = "b"), veg = true),
            Pick(LibMeal("st_b_cottage", "Cottage cheese + berries", 38, 280, kind = "b"), veg = true),
        ),
        "l" to listOf(
            Pick(ProgramSpec.library[1], veg = false),
            Pick(LibMeal("st_l_tuna", "Tuna, edamame and rice bowl", 48, 570, kind = "l"), veg = false),
            Pick(LibMeal("st_l_wrap", "Turkey wrap + Greek yogurt", 50, 600, kind = "l"), veg = false),
            Pick(LibMeal("st_l_tofu", "Tofu, lentil and rice bowl", 47, 650, kind = "l"), veg = true),
            Pick(LibMeal("st_l_eggsalad", "Egg salad sandwich + skyr", 37, 460, kind = "l"), veg = true),
        ),
        "s" to listOf(
            Pick(ProgramSpec.library[2], veg = true),
            Pick(LibMeal("st_s_shake", "Protein shake", 25, 130, kind = "s"), veg = true),
            Pick(LibMeal("st_s_yogurt", "Greek yogurt + almonds", 20, 190, kind = "s"), veg = true),
            Pick(LibMeal("st_s_jerky", "Jerky + banana", 17, 265, kind = "s"), veg = false),
        ),
        "d" to listOf(
            Pick(ProgramSpec.library[3], veg = false),
            Pick(ProgramSpec.library[4], veg = false),
            Pick(ProgramSpec.library[5], veg = false),
            Pick(LibMeal("st_d_stirfry", "Chicken stir-fry + rice", 52, 650, kind = "d"), veg = false),
            Pick(LibMeal("st_d_tempeh", "Tempeh + quinoa bowl", 41, 630, kind = "d"), veg = true),
            Pick(LibMeal("st_d_burrito", "Egg + bean burrito", 40, 680, kind = "d"), veg = true),
        ),
    )

    fun options(slot: String, veg: Boolean): List<LibMeal> = meals[slot].orEmpty().filter { !veg || it.veg }.map { it.meal }

    fun meal(id: String): LibMeal? = meals.values.flatten().firstOrNull { it.meal.id == id }?.meal

    /**
     * One pick per slot that is on, chosen so the day's protein lands nearest [protein] without going far past
     * [calories]. Brute force: four slots of at most six meals.
     */
    fun fit(slots: List<String>, veg: Boolean, protein: Int, calories: Int): Map<String, String> {
        val lists = slots.map { s -> s to options(s, veg) }.filter { it.second.isNotEmpty() }
        var best: List<Int>? = null
        var bestScore = Double.MAX_VALUE
        fun walk(i: Int, picked: List<Int>) {
            if (i == lists.size) {
                val ms = picked.mapIndexed { k, j -> lists[k].second[j] }
                val p = ms.sumOf { it.protein }; val c = ms.sumOf { it.calories }
                // Index sum breaks ties toward the earlier, sample-first picks.
                val score = abs(p - protein) + (c - calories).coerceAtLeast(0) / 20.0 + picked.sum() * 0.01
                if (score < bestScore) { bestScore = score; best = picked }
                return
            }
            for (j in lists[i].second.indices) walk(i + 1, picked + j)
        }
        walk(0, emptyList())
        return best?.mapIndexed { k, j -> lists[k].first to lists[k].second[j].id }?.toMap().orEmpty()
    }

    /**
     * The slots a new plan starts with: all of them, unless the day cannot get down to a small target with all four
     * and dropping the snack lands closer. The user can switch it back on.
     */
    fun startingSlots(slots: List<String>, veg: Boolean, protein: Int, calories: Int): List<String> {
        fun off(on: List<String>) = abs(fit(on, veg, protein, calories).values.sumOf { meal(it)?.protein ?: 0 } - protein)
        val lighter = slots - "s"
        return if (lighter.size < slots.size && off(lighter) < off(slots) && off(slots) > 8) lighter else slots
    }

    /** Where the lifts happen; each picks the block for a training day and for an off day. Rest days never lift. */
    data class Gear(val id: String, val label: String, val train: List<String>, val off: List<String>)

    val gear = listOf(
        Gear("mix", "Gym, and dumbbells at home", listOf("dips", "pushups"), listOf("roller", "legraise", "floorpress", "pullover")),
        Gear("dumbbells", "Home, with dumbbells", listOf("pushups", "floorpress"), listOf("goblet", "dbrow", "roller")),
        Gear("bodyweight", "Bodyweight only", listOf("pushups", "pike"), listOf("squat", "legraise", "plank")),
        Gear("none", "No lifting", emptyList(), emptyList()),
    )

    val extraLifts = listOf(
        Exercise("pike", "Pike push-up", Block.CLIMB, 3, "6 to 10", 0.0, "", bwLabel = "Body", kind = "Push"),
        Exercise("goblet", "Goblet squat", Block.OFF, 3, "10 to 12", 5.0, "Lb", kind = "Legs"),
        Exercise("dbrow", "One-arm DB row", Block.OFF, 3, "8 to 12", 5.0, "Lb / side", kind = "Pull"),
        Exercise("squat", "Air squat", Block.OFF, 3, "15 to 20", 0.0, "", bwLabel = "Body", kind = "Legs"),
        Exercise("plank", "Plank", Block.OFF, 3, "30 to 45 s", 0.0, "", bwLabel = "Hold", kind = "Core"),
    )

    fun lift(id: String): Exercise? = (ProgramSpec.exercises + extraLifts).firstOrNull { it.id == id }

    /**
     * [s] with the plan made from onboarding's answers: the slots that are on, each defaulting to its pick (null keeps
     * the slot's default), every starter meal for those slots in the library, and the blocks for [gearId].
     */
    fun apply(s: Settings, on: List<String>, picks: Map<String, String>, veg: Boolean, gearId: String): Settings {
        val g = gear.firstOrNull { it.id == gearId } ?: gear[0]
        val meals = s.meals.map { m ->
            val pick = picks[m.id]?.let(::meal) ?: return@map m
            m.copy(sub = pick.name, protein = pick.protein, calories = pick.calories, defaultLib = pick.id)
        }
        val library = on.flatMap { options(it, veg) }.ifEmpty { s.library }
        val slots = meals.map { it.id }.filter { it in on }
        val needed = (g.train + g.off).mapNotNull(::lift)
        val exercises = (s.exercises + needed).distinctBy { it.id }
        val plans = s.plans.mapValues { (mode, p) ->
            when (mode) {
                DayMode.CLIMB -> p.copy(slots = slots, lifts = g.train)
                DayMode.OFF -> p.copy(slots = slots, lifts = g.off)
                DayMode.REST -> p.copy(slots = slots)
            }
        }
        return s.copy(meals = meals, library = library, exercises = exercises, plans = plans)
    }
}
