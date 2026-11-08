package com.example.puzzlebubbles

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzlebubbles.domain.WhatsThatGameSummary
import com.example.puzzlebubbles.presentation.games.whatsthat.WhatsThatGameSelectionScreen
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class GameManagementTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val plays = mutableListOf<String>()
    private val edits = mutableListOf<String>()
    private val deletes = mutableListOf<String>()
    private val largeText = mutableStateOf(false)
    private val games = listOf(WhatsThatGameSummary("built-in", "Animals", true)) +
        (1..12).map { WhatsThatGameSummary("custom-$it", "Game $it") }

    @Before fun setup() {
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            PuzzleBubblesTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeText.value) 1.5f else density.fontScale)) {
                    WhatsThatGameSelectionScreen(games, plays::add, {}, {}, onEdit = edits::add, onDelete = deletes::add)
                }
            }
        } }
        compose.waitForIdle()
    }
    private fun row(id: String) = compose.onNodeWithTag("game-row-$id")
    private fun open(id: String) { row(id).performTouchInput { swipeLeft() }; compose.waitForIdle() }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        try {
            val directory = File(compose.activity.externalCacheDir, "management-verification").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
    @Test fun thresholdOneOpenRowAndFirstTapClosesWithoutPlaying() {
        val initial = row("custom-1").fetchSemanticsNode().boundsInRoot
        row("custom-1").performTouchInput {
            swipe(Offset(width * .8f, center.y), Offset(width * .8f - 30f, center.y), 300)
        }
        compose.waitForIdle()
        assertEquals(initial.left, row("custom-1").fetchSemanticsNode().boundsInRoot.left, 1f)
        assertTrue(plays.isEmpty())
        open("custom-1")
        compose.onNodeWithContentDescription("Edit game Game 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete game Game 1").assertIsDisplayed()
        compose.onNodeWithText("Game 1").assertIsDisplayed()
        capture("swipe-actions")
        assertTrue(row("custom-1").fetchSemanticsNode().boundsInRoot.right > initial.center.x)
        open("custom-2")
        compose.onNodeWithContentDescription("Edit game Game 1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Edit game Game 2").assertIsDisplayed()
        row("custom-1").performClick()
        compose.onNodeWithContentDescription("Edit game Game 2").assertDoesNotExist()
        assertTrue(plays.isEmpty())
        row("custom-1").performClick()
        assertEquals(listOf("custom-1"), plays)
    }
    @Test fun predefinedRowsCannotSwipeAndAccessibilityActionsDoNotRequireSwipe() {
        val initial = row("built-in").fetchSemanticsNode().boundsInRoot
        row("built-in").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(initial, row("built-in").fetchSemanticsNode().boundsInRoot)
        assertFalse(row("built-in").fetchSemanticsNode().config.contains(SemanticsActions.CustomActions))
        assertEquals(2, row("custom-1").fetchSemanticsNode().config[SemanticsActions.CustomActions].size)
        compose.onNodeWithContentDescription("Edit game Game 1").assertDoesNotExist()
        for (label in listOf("Edit game", "Delete game")) {
            val action = row("custom-1").fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == label }
            compose.runOnIdle { assertTrue(action.action()) }
        }
        assertEquals(listOf("custom-1"), edits); assertEquals(listOf("custom-1"), deletes)
        assertTrue(plays.isEmpty())
        row("built-in").performClick(); assertEquals(listOf("built-in"), plays)
    }
    @Test fun revealedButtonsAndBackUseTheSameManagementActions() {
        open("custom-1")
        compose.onNodeWithContentDescription("Edit game Game 1").performClick()
        assertEquals(listOf("custom-1"), edits); assertTrue(plays.isEmpty())
        open("custom-2")
        compose.onNodeWithContentDescription("Delete game Game 2").performClick()
        assertEquals(listOf("custom-2"), deletes); assertTrue(plays.isEmpty())
        open("custom-1"); pressBack()
        compose.onNodeWithContentDescription("Edit game Game 1").assertDoesNotExist()
        row("built-in").assertIsDisplayed()
    }
    @Test fun verticalScrollingAndOutsideTapCloseActionsWithoutPlaying() {
        open("custom-1")
        // List padding outside all rows is a dismissal target.
        compose.onNodeWithTag("whats-that-game-list").performTouchInput { click(Offset(2f, 2f)) }
        compose.onNodeWithContentDescription("Edit game Game 1").assertDoesNotExist()
        open("custom-1")
        compose.onNodeWithTag("whats-that-game-list").performTouchInput { swipeUp() }
        compose.onNodeWithContentDescription("Edit game Game 1").assertDoesNotExist()
        assertTrue(plays.isEmpty())
        compose.onNodeWithTag("whats-that-game-list").performScrollToIndex(11)
        row("custom-11").assertIsDisplayed()
    }
    @Test fun enlargedTextKeepsTheNameAndBothActionsVisible() {
        compose.runOnIdle { largeText.value = true }
        open("custom-1")
        compose.onNodeWithText("Game 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Edit game Game 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete game Game 1").assertIsDisplayed()
        capture("swipe-actions-large-text")
        val nameBounds = compose.onNodeWithText("Game 1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val editBounds = compose.onNodeWithContentDescription("Edit game Game 1").fetchSemanticsNode().boundsInRoot
        assertTrue("Keep space between the name and actions", editBounds.left - nameBounds.right >= 8f)
    }
}
