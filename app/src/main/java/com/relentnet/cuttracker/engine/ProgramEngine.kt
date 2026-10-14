package com.relentnet.cuttracker.engine

import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.DayLog
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.Dinner
import com.relentnet.cuttracker.data.Exercise
import com.relentnet.cuttracker.data.LiftSet
import com.relentnet.cuttracker.data.Meal
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.WeighIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** One row of the Today script. */
data class ScriptItem(
    val id: String,
    val label: String,
    val sub: String,
    val time: String,
    val xp: Int,
    val protein: Int = 0,
    val calories: Int = 0,
    val pick: Boolean = false,
    val workout: Boolean = false,
)

enum class DayResult { SEND, MIN, FREEZE, MISSED, TODAY, FUTURE, BEFORE }

data class DaySummary(val date: LocalDate, val result: DayResult, val xp: Int, val mode: DayMode?, val done: Int, val total: Int, val weight: Double?)

data class ChainState(val count: Int, val paused: Boolean, val securedToday: Boolean, val freezesLeft: Int)

/** Every derived number in the app. Pure functions over Room rows and Settings; no clock, no IO. */
object ProgramEngine {

    // Day boundary

    fun programDay(now: LocalDateTime, resetHour: Int): LocalDate =
        if (now.hour < resetHour) now.toLocalDate().minusDays(1) else now.toLocalDate()

    fun dayNumber(day: LocalDate, start: LocalDate): Int = ChronoUnit.DAYS.between(start, day).toInt() + 1

    fun weekOf(day: LocalDate, start: LocalDate): Int = (ChronoUnit.DAYS.between(start, day).toInt() / 7) + 1

    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Checkpoint "Week N" label and anchor row: whole weeks elapsed since the start. */
    fun checkpointWeek(cp: Checkpoint, start: LocalDate, weeks: Int): Int =
        (ChronoUnit.DAYS.between(start, cp.localDate).toInt() / 7).coerceIn(1, weeks)

    // Script

    fun script(mode: DayMode?, s: Settings, dinnerIndex: Int?): List<ScriptItem> {
        if (mode == null) return emptyList()
        val weigh = ScriptItem(ProgramSpec.ITEM_WEIGH, "Weigh in", "Before food. Log it, ignore the number.", "8:30", ProgramSpec.XP_WEIGH)
        val workout = if (mode == DayMode.CLIMB) listOf(
            ScriptItem(ProgramSpec.ITEM_CLIMB, "Climb", "Climb Nashville East. Before lunch.", "11:00", ProgramSpec.XP_CLIMB, workout = true),
            ScriptItem(ProgramSpec.ITEM_PUSH, "Push block", "Dips 3x, push-ups 2x. Last 15 min at the gym.", "12:15", ProgramSpec.XP_PUSH, workout = true),
        ) else listOf(
            ScriptItem(ProgramSpec.ITEM_CORE, "Core + push block", "Ab roller, leg raises, floor press, pullover. 15 min at home.", "any", ProgramSpec.XP_CORE, workout = true),
        )
        val meals = s.meals.map { m -> mealItem(m, dinnerIndex) }
        return if (meals.isEmpty()) listOf(weigh) + workout
        else listOf(weigh, meals.first()) + workout + meals.drop(1)
    }

    private fun mealItem(m: Meal, dinnerIndex: Int?): ScriptItem {
        val d = if (m.pick) dinnerIndex?.let { ProgramSpec.dinners.getOrNull(it) } else null
        val sub = when {
            d != null -> "${d.name}. ${d.protein}g"
            m.protein > 0 -> "${m.sub}. ${m.protein}g"
            else -> m.sub
        }
        return ScriptItem(m.id, m.name, sub, m.time, m.xp, d?.protein ?: m.protein, d?.calories ?: (if (m.pick) 0 else m.calories), pick = m.pick)
    }

    fun totalXp(items: List<ScriptItem>) = items.sumOf { it.xp }

    data class Totals(val protein: Int, val calories: Int, val done: Int, val total: Int) {
        val all get() = total > 0 && done == total
    }

