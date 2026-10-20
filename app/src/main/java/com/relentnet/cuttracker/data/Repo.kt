package com.relentnet.cuttracker.data

import android.content.Context
import com.relentnet.cuttracker.engine.ChainState
import com.relentnet.cuttracker.engine.DayResult
import com.relentnet.cuttracker.engine.ProgramEngine
import com.relentnet.cuttracker.engine.ScriptItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Everything the screens read, derived once per change. */
data class AppState(
    val settings: Settings = Settings(),
    val today: LocalDate = ProgramSpec.PHASE_1_START,
    val log: DayLog = DayLog(ProgramSpec.PHASE_1_START),
    val items: List<ScriptItem> = emptyList(),
    val totals: ProgramEngine.Totals = ProgramEngine.Totals(0, 0, 0, 0),
    val isMin: Boolean = false,
    val isSend: Boolean = false,
    val xp: Int = 0,
    val results: Map<LocalDate, DayResult> = emptyMap(),
    val chain: ChainState = ChainState(0, false, false, 2),
    val sends: Int = 0,
    val weighIns: List<WeighIn> = emptyList(),
    val liftSets: List<LiftSet> = emptyList(),
    val badges: Set<String> = emptySet(),
    val photos: List<ProgressPhoto> = emptyList(),
    val xpEvents: List<XpEvent> = emptyList(),
    val logs: List<DayLog> = emptyList(),
    val loaded: Boolean = false,
) {
    val dayN: Int get() = ProgramEngine.dayNumber(today, settings.startDate)
    val level: Int get() = ProgramEngine.level(xp)
    val weekN: Int get() = ProgramEngine.weekOf(today, settings.startDate).coerceAtLeast(1)
    val mode: DayMode? get() = log.mode
    val done: Set<String> get() = log.doneItemIds
    val freeToday: Boolean get() = settings.checkpoints.any { it.free && it.covers(today) }
    val lastWeighIn: WeighIn? get() = weighIns.maxByOrNull { it.date }
    val missedRun: Int get() = ProgramEngine.missedRunBefore(results, today)
    val workoutsThisWeek: Int get() = ProgramEngine.workoutsInWeek(logs, today)
    /** Past the last program day: the route is topped out until a new one starts. */
    val ended: Boolean get() = settings.onboarded && !today.isBefore(settings.endDate)

    /** Everything Today needs about one program day, past or present. */
    data class DayView(
        val date: LocalDate, val log: DayLog, val items: List<ScriptItem>, val totals: ProgramEngine.Totals,
        val isMin: Boolean, val isSend: Boolean, val result: DayResult,
    )

    fun day(date: LocalDate): DayView {
        val l = logs.firstOrNull { it.date == date } ?: DayLog(date)
        val it = ProgramEngine.script(l.mode, settings, l.picks)
        return DayView(
            date, l, it, ProgramEngine.totals(it, l.doneItemIds),
            ProgramEngine.isMin(it, l.doneItemIds, settings.protein), ProgramEngine.isSend(it, l.doneItemIds),
            results[date] ?: DayResult.FUTURE,
        )
    }

    /** The most recent reading before [date]; the inline weigh-in prefills from it. */
    fun weighInBefore(date: LocalDate): WeighIn? = weighIns.filter { it.date.isBefore(date) }.maxByOrNull { it.date }
}

sealed interface UiEvent {
    data object Send : UiEvent
    data class LevelUp(val level: Int) : UiEvent
    data class Badge(val id: String, val name: String) : UiEvent
    data class Toast(val text: String) : UiEvent
}

object XpKind {
    const val ITEM = "item"
    const val SEND = "send"
    const val BONUS = "bonus"
    const val MIN = "min"
    const val LIFT_SESSION = "lift_session"
    const val LIFT_BEAT = "lift_beat"
    const val WEEK_WON = "week_won"
    const val WORKOUT_TARGET = "workout_target"
    const val PHOTO = "photo"
    const val PROJECT = "project"
}

object BadgeIds {
    const val FIRST_SEND = "first_send"
    fun chain(n: Int) = "chain_$n"

    data class Spec(val id: String, val name: String)

    /** Badge shelf order: the first Send, then the chain milestones. */
    val all: List<Spec> = listOf(Spec(FIRST_SEND, "First Send")) + ProgramSpec.CHAIN_BADGES.map { Spec(chain(it), "$it Chain") }

