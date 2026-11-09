package eu.bfss.puzzlebubbles.domain

interface GameRepository {
    fun getGameCatalog(): List<GameDefinition>
    fun getWhatsThatGames(): List<WhatsThatGameSummary>
    fun getWhatsThatGame(id: String): WhatsThatGame
    fun observeWhatsThatGames(): kotlinx.coroutines.flow.Flow<List<WhatsThatGameSummary>> =
        kotlinx.coroutines.flow.flowOf(getWhatsThatGames())
    suspend fun loadWhatsThatGame(id: String): WhatsThatGame = getWhatsThatGame(id)
    suspend fun saveCustomGame(game: CustomGame) { error("Custom games unavailable") }
    suspend fun loadCustomGame(id: String): CustomGame { error("Custom game unavailable") }
    suspend fun updateCustomGame(game: CustomGame) { error("Custom games unavailable") }
    suspend fun deleteCustomGame(id: String) { error("Custom games unavailable") }
}
