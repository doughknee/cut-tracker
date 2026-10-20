package com.relentnet.cuttracker.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.relentnet.cuttracker.data.AppState
import com.relentnet.cuttracker.data.DayMode
import com.relentnet.cuttracker.data.ProgramSpec
import com.relentnet.cuttracker.data.Repo
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

sealed interface Takeover {
    data object Send : Takeover
    data class LevelUp(val level: Int) : Takeover
}

class AppViewModel(val repo: Repo) : ViewModel() {
    val state: StateFlow<AppState> = repo.state.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())

    /** Wall-clock minute, for the 7pm state. */
    val nowTime = MutableStateFlow(LocalTime.now())

    var takeover by mutableStateOf<Takeover?>(null)
        private set
    data class ToastMsg(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null)
    var toast by mutableStateOf<ToastMsg?>(null)
        private set
    /** A past program day being edited on Today; null means today. */
    var viewDate by mutableStateOf<java.time.LocalDate?>(null)

    /** Tab a notification asked for; the nav consumes it. */
    var pendingTab by mutableStateOf<String?>(null)
    fun requestTab(tab: String) { pendingTab = tab }
    /** Text shared into the app from another app; the New meal sheet consumes it. */
    var pendingShare by mutableStateOf<String?>(null)
    /** The weigh-in reminder asked for Today with the weigh-in row open. */
    var pendingWeigh by mutableStateOf(false)
    private var toastJob: kotlinx.coroutines.Job? = null
    private val pendingLevelUps = ArrayDeque<Int>()

    init {
        viewModelScope.launch {
            repo.events.collect { e ->
                when (e) {
                    // Let the hold fill green on the wall before the takeover covers it.
                    is UiEvent.Send -> viewModelScope.launch {
                        delay(520)
                        // A level-up from the same tick showed in the gap; it comes back after the Send.
                        (takeover as? Takeover.LevelUp)?.let { pendingLevelUps.addFirst(it.level) }
                        takeover = Takeover.Send
                    }
                    is UiEvent.LevelUp -> if (takeover == null) takeover = Takeover.LevelUp(e.level) else pendingLevelUps.addLast(e.level)
                    is UiEvent.Badge -> showToast("${e.name} badge earned")
                    is UiEvent.Toast -> showToast(e.text)
                }
            }
        }
        viewModelScope.launch {
            while (true) { nowTime.value = LocalTime.now(); delay(15_000) }
        }
    }

    fun dismissTakeover() {
        takeover = null
        pendingLevelUps.removeFirstOrNull()?.let { lvl ->
            viewModelScope.launch { delay(250); takeover = Takeover.LevelUp(lvl) }
        }
    }

    /** Plain toasts that arrived while another plain one showed; each gets its turn. An Undo toast never waits. */
    private val queuedToasts = ArrayDeque<ToastMsg>()

    fun dismissToast() { toastJob?.cancel(); toast = null; queuedToasts.removeFirstOrNull()?.let(::show) }

    fun showToast(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
        val msg = ToastMsg(text, action, onAction)
        if (action == null && toast != null) queuedToasts.addLast(msg) else show(msg)
    }

    private fun show(msg: ToastMsg) {
        toast = msg
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(if (msg.action != null) 6000 else 1800)
            toast = null
            if (queuedToasts.isNotEmpty()) { delay(200); queuedToasts.removeFirstOrNull()?.let(::show) }
        }
    }

    /** Applies [f] and offers Undo, which puts the settings back as they were just before. For deletes. */
    fun updateWithUndo(text: String, f: (Settings) -> Settings) = viewModelScope.launch {
        val before = repo.settings.current()
        repo.updateSettings(f)
        val after = repo.settings.current()
        showToast(text, "Undo") { viewModelScope.launch { repo.updateSettings { it.restoreFrom(before, after) } } }
    }

    /** Deletes a library meal (past days keep what they logged) and offers Undo. */
    fun deleteMeal(m: com.relentnet.cuttracker.data.LibMeal) = viewModelScope.launch {
        val before = repo.settings.current()
        repo.replaceLibraryMeal(m, null)
        val after = repo.settings.current()
        showToast("${m.name} deleted", "Undo") { viewModelScope.launch { repo.updateSettings { it.restoreFrom(before, after) } } }
    }

    /** Lift numbers typed on Today, keyed "<date>:<exercise id>", so closing the panel or tapping elsewhere keeps them. */
    val liftDraft = androidx.compose.runtime.mutableStateMapOf<String, Pair<Double, Int>>()

    fun opened() = viewModelScope.launch { repo.opened() }
    fun setMode(mode: DayMode?, preCheck: Set<String> = emptySet(), date: java.time.LocalDate? = null) = viewModelScope.launch { repo.setMode(mode, preCheck, date) }
    fun toggleItem(id: String, date: java.time.LocalDate? = null) = viewModelScope.launch { repo.toggleItem(id, date) }
    fun pickMeal(slotId: String, ref: String?, date: java.time.LocalDate? = null) = viewModelScope.launch { repo.pickMeal(slotId, ref, date) }
    fun saveLibraryMeal(m: com.relentnet.cuttracker.data.LibMeal) = viewModelScope.launch { repo.saveLibraryMeal(m) }
    fun logWeight(lb: Double, date: java.time.LocalDate? = null) = viewModelScope.launch {
        repo.logWeight(lb, date)
        showToast("Logged ${com.relentnet.cuttracker.engine.ProgramEngine.fmt1(lb)} · +${ProgramSpec.XP_WEIGH} xp")
    }
    fun saveLiftSession(entries: List<Repo.LiftEntry>, tickBlock: String? = null) = viewModelScope.launch { repo.saveLiftSession(entries, tickBlock) }
    fun savePhoto(path: String) = viewModelScope.launch { repo.savePhoto(path) }
    fun deletePhoto(date: java.time.LocalDate) = viewModelScope.launch { repo.deletePhoto(date) }
    fun dismissWelcomeBack() = viewModelScope.launch { repo.dismissWelcomeBack() }
    /** Every settings edit goes through here: [f] gets the latest stored settings, never a copy a screen held. */
    fun updateSettings(then: () -> Unit = {}, f: (Settings) -> Settings) = viewModelScope.launch { repo.updateSettings(f); then() }
    fun resetProgram(then: () -> Unit = {}) = viewModelScope.launch { repo.resetProgram(); then() }
    fun seedSample() = viewModelScope.launch { repo.seedSample() }
    fun replaceLibraryMeal(old: com.relentnet.cuttracker.data.LibMeal, new: com.relentnet.cuttracker.data.LibMeal?) = viewModelScope.launch { repo.replaceLibraryMeal(old, new) }

    companion object {
        fun factory(repo: Repo): ViewModelProvider.Factory = viewModelFactory { initializer { AppViewModel(repo) } }
    }
}

/** Undo for a delete: each list a delete can touch goes back to [before] only if the delete changed it ([after]). */
private fun Settings.restoreFrom(before: Settings, after: Settings) = copy(
    library = if (after.library != before.library) before.library else library,
    meals = if (after.meals != before.meals) before.meals else meals,
    plans = if (after.plans != before.plans) before.plans else plans,
    exercises = if (after.exercises != before.exercises) before.exercises else exercises,
    liftKinds = if (after.liftKinds != before.liftKinds) before.liftKinds else liftKinds,
    checkpoints = if (after.checkpoints != before.checkpoints) before.checkpoints else checkpoints,
    customLooks = if (after.customLooks != before.customLooks) before.customLooks else customLooks,
    palette = if (after.palette != before.palette) before.palette else palette,
)
