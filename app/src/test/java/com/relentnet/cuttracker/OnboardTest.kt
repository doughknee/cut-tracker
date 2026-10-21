package com.relentnet.cuttracker

import com.relentnet.cuttracker.ui.onboarding.Onboard
import com.relentnet.cuttracker.ui.onboarding.Onboard.Pace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The defaults onboarding fills in and the pace it says back. */
class OnboardTest {
    @Test fun theGoalIsHalfAPercentAWeekRounded() {
        assertEquals(171.0, Onboard.goalFor(181.8, 12, kg = false))
        assertEquals(77.5, Onboard.goalFor(82.5, 12, kg = true))
        assertEquals(Pace.STEADY, Onboard.pace(181.8, Onboard.goalFor(181.8, 12, false), 12))
    }

    @Test fun targetsComeFromTheWeights() {
        assertEquals(155, Onboard.proteinFor(171.0))
        assertEquals(2200, Onboard.caloriesFor(181.8))
        assertEquals(80, Onboard.proteinFor(50.0))
        assertEquals(4000, Onboard.caloriesFor(500.0))
    }

    @Test fun thePaceIsNamedByPercentAWeek() {
        assertEquals(Pace.UP, Onboard.pace(180.0, 180.0, 12))
        assertEquals(Pace.UP, Onboard.pace(180.0, 190.0, 12))
        assertEquals(Pace.GENTLE, Onboard.pace(180.0, 176.0, 12))
        assertEquals(Pace.STEADY, Onboard.pace(181.8, 170.0, 12))
        assertEquals(Pace.BRISK, Onboard.pace(200.0, 180.0, 12))
        assertEquals(Pace.FAST, Onboard.pace(200.0, 170.0, 4))
    }

    @Test fun theFixForAFastCutLandsAtOnePercent() {
        val n = Onboard.weeksForOnePercent(200.0, 170.0)
        assertEquals(15, n)
        assertFalse(Onboard.pace(200.0, 170.0, n) == Pace.FAST)
        assertEquals(Pace.FAST, Onboard.pace(200.0, 170.0, n - 1))
        assertEquals(52, Onboard.weeksForOnePercent(400.0, 100.0))
    }

    @Test fun aWeightInTheWrongUnitIsCaught() {
        assertTrue(Onboard.weightOk(181.8, kg = false))
        assertFalse(Onboard.weightOk(82.5, kg = false))
        assertTrue(Onboard.weightOk(82.5, kg = true))
        assertFalse(Onboard.weightOk(400.0, kg = true))
    }
}
