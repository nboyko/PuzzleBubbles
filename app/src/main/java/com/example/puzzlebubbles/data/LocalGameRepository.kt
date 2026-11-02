package com.example.puzzlebubbles.data

import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameRepository
import com.example.puzzlebubbles.domain.GameType
import com.example.puzzlebubbles.domain.ChooseItGame
import com.example.puzzlebubbles.domain.ChooseItItem
import com.example.puzzlebubbles.domain.ChooseItGameSummary

class LocalGameRepository : GameRepository {
    override fun getGameCatalog(): List<GameDefinition> = listOf(
        GameDefinition(
            id = "whats-that",
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

    override fun getChooseItGames(): List<ChooseItGameSummary> =
        listOf(ChooseItGameSummary("whats-that", "Animals", isBuiltIn = true))

    override fun getChooseItGame(id: String): ChooseItGame {
        require(id == "whats-that") { "Unknown What's That? game ID: $id" }
        return ChooseItGame(
            id = id,
            title = "Animals",
            items = listOf(
                item("cat"), item("cow"), item("dog"), item("goat"), item("monkey"),
                item("mouse"), item("parrot"), item("pig"), item("rabbit"), item("sheep"),
            ),
        )
    }

    private fun item(id: String) = ChooseItItem(
        id = id,
        imageAssetId = "$id-image",
        audioAssetId = "$id-audio",
    )
}
