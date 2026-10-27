package com.example.puzzlebubbles

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.puzzlebubbles.data.AudioPlayer
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.ChooseItPhase
import com.example.puzzlebubbles.domain.ChooseItSessionFactory
import com.example.puzzlebubbles.presentation.games.chooseit.ChooseItGameScreen
import com.example.puzzlebubbles.presentation.games.chooseit.ChooseItViewModel
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class ChooseItGestureTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var model: ChooseItViewModel
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
        model = ChooseItViewModel(LocalGameRepository().getChooseItGame("choose-it"),
            ChooseItSessionFactory(Random(0)))
        compose.setContent {
            PuzzleBubblesTheme {
                ChooseItGameScreen("What's That?", model.uiState, silentAudio,
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
            assertEquals(ChooseItPhase.CORRECT_FEEDBACK, model.uiState.phase)
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
            assertEquals(ChooseItPhase.CORRECT_FEEDBACK, model.uiState.phase)
        }
    }

    @Test fun verticalWrongSwipeGivesFeedbackThenAllowsCorrectRetry() {
        val right = correct
        val incorrect = wrong
        choice(incorrect).performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertEquals(listOf(incorrect), answers)
            assertEquals(ChooseItPhase.WRONG_FEEDBACK, model.uiState.phase)
            assertEquals(1, model.uiState.roundNumber)
        }
        choice(right).performTouchInput { swipeDown() }
        compose.runOnIdle { assertEquals(listOf(incorrect), answers) }
        compose.mainClock.advanceTimeBy(500)
        choice(right).performTouchInput { swipeDown() }
        compose.runOnIdle {
            assertEquals(listOf(incorrect, right), answers)
            assertEquals(ChooseItPhase.CORRECT_FEEDBACK, model.uiState.phase)
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
            assertEquals(ChooseItPhase.WAITING_FOR_ANSWER, model.uiState.phase)
        }
        compose.onRoot().performTouchInput { moveBy(Offset(30f, 30f)); up() }
        compose.runOnIdle { assertEquals(listOf(id), answers) }
    }

    @Test fun cancelledTouchBelowMovementThresholdDoesNotSelect() {
        choice(correct).performTouchInput { down(center); moveBy(Offset(1f, 1f)); cancel() }
        compose.runOnIdle {
            assertTrue(answers.isEmpty())
            assertEquals(ChooseItPhase.WAITING_FOR_ANSWER, model.uiState.phase)
        }
    }
}
