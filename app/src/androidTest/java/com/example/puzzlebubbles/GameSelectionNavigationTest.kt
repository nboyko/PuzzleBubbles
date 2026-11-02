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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GameSelectionNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val base = LocalGameRepository()
    private val loadedIds = mutableListOf<String>()
    private val repository = object : GameRepository {
        override fun getGameCatalog() = base.getGameCatalog()
        override fun getChooseItGames() = listOf(
            ChooseItGameSummary("whats-that", "Animals", isBuiltIn = true),
            ChooseItGameSummary("whats-that-test-fixture", "Another Game"),
        )
        override fun getChooseItGame(id: String): ChooseItGame {
            loadedIds += id
            return base.getChooseItGame("whats-that").copy(id = id)
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
        val items = base.getChooseItGame("whats-that").items
        val lastPrompt = audio.requests.last { request -> items.any { it.audioAssetId == request } }
        val target = items.single { it.audioAssetId == lastPrompt }
        compose.onNodeWithContentDescription(target.id).performClick()
    }

    @Test fun selectionHasBottomNavigationAndFloatingAddWithoutAHeader() {
        openSelection()
        compose.onNodeWithText("Animals").assertIsDisplayed()
        compose.onNodeWithText("Another Game").assertIsDisplayed()
        compose.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertDoesNotExist()
        val navigation = compose.onNodeWithTag("bottom-navigation").fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithTag("add-game-fab").fetchSemanticsNode().boundsInRoot
        assertTrue("The floating Add Game action must stay above navigation", add.bottom <= navigation.top)
        compose.onNodeWithContentDescription("Add Game — coming soon").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Replay sound").assertDoesNotExist()
        assertTrue(loadedIds.isEmpty())
        assertTrue(audio.requests.isEmpty())
        compose.onNodeWithText("Home").performClick()
        settle()
        compose.onNodeWithTag("home-content").assertIsDisplayed()
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
