package com.example.puzzlebubbles

import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.ChooseItPhase
import com.example.puzzlebubbles.domain.ChooseItSessionEngine
import com.example.puzzlebubbles.domain.ChooseItSessionFactory
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class ChooseItSessionEngineTest {
    private fun engine() = ChooseItSessionEngine(
        ChooseItSessionFactory(Random(11)).create(
            LocalGameRepository().getChooseItGame("whats-that"),
        ),
    )

    @Test
    fun wrongAnswerDoesNotAdvanceAndOverlappingAnswersAreIgnored() {
        val engine = engine()
        val firstRound = engine.currentRound
        val wrongChoice = firstRound.choices.single { it.id != firstRound.correctChoiceId }

        engine.answer(wrongChoice.id)
        assertEquals(ChooseItPhase.WRONG_FEEDBACK, engine.state.phase)
        assertEquals(0, engine.state.roundIndex)

        engine.answer(firstRound.correctChoiceId)
        assertEquals(ChooseItPhase.WRONG_FEEDBACK, engine.state.phase)
        assertEquals(0, engine.state.roundIndex)

        engine.finishFeedback()
        assertEquals(ChooseItPhase.WAITING_FOR_ANSWER, engine.state.phase)
        assertEquals(0, engine.state.roundIndex)
    }

    @Test
    fun sessionCompletesOnlyAfterTenthCorrectRound() {
        val engine = engine()

        repeat(10) { index ->
            engine.answer(engine.currentRound.correctChoiceId)
            assertEquals(ChooseItPhase.CORRECT_FEEDBACK, engine.state.phase)
            engine.finishFeedback()
            if (index < 9) {
                assertEquals(index + 1, engine.state.roundIndex)
                assertEquals(ChooseItPhase.WAITING_FOR_ANSWER, engine.state.phase)
            }
        }

        assertEquals(9, engine.state.roundIndex)
        assertEquals(ChooseItPhase.GAME_COMPLETE, engine.state.phase)
    }
}
