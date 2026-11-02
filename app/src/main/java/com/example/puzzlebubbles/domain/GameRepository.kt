package com.example.puzzlebubbles.domain

interface GameRepository {
    fun getGameCatalog(): List<GameDefinition>
    fun getChooseItGames(): List<ChooseItGameSummary>
    fun getChooseItGame(id: String): ChooseItGame
}
