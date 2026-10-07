package com.example.puzzlebubbles

import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.GameType
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalGameRepositoryTest {
    @Test
    fun catalogContainsTheTwoMvpGamesInOrder() {
        val games = LocalGameRepository().getGameCatalog()
        assertEquals(listOf("choose-it", "matching"), games.map { it.id })
        assertEquals(listOf("What's That?", "Match Up"), games.map { it.title })
        assertEquals(listOf(GameType.CHOOSE_IT, GameType.MATCHING), games.map { it.type })
        assertEquals(listOf("choose-it-cover", "matching-cover"), games.map { it.coverImageAssetId })
        assertEquals(games.size, games.map { it.id }.distinct().size)
    }
}
