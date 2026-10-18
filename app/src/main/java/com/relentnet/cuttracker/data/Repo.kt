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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

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
    val project: WeeklyProject? = null,
    val projectProgress: Int = 0,
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
    val projectSpec: ProjectSpec? get() = project?.let { p -> ProgramSpec.projects.firstOrNull { it.id == p.id } }
    val missedRun: Int get() = ProgramEngine.missedRunBefore(results, today)

    /** Everything Today needs about one program day, past or present. */
    data class DayView(
        val date: LocalDate, val log: DayLog, val items: List<ScriptItem>, val totals: ProgramEngine.Totals,
        val isMin: Boolean, val isSend: Boolean, val result: DayResult,
    )

    fun day(date: LocalDate): DayView {
        val l = logs.firstOrNull { it.date == date } ?: DayLog(date)
        val it = ProgramEngine.script(l.mode, settings, l.dinnerIndex)
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
    data class Send(val bonus: Int) : UiEvent
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
    const val PHOTO = "photo"
    const val PROJECT = "project"
}

object BadgeIds {
    const val FIRST_SEND = "first_send"
    const val CAME_BACK = "came_back"
    fun chain(n: Int) = "chain_$n"

    data class Spec(val id: String, val name: String)

    /** Badge shelf order: 4 across. */
    val all: List<Spec> = listOf(Spec(FIRST_SEND, "First Send")) +
        ProgramSpec.CHAIN_BADGES.map { Spec(chain(it), "$it Chain") } +
        Spec(CAME_BACK, "Came Back") +
        ProgramSpec.projects.map { Spec(it.badgeId, it.badgeName) }

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
        db.badges().observeAll(), db.projects().observeAll(), db.liftSets().observeAll(), db.photos().observeAll(),
    ) { arr ->
        val s = arr[0] as Settings
        val today = arr[1] as LocalDate
        val logs = arr[2] as List<DayLog>
        val weighIns = arr[3] as List<WeighIn>
        val xpEvents = arr[4] as List<XpEvent>
        val badges = arr[5] as List<Badge>
        val projects = arr[6] as List<WeeklyProject>
        val liftSets = arr[7] as List<LiftSet>
        val photos = arr[8] as List<ProgressPhoto>
        val log = logs.firstOrNull { it.date == today } ?: DayLog(today)
        val items = ProgramEngine.script(log.mode, s, log.dinnerIndex)
        val results = ProgramEngine.results(logs, weighIns, s, today)
        val ws = ProgramEngine.weekStart(today)
        val project = projects.firstOrNull { it.weekStart == ws }
        AppState(
            settings = s, today = today, log = log, items = items,
            totals = ProgramEngine.totals(items, log.doneItemIds),
            isMin = ProgramEngine.isMin(items, log.doneItemIds, s.protein),
            isSend = ProgramEngine.isSend(items, log.doneItemIds),
            xp = xpEvents.sumOf { it.amount }, results = results,
            chain = ProgramEngine.chain(results, today), sends = ProgramEngine.sends(results),
            weighIns = weighIns, liftSets = liftSets, badges = badges.map { it.id }.toSet(), photos = photos,
            project = project, projectProgress = project?.let { projectProgress(it, logs, weighIns, liftSets, xpEvents, s, today) } ?: 0,
            xpEvents = xpEvents, logs = logs, loaded = true,
        )
    }

    // Writes

    suspend fun setMode(mode: DayMode?, preCheck: Set<String> = emptySet(), date: LocalDate? = null) = write(date) { s, d ->
        val log = db.dayLogs().get(d) ?: DayLog(d)
        val keep = if (mode == null) emptySet() else {
            val mealIds = s.meals.map { it.id }.toSet() + ProgramSpec.ITEM_WEIGH
            log.doneItemIds.filter { it in mealIds }.toSet()
        }
        db.dayLogs().upsert(log.copy(mode = mode, doneItemIds = keep + preCheck, dinnerIndex = if (mode == null) null else log.dinnerIndex))
    }

    suspend fun toggleItem(id: String, dinnerDefault: Boolean = false, date: LocalDate? = null) = write(date) { s, d ->
        val log = db.dayLogs().get(d) ?: DayLog(d)
        val done = if (id in log.doneItemIds) log.doneItemIds - id else log.doneItemIds + id
        val meal = s.meals.firstOrNull { it.id == id }
        val dinner = if (meal?.pick == true && id in done && log.dinnerIndex == null && dinnerDefault) 0 else log.dinnerIndex
        db.dayLogs().upsert(log.copy(doneItemIds = done, dinnerIndex = dinner))
    }

    suspend fun pickDinner(mealId: String, index: Int, date: LocalDate? = null) = write(date) { _, d ->
        val log = db.dayLogs().get(d) ?: DayLog(d)
        db.dayLogs().upsert(log.copy(doneItemIds = log.doneItemIds + mealId, dinnerIndex = index))
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
        events.tryEmit(UiEvent.Toast("+$gained xp saved"))
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

    /** Called on every foreground: rolls the date, awards Came Back, remembers the open. */
    suspend fun opened() {
        refreshToday()
        write { s, today ->
            val results = ProgramEngine.results(db.dayLogs().all(), db.weighIns().all(), s, today)
            if (ProgramEngine.missedRunBefore(results, today) >= 2 && s.lastOpenDate != today) awardBadge(BadgeIds.CAME_BACK)
            if (s.lastOpenDate != today) settings.update { it.copy(lastOpenDate = today) }
        }
    }

    suspend fun setPhase(phase: Int) = write { _, _ -> settings.update { it.copy(phase = phase) } }

    suspend fun saveSettings(s: Settings) = write { _, _ -> settings.save(s) }

    suspend fun resetProgram() = lock.withLock {
        db.dayLogs().clear(); db.weighIns().clear(); db.liftSets().clear(); db.photos().clear()
        db.xp().clear(); db.badges().clear(); db.projects().clear()
        photosDir.listFiles()?.forEach { it.delete() }
        settings.clear()
        refreshToday()
    }

    // Internals

    /** Runs [block] for [date] (today when null, never a future day), then reconciles that day's XP. */
    private suspend fun write(date: LocalDate? = null, block: suspend (Settings, LocalDate) -> Unit) = lock.withLock {
        val s = settings.current()
        val today = ProgramEngine.programDay(LocalDateTime.now(), s.resetHour)
        todayFlow.value = today
        val d = date?.takeIf { it.isBefore(today) } ?: today
        val xpBefore = db.xp().total()
        block(s, d)
        syncDay(d)
        afterWrite(xpBefore)
    }

    /**
     * Reconciles the XP ledger for [date] with its DayLog: one `item` event per done item, `send` when all items are
     * done (plus one `bonus` roll, kept until the Send is undone), `min` when it is a Min day but not a Send.
     */
    private suspend fun syncDay(date: LocalDate) {
        val s = settings.current()
        val log = db.dayLogs().get(date) ?: DayLog(date)
        val items = ProgramEngine.script(log.mode, s, log.dinnerIndex)
        val existing = db.xp().forDate(date)
        val wanted = LinkedHashMap<Pair<String, String?>, Int>()
        for (it in items) if (it.id in log.doneItemIds) wanted[XpKind.ITEM to it.id] = it.xp
        val send = ProgramEngine.isSend(items, log.doneItemIds)
        val min = ProgramEngine.isMin(items, log.doneItemIds, s.protein)
        if (send) wanted[XpKind.SEND to null] = ProgramSpec.XP_SEND
        if (min && !send) wanted[XpKind.MIN to null] = ProgramSpec.XP_MIN
        val managed = setOf(XpKind.ITEM, XpKind.SEND, XpKind.MIN, XpKind.BONUS)
        val stale = existing.filter { e ->
            e.kind in managed && when (e.kind) {
                XpKind.BONUS -> !send
                else -> (e.kind to e.ref) !in wanted
            }
        }
        if (stale.isNotEmpty()) db.xp().deleteIds(stale.map { it.id })
        val have = existing.filter { it.id !in stale.map { s -> s.id } }.map { it.kind to it.ref }.toSet()
        val now = Instant.now()
        val add = wanted.filter { (k, _) -> k !in have }.map { (k, amt) -> XpEvent(date = date, at = now, kind = k.first, amount = amt, ref = k.second) }
        if (add.isNotEmpty()) db.xp().insertAll(add)
        if (send && existing.none { it.kind == XpKind.BONUS } && add.any { it.kind == XpKind.SEND }) {
            val bonus = ProgramEngine.bonus(Random.nextDouble())
            db.xp().insert(XpEvent(date = date, at = now, kind = XpKind.BONUS, amount = bonus, ref = null))
            // A late check earns the XP; the takeover is for the live moment only.
            if (date == todayFlow.value) events.tryEmit(UiEvent.Send(bonus))
        }
    }

    private suspend fun afterWrite(xpBefore: Int) {
        val s = settings.current()
        val today = todayFlow.value
        val logs = db.dayLogs().all()
        val weighIns = db.weighIns().all()
        val results = ProgramEngine.results(logs, weighIns, s, today)
        val chain = ProgramEngine.chain(results, today)
        if (ProgramEngine.sends(results) >= 1) awardBadge(BadgeIds.FIRST_SEND)
        for (n in ProgramSpec.CHAIN_BADGES) if (chain.count >= n) awardBadge(BadgeIds.chain(n))
        syncProject(s, today, logs, weighIns)
        val xpAfter = db.xp().total()
        val before = ProgramEngine.level(xpBefore)
        val after = ProgramEngine.level(xpAfter)
        if (after > before) events.tryEmit(UiEvent.LevelUp(after))
    }

    private suspend fun awardBadge(id: String) {
        if (db.badges().insert(Badge(id, Instant.now())) != -1L) events.tryEmit(UiEvent.Badge(id, BadgeIds.name(id)))
    }

    /** Ensures this week has a project (random, never last week's), then scores it and pays out once. */
    private suspend fun syncProject(s: Settings, today: LocalDate, logs: List<DayLog>, weighIns: List<WeighIn>) {
        val ws = ProgramEngine.weekStart(today)
        var p = db.projects().get(ws)
        if (p == null) {
            val prev = db.projects().previous(ws)?.id
            val pool = ProgramSpec.projects.filter { it.id != prev }
            p = WeeklyProject(ws, pool.random().id)
            db.projects().upsert(p)
        }
        val spec = ProgramSpec.projects.firstOrNull { it.id == p.id } ?: return
        val progress = projectProgress(p, logs, weighIns, db.liftSets().all(), db.xp().all(), s, today)
        val done = p.done || progress >= spec.target
        if (progress != p.progress || done != p.done) db.projects().upsert(p.copy(progress = progress, done = done))
        if (done && !p.done) {
            db.xp().insert(XpEvent(date = today, at = Instant.now(), kind = XpKind.PROJECT, amount = ProgramSpec.XP_PROJECT, ref = ws.toString()))
            awardBadge(spec.badgeId)
        }
    }

    /** Progress toward the week's project, from the week's rows. */
    fun projectProgress(
        p: WeeklyProject, logs: List<DayLog>, weighIns: List<WeighIn>, liftSets: List<LiftSet>, xpEvents: List<XpEvent>,
        s: Settings, today: LocalDate,
    ): Int {
        val ws = p.weekStart
        val we = ws.plusDays(6)
        fun inWeek(d: LocalDate) = !d.isBefore(ws) && !d.isAfter(we)
        val weekLogs = logs.filter { inWeek(it.date) }
        return when (p.id) {
            "dips3" -> liftSets.filter { inWeek(it.date) && it.exerciseId == "dips" && it.setIndex == 0 }
                .count { ProgramEngine.beat(it.weightLb to it.reps, ProgramEngine.lastSession(liftSets, "dips", it.date)) }
            "weigh7" -> weighIns.count { inWeek(it.date) }
            "sends3" -> {
                val results = ProgramEngine.results(logs, weighIns, s, today)
                var best = 0; var run = 0
                var d = ws
                while (!d.isAfter(we)) { if (results[d] == DayResult.SEND) { run++; best = maxOf(best, run) } else run = 0; d = d.plusDays(1) }
                best
            }
            "push2" -> weekLogs.count { it.mode == DayMode.CLIMB && ProgramSpec.ITEM_PUSH in it.doneItemIds }
            "protein6" -> weekLogs.count { ProgramEngine.totals(ProgramEngine.script(it.mode, s, it.dinnerIndex), it.doneItemIds).protein >= s.protein }
            "offnoon3" -> xpEvents.count {
                it.kind == XpKind.ITEM && it.ref == ProgramSpec.ITEM_CORE && inWeek(it.date) && it.at.atZone(zone).toLocalTime().isBefore(LocalTime.NOON)
            }
            else -> 0
        }
    }
}
