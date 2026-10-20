package com.relentnet.cuttracker

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.relentnet.cuttracker.data.buildBackup
import com.relentnet.cuttracker.data.restoreBackup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The flows that would be embarrassing to break (DONI-468): a whole day to a Send, a frozen day filled in late from
 * the wall, and a backup that comes back the same. Emulator only, with its clock on a Sunday so the sample week fills
 * the wall: `adb -s emulator-5554 shell "date 102512002026.00"`, then
 * `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class FlowsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App
    private val notDone = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not done")

    @Before fun seed() {
        // These wipe the app's data: never on a real phone.
        org.junit.Assume.assumeTrue("emulator only", Build.HARDWARE == "ranchu" || Build.PRODUCT.contains("sdk"))
        val inst = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 33) inst.uiAutomation.grantRuntimePermission(inst.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        runBlocking { app.repo.seedSample() }
    }

    @After fun close() { scenario?.close() }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("SCRIPT").fetchSemanticsNodes().isNotEmpty() }
    }

    /** Swipes every unticked row on screen until none is left. */
    private fun tickEverything() {
        repeat(12) {
            val rows = compose.onAllNodes(notDone).fetchSemanticsNodes()
            if (rows.isEmpty()) return
            compose.onAllNodes(notDone)[0].performScrollTo().performTouchInput { swipeRight() }
            compose.waitForIdle()
            Thread.sleep(400)
        }
    }

    @Test fun tickingEveryItemSendsTheDay() {
        launch()
        tickEverything()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Every item ticked", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun aFrozenDayFilledInLateBecomesASend() {
        launch()
        val frozen = compose.onAllNodesWithContentDescription(", Freeze", substring = true)
        // Early in the week the frozen day sits on last week's wall.
        if (frozen.fetchSemanticsNodes().isEmpty()) compose.onAllNodesWithContentDescription("Earlier week")[0].performClick()
        compose.waitUntil(5_000) { frozen.fetchSemanticsNodes().isNotEmpty() }
        val day = frozen.fetchSemanticsNodes()[0].config[SemanticsProperties.ContentDescription].first().substringBefore(",")
        frozen[0].performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Rest day").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Rest day")[0].performScrollTo().performClick()
        compose.waitForIdle()
        tickEverything()
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("$day, Send").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun aBackupRestoresToTheSameData() = runBlocking {
        val repo = app.repo
        val before = repo.buildBackup()
        repo.restoreBackup(before)
        val after = repo.buildBackup()
        // A restore marks history as kept (the one-time 1.3.0 step); everything else comes back as it was.
        assertEquals(before.copy(exportedAt = "", settings = before.settings.copy(ledgerKept = true)), after.copy(exportedAt = ""))
    }

    @Test fun theUpgradeKeepsWhatAnOlderBuildShowedWithoutChangingXp() = runBlocking {
        val repo = app.repo
        val db = repo.db
        // A past Send whose ledger row is missing, the way a 1.2.0 history can look after a settings change.
        val send = db.xp().all().first { it.kind == com.relentnet.cuttracker.data.XpKind.SEND && it.date.isBefore(repo.today()) }
        db.xp().delete(send.date, com.relentnet.cuttracker.data.XpKind.SEND, null)
        repo.settings.update { it.copy(ledgerKept = false) }
        val xpBefore = db.xp().total()
        repo.keepOldScoresOnce()
        val scored = com.relentnet.cuttracker.engine.ProgramEngine.scoredDays(db.xp().all())
        assertEquals(com.relentnet.cuttracker.engine.DayResult.SEND, scored[send.date])
        assertEquals(xpBefore, db.xp().total())
        org.junit.Assert.assertTrue(repo.settings.current().ledgerKept)
    }
}
