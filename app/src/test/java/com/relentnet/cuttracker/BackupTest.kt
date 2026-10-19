package com.relentnet.cuttracker

import com.relentnet.cuttracker.data.Backup
import com.relentnet.cuttracker.data.BadgeDto
import com.relentnet.cuttracker.data.Checkpoint
import com.relentnet.cuttracker.data.DayLogDto
import com.relentnet.cuttracker.data.LibMeal
import com.relentnet.cuttracker.data.LiftSetDto
import com.relentnet.cuttracker.data.PhotoDto
import com.relentnet.cuttracker.data.ProjectDto
import com.relentnet.cuttracker.data.Settings
import com.relentnet.cuttracker.data.WeighInDto
import com.relentnet.cuttracker.data.XpEventDto
import com.relentnet.cuttracker.data.backupJson
import com.relentnet.cuttracker.data.toDto
import com.relentnet.cuttracker.data.toSettings
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The export must survive a JSON round trip and unknown keys from a newer app; this is what a phone move depends on. */
class BackupTest {
    private val settings = Settings(
        onboarded = true, phase = 1, startDate = LocalDate.of(2026, 10, 19), lengthWeeks = 10, startWeight = 182.0, targetWeight = 170.0,
        caloriesP1 = 2150, caloriesP2 = 2600, protein = 150, workoutsPerWeek = 4,
        checkpoints = listOf(Checkpoint("2026-11-02", 180.0, note = "Pace check."), Checkpoint("2026-11-26", free = true, label = "Thanksgiving", note = "")),
        weighInTime = LocalTime.of(7, 45), pickTime = LocalTime.of(20, 0), resetHour = 3, haptics = false, theme = "light",
        lastOpenDate = LocalDate.of(2026, 10, 25), welcomeBackSeen = LocalDate.of(2026, 10, 24),
        library = listOf(LibMeal("lib_x", "Eggs + toast", 30, 400, "Three eggs, two slices.")),
    )

    private val backup = Backup(
        exportedAt = "2026-10-25T12:00:00Z",
        settings = settings.toDto(),
        dayLogs = listOf(DayLogDto("2026-10-19", "REST", listOf("w", "b", "l", "s", "d"), null, mapOf("d" to "lib:lib_d2", "b" to "custom:40:500:Oats"))),
        weighIns = listOf(WeighInDto("2026-10-19", 182.0), WeighInDto("2026-10-20", 181.4)),
        liftSets = listOf(LiftSetDto("2026-10-23", "dips", 10.0, 8, 0)),
        photos = listOf(PhotoDto("2026-10-19", "2026-10-19.jpg")),
        xpEvents = listOf(XpEventDto("2026-10-19", 1_760_000_000_000L, "send", 50, null)),
        badges = listOf(BadgeDto("first_send", 1_760_000_000_000L)),
        projects = listOf(ProjectDto("2026-10-19", "sends3", 2, false)),
    )

    @Test fun settingsSurviveTheDto() {
        assertEquals(settings, settings.toDto().toSettings())
    }

    @Test fun backupSurvivesJson() {
        val json = backupJson.encodeToString(Backup.serializer(), backup)
        val back = backupJson.decodeFromString(Backup.serializer(), json)
        assertEquals(backup, back)
        assertEquals(settings, back.settings.toSettings())
    }

    @Test fun unknownKeysFromANewerAppAreIgnored() {
        val json = backupJson.encodeToString(Backup.serializer(), backup)
            .replaceFirst("\"version\": 1,", "\"version\": 1,\n  \"futureField\": {\"nested\": true},")
        assertTrue("futureField" in json)
        val back = backupJson.decodeFromString(Backup.serializer(), json)
        assertEquals(backup.dayLogs, back.dayLogs)
    }

    @Test fun oldExportWithoutNewFieldsStillLoads() {
        // A v1 export predates workoutsPerWeek, library and picks; defaults must fill them in.
        val json = backupJson.encodeToString(Backup.serializer(), backup)
            .replace(Regex("\\s*\"workoutsPerWeek\": \\d+,"), "")
            .replace(Regex(",\\s*\"ingredients\": \\[\\]"), "")   // meals had no ingredient lines either
            .replace(Regex(",\\s*\"library\": \\[[\\s\\S]*?\\]\\s*\\}"), "\n  }")
        val back = backupJson.decodeFromString(Backup.serializer(), json)
        assertEquals(3, back.settings.workoutsPerWeek)
        assertTrue(back.settings.library.isNotEmpty())
    }
}
