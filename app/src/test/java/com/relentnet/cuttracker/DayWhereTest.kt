package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.DayPlan
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.ui.beta.dayWhere
import org.junit.Assert.assertEquals
import org.junit.Test

/** The sentence Today's cards and the day pages share follows the plan's lifts, not a fixed script. */
class DayWhereTest {
    private val s = Settings()

    @Test fun restDayWithNoLiftsAsksNothingElse() {
        assertEquals("Weigh in and eat the script. Nothing else is asked.", dayWhere(DayMode.REST, s))
    }

    @Test fun restDayWithALiftGetsABlock() {
        val withLift = s.copy(plans = s.plans + (DayMode.REST to DayPlan(lifts = listOf(s.exercises.first().id))))
        assertEquals("Weigh in and eat the script. A short block at home.", dayWhere(DayMode.REST, withLift))
    }

    @Test fun climbDayWithoutLiftsSaysNoBlock() {
        val bare = s.copy(plans = s.plans + (DayMode.CLIMB to DayPlan(slots = s.plans[DayMode.CLIMB]!!.slots)))
        assertEquals("Session at the gym. No block.", dayWhere(DayMode.CLIMB, bare))
    }
}
