package com.example.puzzlebubbles

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.example.puzzlebubbles.data.AudioPlayer
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.*
import com.example.puzzlebubbles.presentation.PuzzleBubblesApp
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GameSelectionNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val base = LocalGameRepository()
    private val loadedIds = mutableListOf<String>()
    private var loadGate: CompletableDeferred<Unit>? = null
    private var deleteGate: CompletableDeferred<Unit>? = null
    private var failDelete = false
    private val deletedIds = mutableListOf<String>()
    private val entries = MutableStateFlow(listOf(
        WhatsThatGameSummary("whats-that", "Animals", isBuiltIn = true),
        WhatsThatGameSummary("whats-that-test-fixture", "Another Game"),
        WhatsThatGameSummary("whats-that-other-fixture", "Third Game"),
    ))
    private val repository = object : GameRepository {
        override fun getGameCatalog() = base.getGameCatalog()
        override fun getWhatsThatGames() = entries.value
        override fun observeWhatsThatGames() = entries
        override suspend fun deleteCustomGame(id: String) {
            deletedIds += id
            deleteGate?.await()
            check(!failDelete) { "Injected deletion failure" }
            entries.value = entries.value.filterNot { it.id == id }
        }
        override fun getWhatsThatGame(id: String): WhatsThatGame {
            loadedIds += id
            return base.getWhatsThatGame("whats-that").copy(id = id)
        }
        override suspend fun loadWhatsThatGame(id: String): WhatsThatGame {
            loadGate?.await()
            return getWhatsThatGame(id)
        }
    }
    private val audio = object : AudioPlayer {
        val requests = mutableListOf<String>()
        val completion = CompletableDeferred<Unit>()
        var stops = 0
        var active: String? = null
        override fun play(assetId: String) { requests += assetId; active = assetId }
        override suspend fun playToEnd(assetId: String) {
            play(assetId)
            if (assetId == "sfx-game-complete") completion.await()
            active = null
        }
        override fun stop() { stops++; active = null }
        override fun release() = stop()
    }
    private fun show(activity: MainActivity) {
        activity.setContent {
            PuzzleBubblesTheme { PuzzleBubblesApp(repository.getGameCatalog(), repository, audio) }
        }
    }
    @Before fun setup() {
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.onActivity(::show)
        settle()
    }
    private fun settle() {
        // Freeze time only to inspect game feedback. Navigation transitions must
        // finish before asserting the active destination or its audio cleanup.
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }
    private fun openSelection() {
        compose.onNodeWithText("What's That?").performScrollTo().performClick()
        settle()
        compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
    }
    private fun play(name: String = "Animals") {
        compose.onNodeWithContentDescription("Play $name").performClick()
        settle()
    }
    private fun answerCorrectly() {
        val items = base.getWhatsThatGame("whats-that").questions
        val lastPrompt = audio.requests.last { request -> items.any { it.audioAssetId == request } }
        val target = items.single { it.audioAssetId == lastPrompt }
        compose.onNodeWithContentDescription(target.imageId).performClick()
    }

    @Test fun selectionHasBottomNavigationAndFloatingAddWithoutAHeader() {
        openSelection()
        compose.onNodeWithText("Animals").assertIsDisplayed()
        compose.onNodeWithText("Another Game").assertIsDisplayed()
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsNotSelected()
        compose.onNodeWithText("Library").assertIsNotSelected()
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        val navigation = compose.onNodeWithTag("bottom-navigation").fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithTag("add-game-fab").fetchSemanticsNode().boundsInRoot
        assertTrue("The floating Add Game action must stay above navigation", add.bottom <= navigation.top)
        compose.onNodeWithContentDescription("Add Game").assertIsEnabled()
        compose.onNodeWithContentDescription("Replay sound").assertDoesNotExist()
        assertTrue(loadedIds.isEmpty())
        assertTrue(audio.requests.isEmpty())
        compose.onNodeWithText("Home").performClick()
        settle()
        compose.onNodeWithTag("home-content").assertIsDisplayed()
    }

    @Test fun asynchronousLoadingStartsOnePromptAfterTheEntryTransition() {
        openSelection()
        loadGate = CompletableDeferred()
        compose.onNodeWithContentDescription("Play Animals").performClick()
        compose.mainClock.advanceTimeBy(80)
        compose.runOnIdle { loadGate!!.complete(Unit) }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertTrue("Do not play a prompt while the game entry animation is still running", audio.requests.isEmpty())
        settle()
        assertEquals("Opening a loaded game must play exactly one prompt", 1, audio.requests.size)
        assertEquals(0, audio.stops)
        compose.mainClock.advanceTimeBy(2000)
        compose.waitForIdle()
        assertEquals(1, audio.requests.size)
        compose.onNodeWithContentDescription("Replay sound").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertEquals(2, audio.requests.size)
    }

    @Test fun deleteMarksOnlyTheTargetRowAndGuardsRepeatedConfirmation() {
        openSelection()
        val pending = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Pending deletion")
        compose.onNodeWithContentDescription("Delete game Another Game").performClick()
        settle()
        compose.onAllNodes(pending).assertCountEquals(1)
        compose.onNodeWithTag("game-row-whats-that-test-fixture").assert(pending)
        compose.onNodeWithTag("game-row-whats-that").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        compose.onNodeWithTag("game-row-whats-that-other-fixture").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        compose.onNodeWithText("Cancel").performClick()
        settle()
        compose.onAllNodes(pending).assertCountEquals(0)
        deleteGate = CompletableDeferred()
        compose.onNodeWithContentDescription("Delete game Another Game").performClick()
        settle()
        compose.onNodeWithText("Delete").performTouchInput { repeat(3) { click() } }
        settle()
        compose.onNodeWithText("Delete").assertIsNotEnabled()
        compose.onAllNodes(pending).assertCountEquals(1)
        assertEquals(listOf("whats-that-test-fixture"), deletedIds)
        assertTrue(audio.requests.isEmpty())
        compose.runOnIdle { deleteGate!!.complete(Unit) }
        settle()
        compose.onNodeWithTag("game-row-whats-that-test-fixture").assertDoesNotExist()
        compose.onNodeWithText("Third Game").assertIsDisplayed()
        compose.onAllNodes(pending).assertCountEquals(0)
    }

    @Test fun failedDeletionRetainsTheGameAndAllowsExplicitRetry() {
        openSelection()
        failDelete = true
        compose.onNodeWithContentDescription("Delete game Another Game").performClick()
        settle()
        compose.onNodeWithText("Delete").performClick()
        settle()
        compose.onNodeWithText("Couldn't delete this game. Please try again.").assertIsDisplayed()
        compose.onNodeWithText("Delete").assertIsEnabled()
        compose.onNodeWithTag("game-row-whats-that-test-fixture").assertExists()
        assertTrue(audio.requests.isEmpty())
        failDelete = false
        compose.onNodeWithText("Delete").performClick()
        settle()
        compose.onNodeWithTag("game-row-whats-that-test-fixture").assertDoesNotExist()
        compose.onNodeWithText("Third Game").assertIsDisplayed()
        assertEquals(List(2) { "whats-that-test-fixture" }, deletedIds)
    }

    @Test fun libraryNavigationReplacesSelectionWithoutAnExtraBackDestination() {
        openSelection()
        compose.onNodeWithText("Library").performClick()
        settle()
        compose.onNodeWithText("My Library").assertIsDisplayed()
        pressBack()
        settle()
        compose.onNodeWithTag("home-content").assertIsDisplayed()
        compose.onNodeWithTag("whats-that-selection").assertDoesNotExist()
    }

    @Test fun selectedIdAndRapidPlayUseOneGameplayAndSelectionDestination() {
        openSelection()
        compose.onNodeWithContentDescription("Play Another Game").performTouchInput { repeat(3) { click() } }
        settle()
        compose.onNodeWithContentDescription("Round 1 of 10").assertIsDisplayed()
        assertEquals(listOf("whats-that-test-fixture"), loadedIds)
        assertEquals(1, audio.requests.size)
        pressBack()
        settle()
        compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
        assertNull(audio.active)
        pressBack()
        settle()
        compose.onNodeWithTag("home-content").assertIsDisplayed()
    }

    @Test fun recreationPreservesSelectionAndProgressButExitStartsAFreshSession() {
        openSelection()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::show)
        settle()
        compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
        assertTrue(audio.requests.isEmpty())
        play()
        answerCorrectly()
        compose.mainClock.advanceTimeBy(2000)
        compose.onNodeWithContentDescription("Round 2 of 10").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::show)
        settle()
        compose.onNodeWithContentDescription("Round 2 of 10").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        settle()
        compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
        play()
        compose.onNodeWithContentDescription("Round 1 of 10").assertIsDisplayed()
    }

    @Test fun leavingDuringFeedbackStopsAudioAndCancelsPendingAdvance() {
        openSelection()
        play()
        answerCorrectly()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val requests = audio.requests.toList()
        val stops = audio.stops
        compose.onNodeWithContentDescription("Back").performClick()
        settle()
        compose.mainClock.advanceTimeBy(5000)
        compose.onNodeWithTag("whats-that-selection").assertIsDisplayed()
        assertTrue(audio.stops > stops)
        assertNull(audio.active)
        assertEquals(requests, audio.requests)
    }

    @Test fun completionReturnsHomeAndClearsGameplayAndSelection() {
        openSelection()
        play()
        repeat(10) { index ->
            compose.onNodeWithContentDescription("Round ${index + 1} of 10").assertIsDisplayed()
            answerCorrectly()
            compose.mainClock.advanceTimeBy(2000)
        }
        compose.onNodeWithContentDescription("Game complete").assertIsDisplayed()
        assertEquals(1, audio.requests.count { it == "sfx-game-complete" })
        compose.mainClock.advanceTimeBy(3100)
        compose.onNodeWithContentDescription("Game complete").assertIsDisplayed()
        compose.runOnIdle { audio.completion.complete(Unit) }
        settle()
        compose.onNodeWithTag("home-content").assertIsDisplayed()
        compose.onNodeWithTag("whats-that-selection").assertDoesNotExist()
        compose.activityRule.scenario.onActivity {
            assertFalse("Home must not have a completed game or selection to navigate Back to",
                it.onBackPressedDispatcher.hasEnabledCallbacks())
        }
    }
}
