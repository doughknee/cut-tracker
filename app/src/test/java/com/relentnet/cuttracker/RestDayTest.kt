package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.DayLog
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestDayTest {
    private val s = Settings()
    private val start = LocalDate.of(2026, 10, 14)
    private fun d(n: Int) = start.plusDays((n - 1).toLong())

    @Test fun restScriptIsWeighInPlusMeals() {
        val items = ProgramEngine.script(DayMode.REST, s, 0)
        assertEquals(listOf("w", "b", "l", "s", "d"), items.map { it.id })
        assertEquals(45, ProgramEngine.totalXp(items))
        assertTrue(items.none { it.workout })
    }

    @Test fun fullRestDayIsASend() {
        val log = DayLog(d(1), DayMode.REST, setOf("w", "b", "l", "s", "d"), 0)
        val r = ProgramEngine.results(listOf(log), emptyList(), s, d(2))
        assertEquals(DayResult.SEND, r[d(1)])
        // without dinner it is still a Min day: weigh in + 123g is under 165, so not safe; add lunch+breakfast+snack+dinner needed
        val half = DayLog(d(1), DayMode.REST, setOf("w", "b", "l"), 0)
        assertFalse(ProgramEngine.isMin(ProgramEngine.script(DayMode.REST, s, 0), half.doneItemIds, s.protein))
    }

    @Test fun workoutsCountPushAndCoreOnly() {
        // Week of Mon Oct 12 to Sun Oct 18
        val logs = listOf(
            DayLog(LocalDate.of(2026, 10, 14), DayMode.CLIMB, setOf("w", "c", "p")),   // counts
            DayLog(LocalDate.of(2026, 10, 15), DayMode.CLIMB, setOf("w", "c")),        // climbed, no push block: no
            DayLog(LocalDate.of(2026, 10, 16), DayMode.OFF, setOf("w", "k")),          // counts
            DayLog(LocalDate.of(2026, 10, 17), DayMode.REST, setOf("w", "b", "l")),    // never counts
            DayLog(LocalDate.of(2026, 10, 19), DayMode.OFF, setOf("k")),               // next week
        )
        assertEquals(2, ProgramEngine.workoutsInWeek(logs, LocalDate.of(2026, 10, 18)))
        assertEquals(1, ProgramEngine.workoutsInWeek(logs, LocalDate.of(2026, 10, 19)))
        assertTrue(ProgramEngine.isWorkout(logs[0]))
        assertFalse(ProgramEngine.isWorkout(logs[3]))
    }
}
