package eu.bfss.puzzlebubbles.data

import eu.bfss.puzzlebubbles.domain.GameDefinition
import eu.bfss.puzzlebubbles.domain.GameRepository
import eu.bfss.puzzlebubbles.domain.GameType
import eu.bfss.puzzlebubbles.domain.WhatsThatGame
import eu.bfss.puzzlebubbles.domain.WhatsThatChoice
import eu.bfss.puzzlebubbles.domain.WhatsThatQuestion
import eu.bfss.puzzlebubbles.domain.WhatsThatGameSummary

class LocalGameRepository : GameRepository {
    override fun getGameCatalog(): List<GameDefinition> = listOf(
        GameDefinition(
            id = "whats-that",
            title = "What's That?",
            type = GameType.WHATS_THAT,
            coverImageAssetId = "whats-that-cover",
        ),
        GameDefinition(
            id = "matching",
            title = "Match Up",
            type = GameType.MATCHING,
            coverImageAssetId = "matching-cover",
        ),
    )

    override fun getWhatsThatGames(): List<WhatsThatGameSummary> =
        listOf(WhatsThatGameSummary("whats-that", "Animals", isBuiltIn = true))

    override fun getWhatsThatGame(id: String): WhatsThatGame {
        require(id == "whats-that") { "Unknown What's That? game ID: $id" }
        val targets = listOf("cat", "cow", "dog", "goat", "monkey", "mouse", "parrot", "pig", "rabbit", "sheep")
        return WhatsThatGame(
            id = id,
            title = "Animals",
            imagePool = targets.map { WhatsThatChoice(it, "$it-image") },
            questions = targets.map { WhatsThatQuestion(it, it, "$it-audio") },
        )
    }

}
