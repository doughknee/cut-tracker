package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.Backup
import com.relentnet.cuttracker.data.Prefs
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.backupJson
import com.relentnet.cuttracker.data.toDto
import com.relentnet.cuttracker.data.toSettings
import com.relentnet.cuttracker.engine.ProgramEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PrefsTest {
    @After fun reset() { ProgramEngine.firstDay = DayOfWeek.MONDAY }

    private val changed = Prefs(
        showTape = false, showRope = false, wallDecor = false, showXp = false, showTimes = false, showCalories = false,
        swipeToTick = false, eveningCard = false, weighStep = 0.5, startTab = "route", celebrate = "quiet", reduceMotion = "on",
        textScale = 1.3f, highContrast = true, weekStartsSunday = true, clock = "24", checkpointNote = false,
        widgetHideWeight = true, whatsNewNote = false,
    )

    @Test fun everyPrefSurvivesABackup() {
        val s = Settings(prefs = changed)
        val json = backupJson.encodeToString(com.relentnet.cuttracker.data.SettingsDto.serializer(), s.toDto())
        assertEquals(changed, backupJson.decodeFromString(com.relentnet.cuttracker.data.SettingsDto.serializer(), json).toSettings().prefs)
    }

    @Test fun aBackupFromBeforePrefsReadsAsTheOldBehavior() {
        val dto = Settings().toDto()
        val json = backupJson.encodeToString(com.relentnet.cuttracker.data.SettingsDto.serializer(), dto).replace(Regex("\"prefs\":\\s*\\{[^}]*\\},?"), "")
        assertEquals(Prefs(), backupJson.decodeFromString(com.relentnet.cuttracker.data.SettingsDto.serializer(), json).toSettings().prefs)
    }

    @Test fun aSundayStartMovesTheWeek() {
        val wed = LocalDate.of(2026, 10, 21)
        assertEquals(LocalDate.of(2026, 10, 19), ProgramEngine.weekStart(wed))
        ProgramEngine.firstDay = DayOfWeek.SUNDAY
        assertEquals(LocalDate.of(2026, 10, 18), ProgramEngine.weekStart(wed))
        assertEquals(LocalDate.of(2026, 10, 18), ProgramEngine.weekStart(LocalDate.of(2026, 10, 18)))
    }

    @Test fun defaultsAreHowTheAppBehavedBefore() {
        val p = Prefs()
        assertEquals(true, p.showTape && p.showRope && p.showXp && p.showTimes && p.swipeToTick && p.eveningCard && p.whatsNewNote)
        assertEquals(0.2, p.weighStep, 0.0)
        assertEquals("big", p.celebrate)
        assertEquals(DayOfWeek.MONDAY, p.weekStart)
    }

    // Keeps the Backup import used, so a rename of the class shows here first.
    @Suppress("unused") private val backupType = Backup::class
}
