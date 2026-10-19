package com.relentnet.cuttracker.engine

import com.relentnet.cuttracker.data.Block
import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.DayLog
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.LibMeal
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

/** SKIP is a planned day off (set from the calendar): chain kept, no freeze spent, not a Send. */
enum class DayResult { SEND, MIN, FREEZE, SKIP, MISSED, TODAY, FUTURE, BEFORE }

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

    /** Checkpoint "Week N" label: the nearest whole week since the start, so a checkpoint 13 days in reads Week 2. */
    fun checkpointWeek(cp: Checkpoint, start: LocalDate, weeks: Int): Int =
        ((ChronoUnit.DAYS.between(start, cp.localDate).toInt() + 3) / 7).coerceIn(1, weeks)

    // Script

    fun script(mode: DayMode?, s: Settings, picks: Map<String, String> = emptyMap()): List<ScriptItem> {
        if (mode == null) return emptyList()
        val weigh = ScriptItem(ProgramSpec.ITEM_WEIGH, "Weigh in", "Before food. Log it, ignore the number.", "8:30", ProgramSpec.XP_WEIGH)
        val workout = when (mode) {
            DayMode.CLIMB -> listOf(
                ScriptItem(ProgramSpec.ITEM_CLIMB, "Climb", "Climbing session. Before lunch.", "11:00", ProgramSpec.XP_CLIMB, workout = true),
                ScriptItem(ProgramSpec.ITEM_PUSH, "Push block", "Dips 3x, push-ups 2x. Last 15 min at the gym.", "12:15", ProgramSpec.XP_PUSH, workout = true),
            )
            DayMode.OFF -> listOf(
                ScriptItem(ProgramSpec.ITEM_CORE, "Core + push block", "Ab roller, leg raises, floor press, pullover. 15 min at home.", "any", ProgramSpec.XP_CORE, workout = true),
            )
            DayMode.REST -> emptyList()
        }
        val meals = s.meals.map { m -> mealItem(m, picks[m.id], s) }
        return if (meals.isEmpty()) listOf(weigh) + workout
        else listOf(weigh, meals.first()) + workout + meals.drop(1)
    }

    private fun mealItem(m: Meal, ref: String?, s: Settings): ScriptItem {
        val ch = resolveChoice(m, ref, s)
        val sub = if (ch.protein > 0) "${ch.name}. ${ch.protein}g" else ch.name
        return ScriptItem(m.id, m.name, sub, m.time, m.xp, ch.protein, ch.calories, pick = true)
    }

    /** What a slot logs: a library meal, a one-off custom entry, or the slot's own numbers. */
    data class MealChoice(val ref: String?, val name: String, val protein: Int, val calories: Int, val recipe: String = "")

    private fun LibMeal.choice() = MealChoice("lib:$id", name, protein, calories, recipe)

    fun libMeal(id: String?, s: Settings): LibMeal? = if (id == null) null else s.library.firstOrNull { it.id == id }

    fun defaultChoice(slot: Meal, s: Settings): MealChoice =
        libMeal(slot.defaultLib, s)?.choice() ?: MealChoice(null, slot.sub, slot.protein, slot.calories)

    fun resolveChoice(slot: Meal, ref: String?, s: Settings): MealChoice = when {
        ref == null -> defaultChoice(slot, s)
        ref.startsWith("lib:") -> libMeal(ref.removePrefix("lib:"), s)?.choice() ?: defaultChoice(slot, s)
        ref.startsWith("custom:") -> {
            val parts = ref.removePrefix("custom:").split(':', limit = 3)
            MealChoice(ref, parts.getOrNull(2)?.ifBlank { null } ?: "Something else", parts.getOrNull(0)?.toIntOrNull() ?: 0, parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        else -> defaultChoice(slot, s)
    }

    fun customRef(protein: Int, calories: Int, name: String) =
        "custom:$protein:$calories:" + name.trim().replace(';', ' ').replace('=', ' ').replace(':', ' ')

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

    /**
     * Trend in lb per day: a least-squares line through the raw readings of the last [window] days. The fit does the
     * smoothing, so it does not lag the way a line through the 7-day averages would. Null until seven readings spanning
     * at least six days exist; fewer than that is noise dressed up as a trend.
     */
    const val MIN_RATE_READINGS = 7

    fun rate(weighIns: List<WeighIn>, today: LocalDate, window: Int = 14): Double? {
        val recent = weighIns.filter { !it.date.isAfter(today) && !it.date.isBefore(today.minusDays(window.toLong() - 1)) }.sortedBy { it.date }
        if (recent.size < MIN_RATE_READINGS || ChronoUnit.DAYS.between(recent.first().date, recent.last().date) < 6) return null
        val pts = recent.map { w -> ChronoUnit.DAYS.between(today, w.date).toDouble() to w.lb }
        val mx = pts.map { it.first }.average()
        val my = pts.map { it.second }.average()
        val sxx = pts.sumOf { (it.first - mx) * (it.first - mx) }
        if (sxx == 0.0) return null
        return pts.sumOf { (it.first - mx) * (it.second - my) } / sxx
    }

    /** Where the average lands on [date] at the current [rate]; null without a rate or an average. */
    fun projectedAt(weighIns: List<WeighIn>, today: LocalDate, date: LocalDate): Double? {
        val avg = sevenDayAverage(weighIns, today) ?: return null
        val r = rate(weighIns, today) ?: return null
        return avg + r * ChronoUnit.DAYS.between(today, date)
    }

    // Chain, freezes, results

    private fun resultOf(log: DayLog?, weighed: Boolean, s: Settings): DayResult {
        if (log?.mode == null) return if (weighed) MISSED_UNLESS_MIN else DayResult.MISSED
        val items = script(log.mode, s, log.picks)
        return when {
            isSend(items, log.doneItemIds) -> DayResult.SEND
            isMin(items, log.doneItemIds, s.protein) -> DayResult.MIN
            else -> DayResult.MISSED
        }
    }

    private val MISSED_UNLESS_MIN = DayResult.MISSED

    /**
     * Result for every program day from start through [today]. Freezes: two per calendar month, auto-applied to the
     * first two missed days of that month. Today is TODAY unless already Min or Send. Planned skips are SKIP.
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
                s.checkpoints.any { it.free && it.covers(d) } -> DayResult.SKIP
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
     * Chain = count of Send, Min, Freeze and Skip days. It never resets: a missed day pauses it, the next good day
     * continues from the same number.
     */
    fun chain(results: Map<LocalDate, DayResult>, today: LocalDate): ChainState {
        val count = results.values.count { it == DayResult.SEND || it == DayResult.MIN || it == DayResult.FREEZE || it == DayResult.SKIP }
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
        s.exercises.filter { it.block == block && !it.phase2Only }

    /** The most recent session (weight, reps) for an exercise before or on [date], excluding [date] when [excludeDate]. */
    fun lastSession(sets: List<LiftSet>, exerciseId: String, before: LocalDate): Pair<Double, Int>? {
        val last = sets.filter { it.exerciseId == exerciseId && it.date.isBefore(before) }.maxByOrNull { it.date } ?: return null
        return last.weightLb to last.reps
    }

    /** The last [n] sessions of one exercise before [before], oldest first, as (weight, reps). */
    fun sessions(sets: List<LiftSet>, exerciseId: String, before: LocalDate, n: Int = 4): List<Pair<Double, Int>> =
        sets.filter { it.exerciseId == exerciseId && it.date.isBefore(before) }
            .groupBy { it.date }.toSortedMap().values.map { s -> s.first().weightLb to s.first().reps }.takeLast(n)

    fun beat(now: Pair<Double, Int>, last: Pair<Double, Int>?): Boolean =
        last != null && (now.first > last.first || now.second > last.second)

    /** True once any exercise session in the week of [day] beat its previous session. */
    /** A workout is a Climb day with the push block checked or an Off day with the core block checked. */
    fun isWorkout(log: DayLog): Boolean = when (log.mode) {
        DayMode.CLIMB -> ProgramSpec.ITEM_PUSH in log.doneItemIds
        DayMode.OFF -> ProgramSpec.ITEM_CORE in log.doneItemIds
        else -> false
    }

    /** Workouts logged in the Monday-to-Sunday week of [day]. */
    fun workoutsInWeek(logs: List<DayLog>, day: LocalDate): Int {
        val ws = weekStart(day)
        val we = ws.plusDays(6)
        return logs.count { !it.date.isBefore(ws) && !it.date.isAfter(we) && isWorkout(it) }
    }

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