    fun totals(items: List<ScriptItem>, done: Set<String>): Totals {
        val d = items.filter { it.id in done }
        return Totals(d.sumOf { it.protein }, d.sumOf { it.calories }, d.size, items.size)
    }

    /** Min day: weighed in and protein at or above target. */
    fun isMin(items: List<ScriptItem>, done: Set<String>, proteinTarget: Int): Boolean =
        ProgramSpec.ITEM_WEIGH in done && totals(items, done).protein >= proteinTarget

    fun isSend(items: List<ScriptItem>, done: Set<String>): Boolean = items.isNotEmpty() && items.all { it.id in done }

    // XP and levels

    fun level(xp: Int): Int {
        var l = 0
        for (i in ProgramSpec.LEVELS.indices) if (xp >= ProgramSpec.LEVELS[i]) l = i
        return l
    }

    fun levelFloor(level: Int) = ProgramSpec.LEVELS[level.coerceIn(ProgramSpec.LEVELS.indices)]

    fun nextLevelXp(level: Int): Int? = ProgramSpec.LEVELS.getOrNull(level + 1)

    /** 0..1 progress inside the current grade; 1 when maxed. */
    fun levelProgress(xp: Int): Float {
        val l = level(xp)
        val floor = levelFloor(l)
        val next = nextLevelXp(l) ?: return 1f
        return ((xp - floor).toFloat() / (next - floor)).coerceIn(0f, 1f)
    }

    /** Bonus roll: 60% 0, 30% 25, 10% 100. [r] in [0,1). */
    fun bonus(r: Double): Int = when {
        r < 0.6 -> 0
        r < 0.9 -> 25
        else -> 100
    }

    // Weight

    /** Last 7 logged days until 7 exist, then the trailing 7 calendar days with gaps ignored. */
    fun sevenDayAverage(weighIns: List<WeighIn>, today: LocalDate): Double? {
        val sorted = weighIns.sortedBy { it.date }
        if (sorted.isEmpty()) return null
        val pool = if (sorted.size < 7) sorted
        else sorted.filter { !it.date.isBefore(today.minusDays(6)) && !it.date.isAfter(today) }.ifEmpty { sorted.takeLast(7) }
        return pool.map { it.lb }.average()
    }

    /** Linear line from start to target weight over the program; the "on pace" threshold at [dayN]. */
    fun paceWeight(dayN: Int, s: Settings): Double =
        s.startWeight - (s.startWeight - s.targetWeight) * dayN.toDouble() / s.totalDays

    fun onPace(avg: Double, dayN: Int, s: Settings): Boolean = avg <= paceWeight(dayN, s) + ProgramSpec.PACE_TOLERANCE_LB

    // Chain, freezes, results

    private fun resultOf(log: DayLog?, weighed: Boolean, s: Settings): DayResult {
        if (log?.mode == null) return if (weighed) MISSED_UNLESS_MIN else DayResult.MISSED
        val items = script(log.mode, s, log.dinnerIndex)
        return when {
            isSend(items, log.doneItemIds) -> DayResult.SEND
            isMin(items, log.doneItemIds, s.protein) -> DayResult.MIN
            else -> DayResult.MISSED
        }
    }

    private val MISSED_UNLESS_MIN = DayResult.MISSED

    /**
     * Result for every program day from start through [today]. Freezes: two per calendar month, auto-applied to the
     * first two missed days of that month. Today is TODAY unless already Min or Send. Free days count as a Send.
     */
    fun results(logs: List<DayLog>, weighIns: List<WeighIn>, s: Settings, today: LocalDate): Map<LocalDate, DayResult> {
        val byDate = logs.associateBy { it.date }
        val weighed = weighIns.map { it.date }.toSet()
        val out = LinkedHashMap<LocalDate, DayResult>()
        val freezesUsed = HashMap<YearMonth, Int>()
        var d = s.startDate
        val last = s.endDate.minusDays(1)
        while (!d.isAfter(last)) {
            val r: DayResult = when {
                d.isAfter(today) -> DayResult.FUTURE
                s.checkpoints.any { it.free && it.covers(d) } -> DayResult.SEND
                else -> resultOf(byDate[d], d in weighed, s)
            }
            out[d] = when {
                d == today -> if (r == DayResult.SEND || r == DayResult.MIN) r else DayResult.TODAY
                r == DayResult.MISSED -> {
                    val ym = YearMonth.from(d)
                    val used = freezesUsed[ym] ?: 0
                    if (used < ProgramSpec.FREEZES_PER_MONTH) { freezesUsed[ym] = used + 1; DayResult.FREEZE } else DayResult.MISSED
                }
                else -> r
            }
            d = d.plusDays(1)
        }
        return out
    }

