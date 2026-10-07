package com.example.puzzlebubbles.data

import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameRepository
import com.example.puzzlebubbles.domain.GameType

class LocalGameRepository : GameRepository {
    override fun getGameCatalog(): List<GameDefinition> = listOf(
        GameDefinition(
            id = "choose-it",
            title = "What's That?",
            type = GameType.CHOOSE_IT,
            coverImageAssetId = "choose-it-cover",
        ),
        GameDefinition(
            id = "matching",
            title = "Match Up",
            type = GameType.MATCHING,
            coverImageAssetId = "matching-cover",
        ),
    )
}
