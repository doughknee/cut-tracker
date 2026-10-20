package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Settings
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Targets moves default flags with the route's numbers and leaves hand-set flags and skips alone. */
class FollowFlagsTest {
    private val start = LocalDate.of(2026, 10, 14)
    private val base = Settings(onboarded = true, startDate = start, startWeight = 182.0, targetWeight = 170.0, lengthWeeks = 12)
        .let { it.copy(checkpoints = ProgramSpec.defaultCheckpoints(start, 182.0, 170.0, 12)) }

    @Test fun defaultFlagsFollowAShorterRoute() {
        val moved = ProgramSpec.followFlags(base, base.copy(lengthWeeks = 8))
        assertEquals(ProgramSpec.defaultCheckpoints(start, 182.0, 170.0, 8).map { it.date }, moved.checkpoints.map { it.date })
        assertTrue(moved.checkpoints.none { LocalDate.parse(it.date).isAfter(moved.endDate) })
    }

    @Test fun handSetFlagsStayAndSkipsSurvive() {
        val skip = Checkpoint("2026-11-26", free = true, label = "Thanksgiving", note = "")
        val edited = base.copy(checkpoints = base.checkpoints.mapIndexed { i, cp -> if (i == 0) cp.copy(target = 181.0) else cp } + skip)
        val after = ProgramSpec.followFlags(edited, edited.copy(lengthWeeks = 8))
        assertEquals(edited.checkpoints, after.checkpoints)
        val untouched = base.copy(checkpoints = base.checkpoints + skip)
        assertTrue(skip in ProgramSpec.followFlags(untouched, untouched.copy(targetWeight = 168.0)).checkpoints)
    }
}