    fun freezesLeft(results: Map<LocalDate, DayResult>, month: YearMonth): Int =
        (ProgramSpec.FREEZES_PER_MONTH - results.count { (d, r) -> YearMonth.from(d) == month && r == DayResult.FREEZE }).coerceAtLeast(0)

    /**
     * Chain = count of Send, Min and Freeze days. It never resets: a missed day pauses it, the next good day continues
     * from the same number.
     */
    fun chain(results: Map<LocalDate, DayResult>, today: LocalDate): ChainState {
        val count = results.values.count { it == DayResult.SEND || it == DayResult.MIN || it == DayResult.FREEZE }
        val todayR = results[today]
        val secured = todayR == DayResult.SEND || todayR == DayResult.MIN
        val lastDone = results.entries.lastOrNull { it.key.isBefore(today) }?.value
        val paused = !secured && lastDone == DayResult.MISSED
        return ChainState(count, paused, secured, freezesLeft(results, YearMonth.from(today)))
    }

    fun sends(results: Map<LocalDate, DayResult>) = results.values.count { it == DayResult.SEND }

    /** Consecutive missed (unfrozen) program days immediately before [today]. Drives the welcome-back card. */
    fun missedRunBefore(results: Map<LocalDate, DayResult>, today: LocalDate): Int {
        var n = 0
        var d = today.minusDays(1)
        while (results[d] == DayResult.MISSED) { n++; d = d.minusDays(1) }
        return n
    }

    // Lifts

    fun exercisesFor(block: Block, s: Settings): List<Exercise> =
        s.exercises.filter { it.block == block && (s.phase == 2 || !it.phase2Only) }

    fun setsFor(e: Exercise, phase: Int) = if (phase == 2) e.setsPhase2 ?: e.sets else e.sets

    /** The most recent session (weight, reps) for an exercise before or on [date], excluding [date] when [excludeDate]. */
    fun lastSession(sets: List<LiftSet>, exerciseId: String, before: LocalDate): Pair<Double, Int>? {
        val last = sets.filter { it.exerciseId == exerciseId && it.date.isBefore(before) }.maxByOrNull { it.date } ?: return null
        return last.weightLb to last.reps
    }

    fun beat(now: Pair<Double, Int>, last: Pair<Double, Int>?): Boolean =
        last != null && (now.first > last.first || now.second > last.second)

    /** True once any exercise session in the week of [day] beat its previous session. */
    fun weekWon(sets: List<LiftSet>, day: LocalDate): Boolean {
        val ws = weekStart(day)
        val we = ws.plusDays(6)
        val sessions = sets.filter { !it.date.isBefore(ws) && !it.date.isAfter(we) }.groupBy { it.date to it.exerciseId }
        return sessions.any { (k, v) -> beat(v.first().weightLb to v.first().reps, lastSession(sets, k.second, k.first)) }
    }

    // Photos

    fun photoDue(photoDates: List<LocalDate>, today: LocalDate, start: LocalDate): Boolean {
        if (today.isBefore(start)) return false
        val last = photoDates.maxOrNull() ?: return true
        return ChronoUnit.DAYS.between(last, today) >= ProgramSpec.PHOTO_INTERVAL_DAYS
    }

    // Misc formatting

    fun fmt1(v: Double) = ((v * 10).roundToInt() / 10.0).toString().let { if ('.' in it) it else "$it.0" }

    fun pct(dayN: Int, total: Int) = (dayN.toDouble() / total * 100).roundToInt().coerceIn(0, 100)
}
