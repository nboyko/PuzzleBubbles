package eu.bfss.puzzlebubbles

import eu.bfss.puzzlebubbles.data.LocalGameRepository
import eu.bfss.puzzlebubbles.domain.WhatsThatPhase
import eu.bfss.puzzlebubbles.domain.WhatsThatSessionEngine
import eu.bfss.puzzlebubbles.domain.WhatsThatSessionFactory
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsThatSessionEngineTest {
    private fun engine() = WhatsThatSessionEngine(
        WhatsThatSessionFactory(Random(11)).create(
            LocalGameRepository().getWhatsThatGame("whats-that"),
        ),
    )

    @Test
    fun wrongAnswerDoesNotAdvanceAndOverlappingAnswersAreIgnored() {
        val engine = engine()
        val firstRound = engine.currentRound
        val wrongChoice = firstRound.choices.single { it.id != firstRound.correctChoiceId }

        engine.answer(wrongChoice.id)
        assertEquals(WhatsThatPhase.WRONG_FEEDBACK, engine.state.phase)
        assertEquals(0, engine.state.roundIndex)

        engine.answer(firstRound.correctChoiceId)
        assertEquals(WhatsThatPhase.WRONG_FEEDBACK, engine.state.phase)
        assertEquals(0, engine.state.roundIndex)

        engine.finishFeedback()
        assertEquals(WhatsThatPhase.WAITING_FOR_ANSWER, engine.state.phase)
        assertEquals(0, engine.state.roundIndex)
    }

    @Test
    fun sessionCompletesOnlyAfterTenthCorrectRound() {
        val engine = engine()

        repeat(10) { index ->
            engine.answer(engine.currentRound.correctChoiceId)
            assertEquals(WhatsThatPhase.CORRECT_FEEDBACK, engine.state.phase)
            engine.finishFeedback()
            if (index < 9) {
                assertEquals(index + 1, engine.state.roundIndex)
                assertEquals(WhatsThatPhase.WAITING_FOR_ANSWER, engine.state.phase)
            }
        }

        assertEquals(9, engine.state.roundIndex)
        assertEquals(WhatsThatPhase.GAME_COMPLETE, engine.state.phase)
    }
}
