package com.example.puzzlebubbles

import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.ChooseItSessionFactory
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChooseItSessionFactoryTest {
    private val game = LocalGameRepository().getChooseItGame("whats-that")

    @Test
    fun sessionUsesEveryTargetExactlyOnceWithTwoExplicitChoices() {
        val session = ChooseItSessionFactory(Random(7)).create(game)

        assertEquals(10, session.rounds.size)
        assertEquals(game.items.map { it.id }.toSet(), session.rounds.map { it.targetItemId }.toSet())
        assertEquals(10, session.rounds.map { it.targetItemId }.distinct().size)
        session.rounds.forEach { round ->
            assertEquals(2, round.choices.size)
            assertEquals(2, round.choices.map { it.id }.distinct().size)
            assertTrue(round.choices.any { it.id == round.correctChoiceId })
            val incorrect = round.choices.single { it.id != round.correctChoiceId }
            assertNotEquals(round.targetItemId, incorrect.id)
            assertEquals(round.targetItemId, round.correctChoiceId)
        }
    }

    @Test
    fun correctChoiceAppearsOnBothSidesAcrossRandomizedSessions() {
        val correctPositions = (0..12).flatMap { seed ->
            ChooseItSessionFactory(Random(seed)).create(game).rounds.map { round ->
                round.choices.indexOfFirst { it.id == round.correctChoiceId }
            }
        }.toSet()

        assertEquals(setOf(0, 1), correctPositions)
    }
}
