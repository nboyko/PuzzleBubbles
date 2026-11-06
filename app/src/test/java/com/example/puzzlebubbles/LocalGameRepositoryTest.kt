package com.example.puzzlebubbles

import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.GameType
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalGameRepositoryTest {
    @Test
    fun catalogContainsTheTwoMvpGamesInOrder() {
        val games = LocalGameRepository().getGameCatalog()
        assertEquals(listOf("whats-that", "matching"), games.map { it.id })
        assertEquals(listOf("What's That?", "Match Up"), games.map { it.title })
        assertEquals(listOf(GameType.WHATS_THAT, GameType.MATCHING), games.map { it.type })
        assertEquals(listOf("whats-that-cover", "matching-cover"), games.map { it.coverImageAssetId })
        assertEquals(games.size, games.map { it.id }.distinct().size)
    }

    @Test
    fun selectionContainsOnlyTheLoadableDefaultGame() {
        val repository = LocalGameRepository()
        val entries = repository.getWhatsThatGames()
        assertEquals(listOf("whats-that"), entries.map { it.id })
        assertEquals(listOf("Animals"), entries.map { it.name })
        assertEquals(listOf(true), entries.map { it.isBuiltIn })
        entries.forEach { assertEquals(it.id, repository.getWhatsThatGame(it.id).id) }
    }

    @Test
    fun whatsThatPresetContainsTenCompleteUniqueItems() {
        val game = LocalGameRepository().getWhatsThatGame("whats-that")

        assertEquals(10, game.questions.size)
        assertEquals(10, game.questions.map { it.id }.distinct().size)
        assertEquals(10, game.imagePool.map { it.imageAssetId }.distinct().size)
        assertEquals(10, game.questions.map { it.audioAssetId }.distinct().size)
        assertEquals(game.imagePool.map { it.id }.toSet(), game.questions.map { it.imageId }.toSet())
    }
}
