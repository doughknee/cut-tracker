package com.relentnet.cuttracker

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for the overnight fixes, on a seeded sample week. They reset the app's data, so they run on the
 * emulator only: `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`, never with the phone attached.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before fun seed() {
        val inst = InstrumentationRegistry.getInstrumentation()
        if (Build.VERSION.SDK_INT >= 33) inst.uiAutomation.grantRuntimePermission(inst.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val app = inst.targetContext.applicationContext as App
        runBlocking { app.repo.seedSample() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("SCRIPT").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun close() { scenario.close() }

    private fun back() = scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

    private fun openBeta(page: String) {
        compose.onNodeWithText("BETA").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText(page).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(page)[0].performClick()
        compose.waitForIdle()
    }

    @Test fun backClosesTheMealPickerAndStaysInTheApp() {
        compose.onAllNodesWithText("Lunch")[0].performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("DEFAULT").fetchSemanticsNodes().isNotEmpty() }
        back()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("DEFAULT").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("SCRIPT").assertExists()
    }

    @Test fun deletingALiftOffersUndoAndUndoBringsItBack() {
        openBeta("Lifts")
        compose.onNodeWithText("Dips").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("DELETE").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("DELETE").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("UNDO").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Dips").assertDoesNotExist()
        compose.onNodeWithText("UNDO").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Dips").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun typingATargetReplacesTheValue() {
        openBeta("Targets")
        compose.onNodeWithText("165").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("170")
        compose.onAllNodes(hasSetTextAction())[0].performImeAction()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("170").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("165").assertDoesNotExist()
    }

    @Test fun aSheetWithTypedTextAsksBeforeDiscarding() {
        openBeta("Meals")
        compose.onNodeWithContentDescription("Add").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("New meal").fetchSemanticsNodes().isNotEmpty() }
        // Every editable field gets text, so the sheet has edits whichever field is the name.
        val n = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
        for (i in 0 until n) compose.onAllNodes(hasSetTextAction())[i].performTextInput("Oats")
        // A real back key: the sheet lives in its own window, which the activity's dispatcher does not reach.
        // The first press may only close the keyboard.
        val inst = InstrumentationRegistry.getInstrumentation()
        repeat(2) {
            if (compose.onAllNodesWithText("Discard what you typed?").fetchSemanticsNodes().isEmpty()) {
                inst.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK); compose.waitForIdle(); Thread.sleep(500)
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Discard what you typed?").fetchSemanticsNodes().isNotEmpty() }
    }
}
