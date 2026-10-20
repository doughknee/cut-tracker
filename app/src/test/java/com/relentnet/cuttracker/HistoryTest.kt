package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.DayLog
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.XpEvent
import com.relentnet.cuttracker.data.XpKind
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** History stays put: a past day keeps the result its ledger rows recorded, whatever the settings say today. */
class HistoryTest {
    private val s = Settings()
    private val start = LocalDate.of(2026, 10, 14)
    private fun d(n: Int) = start.plusDays((n - 1).toLong())
    private fun xp(date: LocalDate, kind: String) = XpEvent(date = date, at = Instant.EPOCH, kind = kind, amount = 1, ref = null)

    @Test fun pastSendSurvivesAHigherProteinTargetAndANewPlanItem() {
        val rest = ProgramEngine.script(DayMode.REST, s).map { it.id }.toSet()
        val logs = listOf(DayLog(d(1), DayMode.REST, rest), DayLog(d(2), DayMode.OFF, setOf("w", "b", "l", "s", "d")))
        val ledger = listOf(xp(d(1), XpKind.SEND), xp(d(2), XpKind.MIN))
        // Later: protein goes up and Rest days gain the core block.
        val later = s.copy(protein = 400, plans = s.plans + (DayMode.REST to s.plans.getValue(DayMode.REST).copy(lifts = listOf("roller"))))
        val live = ProgramEngine.results(logs, emptyList(), later, d(3))
        assertFalse(live[d(1)] == DayResult.SEND, "without the ledger the new plan re-scores the day")
        val kept = ProgramEngine.results(logs, emptyList(), later, d(3), ProgramEngine.scoredDays(ledger))
        assertEquals(DayResult.SEND, kept[d(1)])
        assertEquals(DayResult.MIN, kept[d(2)])
        assertEquals(2, ProgramEngine.chain(kept, d(3)).count)
        assertEquals(2, ProgramEngine.chain(kept, d(3)).freezesLeft)
    }

    @Test fun todayIsAlwaysLiveAndAMissingRowIsAMiss() {
        val rest = ProgramEngine.script(DayMode.REST, s).map { it.id }.toSet()
        val logs = listOf(DayLog(d(1), DayMode.REST, rest), DayLog(d(2), DayMode.REST, rest))
        val r = ProgramEngine.results(logs, emptyList(), s, d(2), ProgramEngine.scoredDays(emptyList()))
        assertEquals(DayResult.FREEZE, r[d(1)], "no ledger row: missed, then frozen")
        assertEquals(DayResult.SEND, r[d(2)], "today is scored live")
    }

    @Test fun sendWinsOverMinOnTheSameDay() {
        assertEquals(DayResult.SEND, ProgramEngine.scoredDays(listOf(xp(d(1), XpKind.SEND), xp(d(1), XpKind.MIN)))[d(1)])
        assertEquals(DayResult.SEND, ProgramEngine.scoredDays(listOf(xp(d(1), XpKind.MIN), xp(d(1), XpKind.SEND)))[d(1)])
    }

    @Test fun aLiftBlockOnAnyKindOfDayIsAWorkout() {
        assertTrue(ProgramEngine.isWorkout(DayLog(d(1), DayMode.REST, setOf("k"))))
        assertTrue(ProgramEngine.isWorkout(DayLog(d(1), DayMode.CLIMB, setOf("p"))))
        assertFalse(ProgramEngine.isWorkout(DayLog(d(1), DayMode.CLIMB, setOf("k"))))
        assertFalse(ProgramEngine.isWorkout(DayLog(d(1), null, setOf("k", "p"))))
    }
}
