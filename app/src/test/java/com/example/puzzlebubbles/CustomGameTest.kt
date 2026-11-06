package com.example.puzzlebubbles

import com.example.puzzlebubbles.domain.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CustomGameTest {
    private fun game(count: Int) = CustomGame("whats-that-test", "Animals", listOf("a", "b", "distractor-only"),
        List(count) { CustomQuestion("a", "same-sound") })
    @Test fun oneToTenQuestionsEachProduceOneRoundIncludingRepeatedPairs() {
        for (count in 1..10) {
            val rounds = WhatsThatSessionFactory(Random(2)).create(game(count).playable()).rounds
            assertEquals(count, rounds.size)
            assertEquals(count, rounds.map { it.id }.distinct().size)
            rounds.forEach {
                assertEquals("a", it.correctChoiceId)
                assertEquals("library-sound:same-sound", it.audioAssetId)
                assertEquals(2, it.choices.map { choice -> choice.id }.distinct().size)
                assertTrue(it.choices.all { choice -> choice.id in game(count).imageIds })
            }
        }
    }
    @Test fun rejectsInvalidQuestionCountsNamesPoolsAndRelationships() {
        val invalid = listOf(game(0), game(11), game(1).copy(name = "  "), game(1).copy(imageIds = listOf("a")),
            game(1).copy(questions = listOf(CustomQuestion("outside", "sound"))))
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { it.validate() } }
    }
    @Test fun distractorOnlyPhotosAreUsedAndQuestionsAreShuffled() {
        val game = game(10).copy(questions = List(10) { CustomQuestion("a", "sound-$it") })
        val rounds = WhatsThatSessionFactory(Random(5)).create(game.playable()).rounds
        assertTrue(rounds.any { it.choices.any { choice -> choice.id == "distractor-only" } })
        assertEquals(game.questions.map { "library-sound:${it.soundId}" }.toSet(), rounds.map { it.audioAssetId }.toSet())
        assertNotEquals(game.questions.map { "library-sound:${it.soundId}" }, rounds.map { it.audioAssetId })
    }
}
