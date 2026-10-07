package com.example.puzzlebubbles.domain

interface GameRepository {
    fun getGameCatalog(): List<GameDefinition>
}
