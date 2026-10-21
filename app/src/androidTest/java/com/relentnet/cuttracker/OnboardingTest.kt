package com.relentnet.cuttracker

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.relentnet.cuttracker.data.buildBackup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The three ways into onboarding: a fresh install, the next route, and the preview that must write nothing. Emulator only. */
@RunWith(AndroidJUnit4::class)
class OnboardingTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as App

    @Before fun grant() {
        org.junit.Assume.assumeTrue("emulator only", Build.HARDWARE == "ranchu" || Build.PRODUCT.contains("sdk"))
        val inst = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 33) inst.uiAutomation.grantRuntimePermission(inst.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    @After fun close() { scenario?.close() }

    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java) }

    private fun waitFor(text: String, ms: Long = 15_000) =
        compose.waitUntil(ms) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun tap(text: String) {
        waitFor(text)
        compose.onAllNodesWithText(text)[0].performClick()
        compose.waitForIdle()
    }

    /** Next through every step after the hello, to the start call. */
    private fun walkToTheEnd() {
        while (compose.onAllNodesWithText("START THE ROUTE").fetchSemanticsNodes().isEmpty()) tap("NEXT")
        tap("START THE ROUTE")
    }

    @Test fun aFreshInstallLandsOnTodayUnderTheStartCall() {
        runBlocking { app.repo.resetProgram() }
        launch()
        tap("LET'S GO")
        tap("NEXT")   // the words: climbing
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("181.8")
        walkToTheEnd()
        waitFor("Climb on.")
        tap("PICK A DAY")
        runBlocking {
            val s = app.repo.settings.current()
            assertTrue(s.onboarded)
            assertTrue("the wall guide does not pop over the first Today", s.tourSeen)
            assertEquals(181.8, s.startWeight, 0.001)
            assertEquals(171.0, s.targetWeight, 0.001)
            assertEquals(155, s.protein)
            // The meals step put its starters in the library and kept every slot on for this target.
            assertTrue(s.library.size > com.relentnet.cuttracker.data.ProgramSpec.library.size)
            assertEquals(4, s.plans.getValue(com.relentnet.cuttracker.data.DayMode.OFF).slots.size)
            compose.waitUntil(5_000) { runBlocking { app.repo.db.weighIns().all().any { it.date == app.repo.today() } } }
        }
    }

    @Test fun theNextRouteCanBeCalledOffAndKeepsEveryLog() {
        runBlocking { app.repo.seedSample(); app.repo.settings.update { it.copy(onboarded = false) } }
        val logs = runBlocking { app.repo.db.dayLogs().all().size }
        val xp = runBlocking { app.repo.db.xp().total() }
        launch()
        waitFor("Next route.")
        tap("NOT YET")
        compose.waitUntil(5_000) { runBlocking { app.repo.settings.current().onboarded } }
        scenario?.close()

        runBlocking { app.repo.settings.update { it.copy(onboarded = false) } }
        launch()
        tap("LET'S GO")
        walkToTheEnd()
        waitFor("Climb on.")
        runBlocking {
            assertTrue(app.repo.settings.current().onboarded)
            assertEquals(logs, app.repo.db.dayLogs().all().size)
            assertEquals(xp, app.repo.db.xp().total())
        }
    }

    @Test fun aFullPreviewChangesNothing() {
        runBlocking { app.repo.seedSample(); app.repo.settings.update { it.copy(devOptions = true) } }
        launch()
        tap("YOU")
        waitFor("Developer options")
        compose.onAllNodesWithText("Developer options")[0].performScrollTo().performClick()
        waitFor("RUN")
        val settings = runBlocking { app.repo.settings.current() }
        val backup = runBlocking { app.repo.buildBackup().copy(exportedAt = "") }
        tap("RUN")
        tap("LET'S GO")
        tap("NEXT")
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("150")
        walkToTheEnd()
        waitFor("Climb on.")
        // The takeover's own Back, drawn over everything, is the last one in the tree.
        compose.onAllNodesWithText("BACK").let { it[it.fetchSemanticsNodes().size - 1].performClick() }
        waitFor("Developer options")
        runBlocking {
            assertEquals(settings, app.repo.settings.current())
            assertEquals(backup, app.repo.buildBackup().copy(exportedAt = ""))
        }
    }
}
