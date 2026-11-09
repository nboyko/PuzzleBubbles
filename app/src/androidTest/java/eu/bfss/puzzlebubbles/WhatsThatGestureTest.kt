package eu.bfss.puzzlebubbles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import eu.bfss.puzzlebubbles.data.AudioPlayer
import eu.bfss.puzzlebubbles.data.LocalGameRepository
import eu.bfss.puzzlebubbles.domain.WhatsThatPhase
import eu.bfss.puzzlebubbles.domain.WhatsThatSessionFactory
import eu.bfss.puzzlebubbles.presentation.games.whatsthat.WhatsThatGameScreen
import eu.bfss.puzzlebubbles.presentation.games.whatsthat.WhatsThatViewModel
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class WhatsThatGestureTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var model: WhatsThatViewModel
    private val answers = mutableListOf<String>()
    private val silentAudio = object : AudioPlayer {
        override fun play(assetId: String) = Unit
        override suspend fun playToEnd(assetId: String) = Unit
        override fun stop() = Unit
        override fun release() = Unit
    }
    private val correct get() = model.uiState.round.correctChoiceId
    private val wrong get() = model.uiState.round.choices.first { it.id != correct }.id
    private fun choice(id: String) = compose.onNodeWithContentDescription(id)

    @Before fun setup() {
        compose.mainClock.autoAdvance = false
        model = WhatsThatViewModel(LocalGameRepository().getWhatsThatGame("whats-that"),
            WhatsThatSessionFactory(Random(0)))
        compose.setContent {
            PuzzleBubblesTheme {
                WhatsThatGameScreen("What's That?", model.uiState, silentAudio,
                    onAnswer = { answers += it; model.answer(it) }, onReplay = model::replayAudio,
                    onFeedbackFinished = model::finishFeedback, onBack = {}, onGameComplete = {},
                    modifier = Modifier.fillMaxSize())
            }
        }
        compose.mainClock.advanceTimeBy(300)
    }

    @Test fun ordinaryTapStillSelectsExactlyOnce() {
        val id = correct
        choice(id).performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(listOf(id), answers)
            assertEquals(WhatsThatPhase.CORRECT_FEEDBACK, model.uiState.phase)
        }
    }

    @Test fun accessibleClickStillSelectsExactlyOnce() {
        val id = correct
        choice(id).performClick()
        compose.runOnIdle { assertEquals(listOf(id), answers) }
    }

    @Test fun horizontalSwipeSelectsCorrectImageExactlyOnce() {
        val id = correct
        choice(id).performTouchInput { swipeRight() }
        compose.runOnIdle {
            assertEquals(listOf(id), answers)
            assertEquals(WhatsThatPhase.CORRECT_FEEDBACK, model.uiState.phase)
        }
    }

    @Test fun verticalWrongSwipeGivesFeedbackThenAllowsCorrectRetry() {
        val right = correct
        val incorrect = wrong
        choice(incorrect).performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertEquals(listOf(incorrect), answers)
            assertEquals(WhatsThatPhase.WRONG_FEEDBACK, model.uiState.phase)
            assertEquals(1, model.uiState.roundNumber)
        }
        choice(right).performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(listOf(incorrect), answers) }
        compose.mainClock.advanceTimeBy(500)
        choice(right).performTouchInput { swipeDown() }
        compose.runOnIdle {
            assertEquals(listOf(incorrect, right), answers)
            assertEquals(WhatsThatPhase.CORRECT_FEEDBACK, model.uiState.phase)
        }
    }

    @Test fun swipingOutAndAcrossAnotherImageSelectsOnlyTheStartingImage() {
        val incorrect = wrong
        val start = choice(incorrect).fetchSemanticsNode().boundsInRoot.center
        val end = choice(correct).fetchSemanticsNode().boundsInRoot.center
        choice(incorrect).performTouchInput { swipe(center, center + (end - start)) }
        compose.runOnIdle {
            assertEquals(listOf(incorrect), answers)
            assertEquals(incorrect, model.uiState.feedbackChoiceId)
        }
    }

    @Test fun heldSwipeDoesNotSelectAgainInTheNextRound() {
        val id = correct
        choice(id).performTouchInput { down(center); moveBy(Offset(100f, 0f)) }
        compose.runOnIdle { assertEquals(listOf(id), answers) }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle {
            assertEquals(2, model.uiState.roundNumber)
            assertEquals(WhatsThatPhase.WAITING_FOR_ANSWER, model.uiState.phase)
        }
        compose.onRoot().performTouchInput { moveBy(Offset(30f, 30f)); up() }
        compose.runOnIdle { assertEquals(listOf(id), answers) }
    }

    @Test fun cancelledTouchBelowMovementThresholdDoesNotSelect() {
        choice(correct).performTouchInput { down(center); moveBy(Offset(1f, 1f)); cancel() }
        compose.runOnIdle {
            assertTrue(answers.isEmpty())
            assertEquals(WhatsThatPhase.WAITING_FOR_ANSWER, model.uiState.phase)
        }
    }
}
