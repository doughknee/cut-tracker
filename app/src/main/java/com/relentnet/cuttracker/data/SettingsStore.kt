package com.relentnet.cuttracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/** Everything in [ProgramSpec] that Settings can override, plus app state flags. */
data class Settings(
    val onboarded: Boolean = false,
    val phase: Int = 1,
    val startDate: LocalDate = ProgramSpec.PHASE_1_START,
    val lengthWeeks: Int = ProgramSpec.LENGTH_WEEKS,
    val startWeight: Double = ProgramSpec.START_WEIGHT,
    val targetWeight: Double = ProgramSpec.TARGET_WEIGHT,
    val caloriesP1: Int = ProgramSpec.CALORIES_P1,
    val caloriesP2: Int = ProgramSpec.CALORIES_P2,
    val protein: Int = ProgramSpec.PROTEIN_G,
    val workoutsPerWeek: Int = ProgramSpec.WORKOUTS_PER_WEEK,
    val meals: List<Meal> = ProgramSpec.meals,
    val exercises: List<Exercise> = ProgramSpec.exercises,
    val checkpoints: List<Checkpoint> = ProgramSpec.checkpoints,
    val weighInTime: LocalTime = ProgramSpec.WEIGH_IN_TIME,
    val pickTime: LocalTime = ProgramSpec.PICK_TIME,
    val resetHour: Int = ProgramSpec.DAY_RESET_HOUR,
    val haptics: Boolean = true,
    val theme: String = "system",
    /** Last program day the app was opened on; drives the welcome-back card. */
    val lastOpenDate: LocalDate? = null,
    /** Program day on which the welcome-back card was dismissed, so it shows once per re-entry. */
    val welcomeBackSeen: LocalDate? = null,
    val library: List<LibMeal> = ProgramSpec.library,
    /** The rules sheet opened itself once on the first Today. */
    val tourSeen: Boolean = false,
) {
    val calories: Int get() = caloriesP1
    val endDate: LocalDate get() = startDate.plusDays(lengthWeeks * 7L)
    val totalDays: Int get() = lengthWeeks * 7
}

private val Context.store: DataStore<Preferences> by preferencesDataStore("settings")

