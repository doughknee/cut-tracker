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
    var toast by mutableStateOf<String?>(null)
        private set
    /** A past program day being edited on Today; null means today. */
    var viewDate by mutableStateOf<java.time.LocalDate?>(null)

    /** Tab a notification asked for; the nav consumes it. */
    var pendingTab by mutableStateOf<String?>(null)
    fun requestTab(tab: String) { pendingTab = tab }
    /** Text shared into the app from another app; the New meal sheet consumes it. */
    var pendingShare by mutableStateOf<String?>(null)
    private var toastJob: kotlinx.coroutines.Job? = null
    private val pendingLevelUps = ArrayDeque<Int>()

    init {
        viewModelScope.launch {
            repo.events.collect { e ->
                when (e) {
                    // Let the hold fill green on the wall before the takeover covers it.
                    is UiEvent.Send -> viewModelScope.launch { delay(520); takeover = Takeover.Send }
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

    fun showToast(text: String) {
        toast = text
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(1400); toast = null }
    }

    fun opened() = viewModelScope.launch { repo.opened() }
    fun setMode(mode: DayMode?, preCheck: Set<String> = emptySet(), date: java.time.LocalDate? = null) = viewModelScope.launch { repo.setMode(mode, preCheck, date) }
    fun toggleItem(id: String, date: java.time.LocalDate? = null) = viewModelScope.launch { repo.toggleItem(id, date) }
    fun pickMeal(slotId: String, ref: String?, date: java.time.LocalDate? = null) = viewModelScope.launch { repo.pickMeal(slotId, ref, date) }
    fun saveLibraryMeal(m: com.relentnet.cuttracker.data.LibMeal) = viewModelScope.launch { repo.saveLibraryMeal(m) }
    fun logWeight(lb: Double, date: java.time.LocalDate? = null) = viewModelScope.launch {
        repo.logWeight(lb, date)
        showToast("Logged ${com.relentnet.cuttracker.engine.ProgramEngine.fmt1(lb)} · +${ProgramSpec.XP_WEIGH} xp")
    }
    fun saveLiftSession(entries: List<Repo.LiftEntry>) = viewModelScope.launch { repo.saveLiftSession(entries) }
    fun savePhoto(path: String) = viewModelScope.launch { repo.savePhoto(path) }
    fun deletePhoto(date: java.time.LocalDate) = viewModelScope.launch { repo.deletePhoto(date) }
    fun dismissWelcomeBack() = viewModelScope.launch { repo.dismissWelcomeBack() }
    fun saveSettings(s: Settings, then: () -> Unit = {}) = viewModelScope.launch { repo.saveSettings(s); then() }
    fun resetProgram(then: () -> Unit = {}) = viewModelScope.launch { repo.resetProgram(); then() }

    companion object {
        fun factory(repo: Repo): ViewModelProvider.Factory = viewModelFactory { initializer { AppViewModel(repo) } }
    }
}
