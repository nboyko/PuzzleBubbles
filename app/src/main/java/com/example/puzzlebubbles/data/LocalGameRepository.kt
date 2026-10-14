package com.example.puzzlebubbles.data

import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameRepository
import com.example.puzzlebubbles.domain.GameType
import com.example.puzzlebubbles.domain.ChooseItGame
import com.example.puzzlebubbles.domain.ChooseItItem

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

    override fun getChooseItGame(id: String): ChooseItGame {
        require(id == "choose-it") { "Unknown What's That? game ID: $id" }
        return ChooseItGame(
            id = id,
            title = "What's That?",
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
