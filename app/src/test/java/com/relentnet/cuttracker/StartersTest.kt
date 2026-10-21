package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.Starters
import com.relentnet.cuttracker.engine.ProgramEngine
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Onboarding's starter plan: picks that land near the target, vegetarian means vegetarian, and the script follows. */
class StartersTest {
    private val all = listOf("b", "l", "s", "d")

    private fun protein(picks: Map<String, String>) = picks.values.sumOf { Starters.meal(it)!!.protein }

    @Test fun theSampleTargetKeepsTheSampleDay() {
        val picks = Starters.fit(all, veg = false, protein = 169, calories = 2120)
        assertEquals(mapOf("b" to "lib_b", "l" to "lib_l", "s" to "lib_s", "d" to "lib_d1"), picks)
    }

    @Test fun aSmallerTargetGetsASmallerDay() {
        // Four meals cannot go under the lightest four; that floor is what a small target gets.
        val floor = all.sumOf { slot -> Starters.options(slot, false).minOf { it.protein } }
        assertTrue(protein(Starters.fit(all, veg = false, protein = 100, calories = 1500)) <= floor + 6)
        for (target in listOf(140, 155)) {
            val picks = Starters.fit(all, veg = false, protein = target, calories = 1900)
            assertTrue(abs(protein(picks) - target) <= 8, "target $target got ${protein(picks)}")
        }
        val three = Starters.fit(listOf("b", "l", "d"), veg = false, protein = 120, calories = 1600)
        assertEquals(setOf("b", "l", "d"), three.keys)
        assertTrue(abs(protein(three) - 120) <= 8, "three meals got ${protein(three)}")
    }

    @Test fun aSmallTargetStartsWithoutTheSnack() {
        assertEquals(listOf("b", "l", "d"), Starters.startingSlots(all, veg = false, protein = 120, calories = 1700))
        assertEquals(all, Starters.startingSlots(all, veg = false, protein = 165, calories = 2250))
    }

    @Test fun vegetarianPicksAreAllVegetarian() {
        val veg = Starters.meals.values.flatten().filter { it.veg }.map { it.meal.id }.toSet()
        val picks = Starters.fit(all, veg = true, protein = 150, calories = 2000)
        assertEquals(4, picks.size)
        assertTrue(picks.values.all { it in veg })
        for (slot in all) assertTrue(Starters.options(slot, veg = true).isNotEmpty(), "$slot has a vegetarian option")
    }

    @Test fun applyMakesTheScriptTheirs() {
        val picks = Starters.fit(listOf("b", "l", "d"), veg = true, protein = 120, calories = 1700)
        val s = Starters.apply(Settings(), listOf("b", "l", "d"), picks, veg = true, gearId = "bodyweight")
        val script = ProgramEngine.script(DayMode.OFF, s)
        assertTrue(script.none { it.id == "s" }, "the snack is off")
        assertEquals(protein(picks), script.sumOf { it.protein })
        val block = ProgramEngine.exercisesFor(DayMode.OFF, s).map { it.id }
        assertEquals(listOf("squat", "legraise", "plank"), block)
        assertTrue(s.library.all { Starters.meals.values.flatten().first { p -> p.meal.id == it.id }.veg })
        assertTrue(ProgramEngine.script(DayMode.CLIMB, s).any { it.id == ProgramSpec.ITEM_CLIMB }, "the session stays")
    }

    @Test fun noLiftingMeansNoBlock() {
        val s = Starters.apply(Settings(), all, emptyMap(), veg = false, gearId = "none")
        assertTrue(ProgramEngine.blockItem(DayMode.CLIMB, s) == null && ProgramEngine.blockItem(DayMode.OFF, s) == null)
        assertEquals(Settings().meals, s.meals, "no picks keeps every slot as it was")
    }
}