    fun name(id: String) = all.firstOrNull { it.id == id }?.name ?: id
}

/**
 * The single write path. Every mutation ends in [syncDay] so the XP ledger is always reproducible from DayLog, then
 * [afterWrite] which awards badges, level-ups and project progress. Reads are one combined [state] flow.
 */
class Repo(context: Context, val db: CutDb, val settings: SettingsStore) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    val events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    private val todayFlow = MutableStateFlow(LocalDate.now())
    val zone: ZoneId get() = ZoneId.systemDefault()
    val photosDir = context.filesDir.resolve("photos").apply { mkdirs() }

    init {
        scope.launch { refreshToday() }
        scope.launch {
            while (true) {
                val s = settings.current()
                val now = LocalDateTime.now()
                var next = now.toLocalDate().atTime(LocalTime.of(s.resetHour, 0))
                if (!next.isAfter(now)) next = next.plusDays(1)
                delay(Duration.between(now, next).toMillis().coerceAtLeast(1000))
                refreshToday()
            }
        }
    }

    suspend fun today(): LocalDate = ProgramEngine.programDay(LocalDateTime.now(), settings.current().resetHour)

    suspend fun refreshToday() { todayFlow.value = today() }

    @Suppress("UNCHECKED_CAST")
    val state: Flow<AppState> = combine(
        settings.flow, todayFlow, db.dayLogs().observeAll(), db.weighIns().observeAll(), db.xp().observeAll(),
        db.badges().observeAll(), db.liftSets().observeAll(), db.photos().observeAll(),
    ) { arr ->
        val s = arr[0] as Settings
        val today = arr[1] as LocalDate
        val logs = arr[2] as List<DayLog>
        val weighIns = arr[3] as List<WeighIn>
        val xpEvents = arr[4] as List<XpEvent>
        val badges = arr[5] as List<Badge>
        val liftSets = arr[6] as List<LiftSet>
        val photos = arr[7] as List<ProgressPhoto>
        val log = logs.firstOrNull { it.date == today } ?: DayLog(today)
        val items = ProgramEngine.script(log.mode, s, log.picks)
        val results = ProgramEngine.results(logs, weighIns, s, today, ProgramEngine.scoredDays(xpEvents))
        AppState(
            settings = s, today = today, log = log, items = items,
            totals = ProgramEngine.totals(items, log.doneItemIds),
            isMin = ProgramEngine.isMin(items, log.doneItemIds, s.protein),
            isSend = ProgramEngine.isSend(items, log.doneItemIds),
            xp = xpEvents.sumOf { it.amount }, results = results,
            chain = ProgramEngine.chain(results, today), sends = ProgramEngine.sends(results),
            weighIns = weighIns, liftSets = liftSets, badges = badges.map { it.id }.toSet(), photos = photos,
            xpEvents = xpEvents, logs = logs, loaded = true,
        )
    }

    // Writes

    suspend fun setMode(mode: DayMode?, preCheck: Set<String> = emptySet(), date: LocalDate? = null) = write(date, rescore = true) { s, d ->
        val log = db.dayLogs().get(d) ?: DayLog(d)
        // "Change" (null) keeps every tick so picking the same kind again loses nothing; a kind keeps what its script has.
        val keep = if (mode == null) log.doneItemIds else {
            val ids = ProgramEngine.script(mode, s, log.picks).map { it.id }.toSet() + s.meals.map { it.id } + ProgramSpec.ITEM_WEIGH
            log.doneItemIds.filter { it in ids }.toSet()
        }
        db.dayLogs().upsert(log.copy(mode = mode, doneItemIds = keep + preCheck, picks = log.picks))
    }

    /** Checks or unchecks an item. A meal checked this way logs the slot's current pick, or its default. */
    suspend fun toggleItem(id: String, date: LocalDate? = null) = write(date, rescore = isTicked(id, date)) { _, d ->
        val log = db.dayLogs().get(d) ?: DayLog(d)
        val done = if (id in log.doneItemIds) log.doneItemIds - id else log.doneItemIds + id
        db.dayLogs().upsert(log.copy(doneItemIds = done))
    }

    /** Logs [ref] ("lib:<id>" or "custom:...", null = the default) for a meal slot and checks it. */
    suspend fun pickMeal(slotId: String, ref: String?, date: LocalDate? = null) = write(date) { _, d ->
        val log = db.dayLogs().get(d) ?: DayLog(d)
        val picks = if (ref == null) log.picks - slotId else log.picks + (slotId to ref)
        db.dayLogs().upsert(log.copy(doneItemIds = log.doneItemIds + slotId, picks = picks))
    }

    /**
     * Changes or deletes ([new] = null) a library meal. Past days that logged [old], by pick or as their slot's default,
     * keep its numbers as a one-off entry first, so history and This week do not move.
     */
    suspend fun replaceLibraryMeal(old: LibMeal, new: LibMeal?) = write { s, today ->
        if (new == null || new.protein != old.protein || new.calories != old.calories) {
            val ref = "lib:${old.id}"
            val frozen = ProgramEngine.customRef(old.protein, old.calories, old.name)
            val byDefault = s.meals.filter { it.defaultLib == old.id }.map { it.id }.toSet()
            for (log in db.dayLogs().all()) {
                if (!log.date.isBefore(today)) continue
                val picks = log.picks.mapValues { (_, v) -> if (v == ref) frozen else v } +
                    byDefault.filter { it in log.doneItemIds && it !in log.picks }.associateWith { frozen }
                if (picks != log.picks) db.dayLogs().upsert(log.copy(picks = picks))
            }
        }
        settings.update { st ->
            if (new == null) st.copy(library = st.library.filter { it.id != old.id }, meals = st.meals.map { if (it.defaultLib == old.id) it.copy(defaultLib = null) else it })
            else st.copy(library = st.library.map { if (it.id == old.id) new else it })
        }
    }

    suspend fun saveLibraryMeal(m: LibMeal) = write { _, _ ->
        settings.update { it.copy(library = it.library.filter { x -> x.id != m.id } + m) }
    }

    suspend fun logWeight(lb: Double, date: LocalDate? = null) = write(date) { _, d ->
        db.weighIns().upsert(WeighIn(d, lb))
        val log = db.dayLogs().get(d) ?: DayLog(d)
        db.dayLogs().upsert(log.copy(doneItemIds = log.doneItemIds + ProgramSpec.ITEM_WEIGH))
    }

    data class LiftEntry(val exerciseId: String, val weightLb: Double, val reps: Int, val sets: Int)

    suspend fun saveLiftSession(entries: List<LiftEntry>) = write { s, today ->
        val before = db.liftSets().all()
        var gained = 0
        if (db.xp().count(XpKind.LIFT_SESSION, today.toString()) == 0) {
            db.xp().insert(XpEvent(date = today, at = Instant.now(), kind = XpKind.LIFT_SESSION, amount = ProgramSpec.XP_LIFT_SESSION, ref = today.toString()))
            gained += ProgramSpec.XP_LIFT_SESSION
        }
        for (e in entries) {
            val last = ProgramEngine.lastSession(before, e.exerciseId, today)
            db.liftSets().deleteFor(today, e.exerciseId)
            db.liftSets().insertAll((0 until e.sets).map { LiftSet(date = today, exerciseId = e.exerciseId, weightLb = e.weightLb, reps = e.reps, setIndex = it) })
            if (ProgramEngine.beat(e.weightLb to e.reps, last) && db.xp().count(XpKind.LIFT_BEAT, "$today:${e.exerciseId}") == 0) {
                db.xp().insert(XpEvent(date = today, at = Instant.now(), kind = XpKind.LIFT_BEAT, amount = ProgramSpec.XP_LIFT_BEAT, ref = "$today:${e.exerciseId}"))
                gained += ProgramSpec.XP_LIFT_BEAT
            }
        }
        val ws = ProgramEngine.weekStart(today)
        if (ProgramEngine.weekWon(db.liftSets().all(), today) && db.xp().count(XpKind.WEEK_WON, ws.toString()) == 0) {
            db.xp().insert(XpEvent(date = today, at = Instant.now(), kind = XpKind.WEEK_WON, amount = ProgramSpec.XP_WEEK_WON, ref = ws.toString()))
            gained += ProgramSpec.XP_WEEK_WON
        }
        events.tryEmit(UiEvent.Toast(if (gained > 0) "Session saved · +$gained xp" else "Session updated"))
    }

    suspend fun savePhoto(path: String) = write { _, today ->
        db.photos().upsert(ProgressPhoto(today, path))
        if (db.xp().count(XpKind.PHOTO, today.toString()) == 0) {
            db.xp().insert(XpEvent(date = today, at = Instant.now(), kind = XpKind.PHOTO, amount = ProgramSpec.XP_PHOTO, ref = today.toString()))
        }
        events.tryEmit(UiEvent.Toast("Photo saved · +${ProgramSpec.XP_PHOTO} xp"))
    }

    suspend fun deletePhoto(date: LocalDate) = write { _, _ ->
        db.photos().all().firstOrNull { it.date == date }?.let { java.io.File(it.path).delete() }
        db.photos().delete(date)
    }

    suspend fun dismissWelcomeBack() = write { _, today -> settings.update { it.copy(welcomeBackSeen = today) } }

    /** Runs [block] under the write lock in one transaction: a restore either lands whole or leaves the data as it was. */
    suspend fun replaceAll(block: suspend () -> Unit) = lock.withLock { db.withTransaction { block() } }

    /** Called on every foreground: rolls the date, awards Came Back, remembers the open. */
    suspend fun opened() {
        refreshToday()
        write { s, today ->
            val results = ProgramEngine.results(db.dayLogs().all(), db.weighIns().all(), s, today, ProgramEngine.scoredDays(db.xp().all()))
            if (s.lastOpenDate != today) settings.update { it.copy(lastOpenDate = today) }
        }
    }

    /** Applies [f] to the stored settings inside the write lock, so two quick edits never undo each other. */
    suspend fun updateSettings(f: (Settings) -> Settings) = write { _, _ -> settings.update(f) }

    /**
     * A week of made-up logs for store screenshots, through the real write paths so XP and results come out right.
     * Debug builds only; MainActivity gates it on the debuggable flag and an intent extra.
     * Right after an install the first run races first-launch setup and leaves onboarding on; force-stop and run it again.
     */
    suspend fun seedSample() {
        resetProgram()
        val today = today()
        // Six days behind today, so a Sunday "today" shows the whole week on the wall.
        val start = today.minusDays(6)
        val d = Settings()
        settings.save(d.copy(
            onboarded = true, tourSeen = true, startDate = start, startWeight = 182.4, targetWeight = 170.0,
            checkpoints = ProgramSpec.defaultCheckpoints(start, 182.4, 170.0, d.lengthWeeks),
        ))
        refreshToday()
        val s = settings.current()
        fun script(mode: DayMode) = ProgramEngine.script(mode, s).map { it.id }
        // Send is the whole script; Min is the script without the block. Mon Send, Tue Min, Wed missed (the month's
        // first freeze), Thu Send, Fri Send, Sat Min, and today an Off day with two lines done.
        val week = listOf(
            DayMode.REST to script(DayMode.REST), DayMode.OFF to script(DayMode.OFF) - ProgramEngine.blockItemId(DayMode.OFF), null,
            DayMode.REST to script(DayMode.REST), DayMode.CLIMB to script(DayMode.CLIMB), DayMode.OFF to script(DayMode.OFF) - ProgramEngine.blockItemId(DayMode.OFF),
            DayMode.OFF to listOf(ProgramSpec.ITEM_WEIGH, s.meals.first().id),
        )
        for ((i, day) in week.withIndex()) {
            val date = start.plusDays(i.toLong())
            if (day == null) continue
            val (mode, done) = day
            setMode(mode, date = date)
            logWeight(182.4 - i * 0.3, date)
            for (id in done - ProgramSpec.ITEM_WEIGH) toggleItem(id, date)
        }
    }

    suspend fun resetProgram() = lock.withLock {
        db.dayLogs().clear(); db.weighIns().clear(); db.liftSets().clear(); db.photos().clear()
        db.xp().clear(); db.badges().clear(); db.projects().clear()
        photosDir.listFiles()?.forEach { it.delete() }
        settings.clear()
        refreshToday()
    }

    // Internals

    /** Runs [block] for [date] (today when null, never a future day), then reconciles that day's XP. */
    /** Whether [id] is ticked on [date] (today when null): unticking is the one edit that may lower a past day. */
    private suspend fun isTicked(id: String, date: LocalDate?): Boolean =
        id in (db.dayLogs().get(date?.takeIf { it.isBefore(today()) } ?: today())?.doneItemIds ?: emptySet())

    /**
     * [rescore] lets a past day's Send or Min be taken away by this write (an untick, a new kind). Without it a past
     * day can only move up, so fixing a weigh-in typo or ticking a forgotten snack never re-judges it by today's targets.
     */
    private suspend fun write(date: LocalDate? = null, rescore: Boolean = false, block: suspend (Settings, LocalDate) -> Unit) = lock.withLock {
        val s = settings.current()
        val today = ProgramEngine.programDay(LocalDateTime.now(), s.resetHour)
        todayFlow.value = today
        val d = date?.takeIf { it.isBefore(today) } ?: today
        val xpBefore = db.xp().total()
        block(s, d)
        syncDay(d, keepScore = !rescore && d.isBefore(today))
        afterWrite(xpBefore)
    }

    /**
     * Reconciles the XP ledger for [date] with its DayLog: one `item` event per done item, `send` when all items are
     * done, `min` when it is a Min day but not a Send.
     */
    private suspend fun syncDay(date: LocalDate, keepScore: Boolean = false) {
        val s = settings.current()
        val log = db.dayLogs().get(date) ?: DayLog(date)
        val items = ProgramEngine.script(log.mode, s, log.picks)
        val existing = db.xp().forDate(date)
        val wanted = LinkedHashMap<Pair<String, String?>, Int>()
        for (it in items) if (it.id in log.doneItemIds) wanted[XpKind.ITEM to it.id] = it.xp
        val send = ProgramEngine.isSend(items, log.doneItemIds)
        val min = ProgramEngine.isMin(items, log.doneItemIds, s.protein)
        if (send) wanted[XpKind.SEND to null] = ProgramSpec.XP_SEND
        if (min && !send) wanted[XpKind.MIN to null] = ProgramSpec.XP_MIN
        if (keepScore) {
            // A past day keeps the score it has unless this write earns a better one.
            if (existing.any { it.kind == XpKind.SEND }) { wanted.remove(XpKind.MIN to null); wanted[XpKind.SEND to null] = ProgramSpec.XP_SEND }
            else if (existing.any { it.kind == XpKind.MIN } && !send) wanted[XpKind.MIN to null] = ProgramSpec.XP_MIN
        }
        val managed = setOf(XpKind.ITEM, XpKind.SEND, XpKind.MIN)
        val stale = existing.filter { e -> e.kind in managed && (e.kind to e.ref) !in wanted }
        if (stale.isNotEmpty()) db.xp().deleteIds(stale.map { it.id })
        val have = existing.filter { it.id !in stale.map { s -> s.id } }.map { it.kind to it.ref }.toSet()
        val now = Instant.now()
        val add = wanted.filter { (k, _) -> k !in have }.map { (k, amt) -> XpEvent(date = date, at = now, kind = k.first, amount = amt, ref = k.second) }
        if (add.isNotEmpty()) db.xp().insertAll(add)
        // A late check earns the XP; the takeover is for the live moment only.
        if (send && add.any { it.kind == XpKind.SEND } && date == todayFlow.value) events.tryEmit(UiEvent.Send)
    }

    private suspend fun afterWrite(xpBefore: Int) {
        val s = settings.current()
        val today = todayFlow.value
        val logs = db.dayLogs().all()
        val weighIns = db.weighIns().all()
        val results = ProgramEngine.results(logs, weighIns, s, today, ProgramEngine.scoredDays(db.xp().all()))
        val chain = ProgramEngine.chain(results, today)
        if (ProgramEngine.sends(results) >= 1) awardBadge(BadgeIds.FIRST_SEND)
        for (n in ProgramSpec.CHAIN_BADGES) if (chain.count >= n) awardBadge(BadgeIds.chain(n))
        val ws = ProgramEngine.weekStart(today)
        if (ProgramEngine.workoutsInWeek(logs, today) >= s.workoutsPerWeek && db.xp().count(XpKind.WORKOUT_TARGET, ws.toString()) == 0) {
            db.xp().insert(XpEvent(date = today, at = Instant.now(), kind = XpKind.WORKOUT_TARGET, amount = ProgramSpec.XP_WORKOUT_TARGET, ref = ws.toString()))
            events.tryEmit(UiEvent.Toast("Workout target hit · +${ProgramSpec.XP_WORKOUT_TARGET} xp"))
        }
        val xpAfter = db.xp().total()
        val before = ProgramEngine.level(xpBefore)
        val after = ProgramEngine.level(xpAfter)
        if (after > before) events.tryEmit(UiEvent.LevelUp(after))
    }

    private suspend fun awardBadge(id: String) {
        if (db.badges().insert(Badge(id, Instant.now())) != -1L) events.tryEmit(UiEvent.Badge(id, BadgeIds.name(id)))
    }
}
