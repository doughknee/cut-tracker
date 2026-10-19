package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.DayLog
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.WeighIn
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgramEngineTest {
    private val s = Settings()
    private val start = LocalDate.of(2026, 10, 14)
    private fun d(n: Int) = start.plusDays((n - 1).toLong())

    private fun sendLog(date: LocalDate, mode: DayMode = DayMode.CLIMB): DayLog {
        val items = ProgramEngine.script(mode, s).map { it.id }.toSet()
        return DayLog(date, mode, items)
    }

    private fun minLog(date: LocalDate) = DayLog(date, DayMode.OFF, setOf("w", "b", "l", "s", "d"))

    @Test fun rolloverAt0359StaysYesterday() {
        assertEquals(LocalDate.of(2026, 10, 14), ProgramEngine.programDay(LocalDateTime.of(2026, 10, 15, 3, 59), 4))
        assertEquals(LocalDate.of(2026, 10, 15), ProgramEngine.programDay(LocalDateTime.of(2026, 10, 15, 4, 0), 4))
        assertEquals(LocalDate.of(2026, 10, 15), ProgramEngine.programDay(LocalDateTime.of(2026, 10, 15, 1, 0), 0))
    }

    @Test fun dayNumberAndEnd() {
        assertEquals(1, ProgramEngine.dayNumber(start, start))
        assertEquals(84, ProgramEngine.dayNumber(LocalDate.of(2027, 1, 5), start))
        assertEquals(LocalDate.of(2027, 1, 6), s.endDate)
        assertEquals(84, s.totalDays)
        assertEquals(70, s.copy(lengthWeeks = 10).totalDays)
    }

    @Test fun sevenDayAverageUsesLoggedDaysUntilSevenExist() {
        val w = listOf(WeighIn(d(1), 181.8), WeighIn(d(2), 181.2))
        assertEquals(181.5, ProgramEngine.sevenDayAverage(w, d(2))!!, 1e-9)
        assertNull(ProgramEngine.sevenDayAverage(emptyList(), d(1)))
        // 9 entries with a gap: trailing 7 calendar days, gaps ignored
        val many = (1..9).filter { it != 8 }.map { WeighIn(d(it), 180.0 + it) }
        val avg = ProgramEngine.sevenDayAverage(many, d(9))!!
        // days 3..9 minus day 8: 183,184,185,186,187,189
        assertEquals(listOf(183.0, 184.0, 185.0, 186.0, 187.0, 189.0).average(), avg, 1e-9)
    }

    @Test fun paceLine() {
        assertEquals(181.8 - 11.8 / 84, ProgramEngine.paceWeight(1, s), 1e-9)
        assertEquals(181.8 - 11.8 * 42 / 84, ProgramEngine.paceWeight(42, s), 1e-9)
        assertEquals(170.0, ProgramEngine.paceWeight(84, s), 1e-9)
        assertTrue(ProgramEngine.onPace(181.5, 2, s))
        assertFalse(ProgramEngine.onPace(178.0, 84, s))
    }

    @Test fun levels() {
        assertEquals(0, ProgramEngine.level(0))
        assertEquals(0, ProgramEngine.level(299))
        assertEquals(1, ProgramEngine.level(300))
        assertEquals(3, ProgramEngine.level(1200))
        assertEquals(10, ProgramEngine.level(99999))
        assertEquals(1f, ProgramEngine.levelProgress(7500))
        assertEquals(0.5f, ProgramEngine.levelProgress(150))
    }

    @Test fun bonusRoll() {
        assertEquals(0, ProgramEngine.bonus(0.0))
        assertEquals(0, ProgramEngine.bonus(0.59))
        assertEquals(25, ProgramEngine.bonus(0.6))
        assertEquals(25, ProgramEngine.bonus(0.89))
        assertEquals(100, ProgramEngine.bonus(0.9))
    }

    @Test fun minVsSend() {
        val items = ProgramEngine.script(DayMode.CLIMB, s)
        assertEquals(7, items.size)
        assertEquals(100, ProgramEngine.totalXp(items))
        assertEquals(75, ProgramEngine.totalXp(ProgramEngine.script(DayMode.OFF, s)))
        assertFalse(ProgramEngine.isMin(items, setOf("w", "b"), 165))
        assertTrue(ProgramEngine.isMin(items, setOf("w", "b", "l", "s", "d"), 165))
        assertFalse(ProgramEngine.isSend(items, setOf("w", "b", "l", "s", "d")))
        assertTrue(ProgramEngine.isSend(items, items.map { it.id }.toSet()))
        assertEquals(48, ProgramEngine.totals(items, setOf("b")).protein)
        assertEquals(430, ProgramEngine.totals(items, setOf("b")).calories)
        // the slot's default meal supplies the numbers; a pick replaces them
        assertEquals(820, ProgramEngine.totals(items, setOf("d")).calories)
        val picked = ProgramEngine.script(DayMode.CLIMB, s, mapOf("d" to "lib:lib_d2"))
        assertEquals(760, ProgramEngine.totals(picked, setOf("d")).calories)
        assertEquals(44, ProgramEngine.totals(picked, setOf("d")).protein)
        val custom = ProgramEngine.script(DayMode.CLIMB, s, mapOf("l" to ProgramEngine.customRef(30, 400, "Pizza")))
        assertEquals("Pizza. 30g", custom.first { it.id == "l" }.sub)
        assertEquals(400, ProgramEngine.totals(custom, setOf("l")).calories)
        // an unknown library id falls back to the default
        assertEquals(820, ProgramEngine.totals(ProgramEngine.script(DayMode.CLIMB, s, mapOf("d" to "lib:gone")), setOf("d")).calories)
    }

    @Test fun chainWithNoMisses() {
        val logs = (1..5).map { sendLog(d(it)) }
        val r = ProgramEngine.results(logs, emptyList(), s, d(6))
        val c = ProgramEngine.chain(r, d(6))
        assertEquals(5, c.count)
        assertFalse(c.paused)
        assertEquals(DayResult.TODAY, r[d(6)])
        assertEquals(2, c.freezesLeft)
    }

    @Test fun oneMissIsFrozen() {
        val logs = (1..7).filter { it != 7 }.map { sendLog(d(it)) }  // day 7 = Oct 20 skipped
        val r = ProgramEngine.results(logs, emptyList(), s, d(8))
        assertEquals(DayResult.FREEZE, r[d(7)])
        val c = ProgramEngine.chain(r, d(8))
        assertEquals(7, c.count)
        assertFalse(c.paused)
        assertEquals(1, c.freezesLeft)
    }

    @Test fun threeMissesInAMonthPausesOnTheThird() {
        val missed = setOf(7, 9, 11)
        val logs = (1..11).filter { it !in missed }.map { if (it % 2 == 0) minLog(d(it)) else sendLog(d(it)) }
        val r = ProgramEngine.results(logs, emptyList(), s, d(12))
        assertEquals(DayResult.FREEZE, r[d(7)])
        assertEquals(DayResult.FREEZE, r[d(9)])
        assertEquals(DayResult.MISSED, r[d(11)])
        assertEquals(DayResult.MIN, r[d(10)])
        val c = ProgramEngine.chain(r, d(12))
        assertEquals(10, c.count)
        assertTrue(c.paused)
        assertEquals(0, c.freezesLeft)
        assertEquals(1, ProgramEngine.missedRunBefore(r, d(12)))
        // today becomes a Min day: chain picks right back up
        val r2 = ProgramEngine.results(logs + minLog(d(12)), emptyList(), s, d(12))
        val c2 = ProgramEngine.chain(r2, d(12))
        assertEquals(11, c2.count)
        assertFalse(c2.paused)
        assertTrue(c2.securedToday)
    }

    @Test fun freezesResetAtMonthBoundary() {
        // Oct 30, Oct 31 missed (both frozen), Nov 1 missed (frozen again: new month)
        val oct30 = LocalDate.of(2026, 10, 30)
        val logs = generateSequence(start) { it.plusDays(1) }.takeWhile { it.isBefore(oct30) }.map { sendLog(it) }.toList()
        val r = ProgramEngine.results(logs, emptyList(), s, LocalDate.of(2026, 11, 2))
        assertEquals(DayResult.FREEZE, r[oct30])
        assertEquals(DayResult.FREEZE, r[LocalDate.of(2026, 10, 31)])
        assertEquals(DayResult.FREEZE, r[LocalDate.of(2026, 11, 1)])
        assertEquals(0, ProgramEngine.freezesLeft(r, YearMonth.of(2026, 10)))
        assertEquals(1, ProgramEngine.freezesLeft(r, YearMonth.of(2026, 11)))
    }

    @Test fun freeDaysCountAsSend() {
        val free = s.copy(checkpoints = s.checkpoints + listOf(
            Checkpoint("2026-11-26", free = true, label = "Thanksgiving", note = ""),
            Checkpoint("2026-12-24", free = true, days = 2, label = "Christmas", note = ""),
        ))
        val r = ProgramEngine.results(emptyList(), emptyList(), free, LocalDate.of(2026, 12, 26))
        assertEquals(DayResult.SEND, r[LocalDate.of(2026, 11, 26)])
        assertEquals(DayResult.SEND, r[LocalDate.of(2026, 12, 24)])
        assertEquals(DayResult.SEND, r[LocalDate.of(2026, 12, 25)])
    }

    @Test fun checkpointWeeks() {
        val cps = ProgramSpec.defaultCheckpoints(start, 181.8, 170.0, 12)
        assertEquals(6, cps.size)
        assertEquals(2, ProgramEngine.checkpointWeek(cps[0], start, 12))
        assertEquals(4, ProgramEngine.checkpointWeek(cps[1], start, 12))
        assertEquals(8, ProgramEngine.checkpointWeek(cps[3], start, 12))
        assertEquals(12, ProgramEngine.checkpointWeek(cps[5], start, 12))
        assertEquals(170.0, cps[5].target)
        assertEquals(180.0, cps[0].target)
        // Odd lengths still end on the target.
        val five = ProgramSpec.defaultCheckpoints(start, 180.0, 175.0, 5)
        assertEquals(5, five.size)
        assertEquals(175.0, five.last().target)
        val seven = ProgramSpec.defaultCheckpoints(start, 180.0, 175.0, 7)
        assertEquals(listOf(2, 4, 6, 7), seven.map { ProgramEngine.checkpointWeek(it, start, 7) })
    }

    @Test fun photoCadence() {
        assertTrue(ProgramEngine.photoDue(emptyList(), start, start))
        assertFalse(ProgramEngine.photoDue(listOf(start), d(14), start))
        assertTrue(ProgramEngine.photoDue(listOf(start), d(15), start))
    }
}
