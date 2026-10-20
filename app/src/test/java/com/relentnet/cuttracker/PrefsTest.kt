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
        bigHolds = true, weekdayLabels = false, showChain = false, showUpNext = false, showSubs = false, strikeDone = false,
        showProtein = false, doneOpen = true, showWeightTile = false, weightHero = "latest", weightDecimals = 0, chartWeeks = 4,
        chartDots = true, chartRate = false, chartCheckpoints = false, weighText = "Scale first.", pickText = "Pick one.",
        snoozeMinutes = 30, lockScreenHide = true, widgetTheme = "dark", widgetSeeThrough = true, widgetXp = false,
        boldText = true, bigTargets = true, holdMarks = true, plainWall = true, dayFirst = true, secureScreen = true,
        liftSets = 4, liftReps = "5", liftStepScale = 2.0, foodPortion = false, keepScreenOn = true,
    )

    @Test fun everyPrefIsChangedInTheBackupTest() {
        // A new pref left at its default here would pass the backup test without being carried; this keeps the list whole.
        val d = Prefs()
        for (f in Prefs::class.java.declaredFields.filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }) {
            f.isAccessible = true
            assert(f.get(changed) != f.get(d)) { "${f.name} is still its default in PrefsTest.changed" }
        }
    }

    @Test fun theChartWindowEndsTodayAndStaysFull() {
        assertEquals(1..84, com.relentnet.cuttracker.ui.weight.chartWindow(30, 84, 0))
        assertEquals(3..30, com.relentnet.cuttracker.ui.weight.chartWindow(30, 84, 4))
        assertEquals(1..28, com.relentnet.cuttracker.ui.weight.chartWindow(5, 84, 4))
        assertEquals(57..84, com.relentnet.cuttracker.ui.weight.chartWindow(90, 84, 4))
        assertEquals(1..28, com.relentnet.cuttracker.ui.weight.chartWindow(10, 28, 4))
    }

    @Test fun datesReadMonthOrDayFirst() {
        val a = LocalDate.of(2026, 10, 19)
        try {
            assertEquals("Oct 19 to 25", com.relentnet.cuttracker.data.Dates.span(a, a.plusDays(6)))
            assertEquals("Oct 30 to Nov 5", com.relentnet.cuttracker.data.Dates.span(a.plusDays(11), a.plusDays(17)))
            com.relentnet.cuttracker.data.Dates.dayFirst = true
            assertEquals("19 Oct", com.relentnet.cuttracker.data.Dates.md(a))
            assertEquals("19 to 25 Oct", com.relentnet.cuttracker.data.Dates.span(a, a.plusDays(6)))
            assertEquals("Mon 19 Oct", com.relentnet.cuttracker.data.Dates.wmd(a))
        } finally { com.relentnet.cuttracker.data.Dates.dayFirst = false }
    }

    @Test fun ownWordsReplaceTheReminderOnlyWhenTyped() {
        val k = com.relentnet.cuttracker.notify.Reminders.KIND_WEIGH
        assertEquals(com.relentnet.cuttracker.notify.Reminders.text(k), com.relentnet.cuttracker.notify.Reminders.text(k, Prefs(weighText = "  ")))
        assertEquals("Scale first.", com.relentnet.cuttracker.notify.Reminders.text(k, changed))
    }

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
        assertEquals(true, p.weekdayLabels && p.showChain && p.showUpNext && p.showSubs && p.strikeDone && p.showProtein && p.showWeightTile)
        assertEquals(true, p.chartRate && p.chartCheckpoints && p.widgetXp && p.foodPortion)
        assertEquals(false, p.bigHolds || p.doneOpen || p.chartDots || p.lockScreenHide || p.widgetSeeThrough || p.boldText || p.bigTargets ||
            p.holdMarks || p.plainWall || p.dayFirst || p.secureScreen || p.keepScreenOn)
        assertEquals(listOf("average", "phone", "8 to 12"), listOf(p.weightHero, p.widgetTheme, p.liftReps))
        assertEquals(listOf(1, 0, 0, 3), listOf(p.weightDecimals, p.chartWeeks, p.snoozeMinutes, p.liftSets))
        assertEquals(1.0, p.liftStepScale, 0.0)
    }

    // Keeps the Backup import used, so a rename of the class shows here first.
    @Suppress("unused") private val backupType = Backup::class
}