class SettingsStore(context: Context) {
    private val ds = context.applicationContext.store
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val phase = intPreferencesKey("phase")
        val startDate = stringPreferencesKey("startDate")
        val lengthWeeks = intPreferencesKey("lengthWeeks")
        val startWeight = doublePreferencesKey("startWeight")
        val targetWeight = doublePreferencesKey("targetWeight")
        val caloriesP1 = intPreferencesKey("caloriesP1")
        val caloriesP2 = intPreferencesKey("caloriesP2")
        val protein = intPreferencesKey("protein")
        val workoutsPerWeek = intPreferencesKey("workoutsPerWeek")
        val meals = stringPreferencesKey("meals")
        val library = stringPreferencesKey("library")
        val exercises = stringPreferencesKey("exercises")
        val checkpoints = stringPreferencesKey("checkpoints")
        val weighInTime = stringPreferencesKey("weighInTime")
        val pickTime = stringPreferencesKey("pickTime")
        val resetHour = intPreferencesKey("resetHour")
        val haptics = booleanPreferencesKey("haptics")
        val tourSeen = booleanPreferencesKey("tourSeen")
        val theme = stringPreferencesKey("theme")
        val lastOpenDate = stringPreferencesKey("lastOpenDate")
        val welcomeBackSeen = stringPreferencesKey("welcomeBackSeen")
    }

    val flow: Flow<Settings> = ds.data.map { p -> read(p) }

    suspend fun current(): Settings = flow.first()

    private fun read(p: Preferences): Settings {
        val d = Settings()
        return Settings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            phase = p[K.phase] ?: d.phase,
            startDate = p[K.startDate]?.let(LocalDate::parse) ?: d.startDate,
            lengthWeeks = p[K.lengthWeeks] ?: d.lengthWeeks,
            startWeight = p[K.startWeight] ?: d.startWeight,
            targetWeight = p[K.targetWeight] ?: d.targetWeight,
            caloriesP1 = p[K.caloriesP1] ?: d.caloriesP1,
            caloriesP2 = p[K.caloriesP2] ?: d.caloriesP2,
            protein = p[K.protein] ?: d.protein,
            workoutsPerWeek = p[K.workoutsPerWeek] ?: d.workoutsPerWeek,
            // A meals list saved before the library existed has no defaults; borrow them from the spec by slot id.
            meals = (p[K.meals]?.let { runCatching { json.decodeFromString(ListSerializer(Meal.serializer()), it) }.getOrNull() } ?: d.meals)
                .map { m -> if (m.defaultLib != null) m else m.copy(defaultLib = ProgramSpec.meals.firstOrNull { it.id == m.id }?.defaultLib) },
            library = p[K.library]?.let { runCatching { json.decodeFromString(ListSerializer(LibMeal.serializer()), it) }.getOrNull() } ?: d.library,
            exercises = p[K.exercises]?.let { runCatching { json.decodeFromString(ListSerializer(Exercise.serializer()), it) }.getOrNull() } ?: d.exercises,
            checkpoints = p[K.checkpoints]?.let { runCatching { json.decodeFromString(ListSerializer(Checkpoint.serializer()), it) }.getOrNull() } ?: d.checkpoints,
            weighInTime = p[K.weighInTime]?.let(LocalTime::parse) ?: d.weighInTime,
            pickTime = p[K.pickTime]?.let(LocalTime::parse) ?: d.pickTime,
            resetHour = p[K.resetHour] ?: d.resetHour,
            haptics = p[K.haptics] ?: d.haptics,
            tourSeen = p[K.tourSeen] ?: d.tourSeen,
            theme = p[K.theme] ?: d.theme,
            lastOpenDate = p[K.lastOpenDate]?.let(LocalDate::parse),
            welcomeBackSeen = p[K.welcomeBackSeen]?.let(LocalDate::parse),
        )
    }

    /** Writes every field of [s]; Save in Settings applies all at once. */
    suspend fun save(s: Settings) = ds.edit { p -> write(p, s) }

    suspend fun update(block: (Settings) -> Settings) = ds.edit { p -> write(p, block(read(p))) }

    private fun write(p: MutablePreferences, s: Settings) {
        p[K.onboarded] = s.onboarded
        p[K.phase] = s.phase
        p[K.startDate] = s.startDate.toString()
        p[K.lengthWeeks] = s.lengthWeeks
        p[K.startWeight] = s.startWeight
        p[K.targetWeight] = s.targetWeight
        p[K.caloriesP1] = s.caloriesP1
        p[K.caloriesP2] = s.caloriesP2
        p[K.protein] = s.protein
        p[K.workoutsPerWeek] = s.workoutsPerWeek
        p[K.meals] = json.encodeToString(ListSerializer(Meal.serializer()), s.meals)
        p[K.library] = json.encodeToString(ListSerializer(LibMeal.serializer()), s.library)
        p[K.exercises] = json.encodeToString(ListSerializer(Exercise.serializer()), s.exercises)
        p[K.checkpoints] = json.encodeToString(ListSerializer(Checkpoint.serializer()), s.checkpoints)
        p[K.weighInTime] = s.weighInTime.toString()
        p[K.pickTime] = s.pickTime.toString()
        p[K.resetHour] = s.resetHour
        p[K.haptics] = s.haptics
        p[K.tourSeen] = s.tourSeen
        p[K.theme] = s.theme
        if (s.lastOpenDate != null) p[K.lastOpenDate] = s.lastOpenDate.toString() else p.remove(K.lastOpenDate)
        if (s.welcomeBackSeen != null) p[K.welcomeBackSeen] = s.welcomeBackSeen.toString() else p.remove(K.welcomeBackSeen)
    }

    suspend fun clear() = ds.edit { it.clear() }
}
