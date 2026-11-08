package com.example.puzzlebubbles.data

import androidx.room.*
import com.example.puzzlebubbles.data.photos.PhotoDatabase
import com.example.puzzlebubbles.data.photos.PhotoEntity
import com.example.puzzlebubbles.data.sounds.SoundEntity
import com.example.puzzlebubbles.domain.*
import kotlinx.coroutines.flow.map

@Entity(tableName = "custom_games")
data class CustomGameEntity(@PrimaryKey val id: String, val name: String, val createdAt: Long)

@Entity(tableName = "game_images", primaryKeys = ["gameId", "photoId"], foreignKeys = [
    ForeignKey(entity = CustomGameEntity::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = PhotoEntity::class, parentColumns = ["id"], childColumns = ["photoId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("photoId")])
data class GameImageEntity(val gameId: String, val photoId: String, val position: Int)

@Entity(tableName = "game_questions", primaryKeys = ["gameId", "position"], foreignKeys = [
    ForeignKey(entity = GameImageEntity::class, parentColumns = ["gameId", "photoId"], childColumns = ["gameId", "photoId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = SoundEntity::class, parentColumns = ["id"], childColumns = ["soundId"], onDelete = ForeignKey.RESTRICT),
], indices = [Index(value = ["gameId", "photoId"]), Index("soundId")])
data class GameQuestionEntity(val gameId: String, val position: Int, val photoId: String, val soundId: String)

@Dao
interface CustomGameDao {
    @Query("SELECT * FROM custom_games ORDER BY createdAt DESC, id DESC")
    fun observe(): kotlinx.coroutines.flow.Flow<List<CustomGameEntity>>
    @Query("SELECT * FROM custom_games WHERE id = :id") suspend fun find(id: String): CustomGameEntity?
    @Query("SELECT * FROM game_images WHERE gameId = :id ORDER BY position") suspend fun images(id: String): List<GameImageEntity>
    @Query("SELECT * FROM game_questions WHERE gameId = :id ORDER BY position") suspend fun questions(id: String): List<GameQuestionEntity>
    @Insert suspend fun insert(game: CustomGameEntity)
    @Insert suspend fun insertImages(images: List<GameImageEntity>)
    @Insert suspend fun insertQuestions(questions: List<GameQuestionEntity>)
    @Query("UPDATE custom_games SET name = :name WHERE id = :id") suspend fun rename(id: String, name: String)
    @Query("DELETE FROM game_images WHERE gameId = :id") suspend fun clearImages(id: String)
    @Query("DELETE FROM custom_games WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT EXISTS(SELECT 1 FROM game_images WHERE photoId = :id)") suspend fun usesPhoto(id: String): Boolean
    @Query("SELECT EXISTS(SELECT 1 FROM game_questions WHERE soundId = :id)") suspend fun usesSound(id: String): Boolean
}

class LibraryGameRepository(private val database: PhotoDatabase) : GameRepository {
    private val bundled = LocalGameRepository()
    override fun getGameCatalog() = bundled.getGameCatalog()
    override fun getWhatsThatGames() = bundled.getWhatsThatGames()
    override fun getWhatsThatGame(id: String) = bundled.getWhatsThatGame(id)
    override fun observeWhatsThatGames() = database.games().observe().map { rows ->
        bundled.getWhatsThatGames() + rows.map { WhatsThatGameSummary(it.id, it.name) }
    }
    override suspend fun loadWhatsThatGame(id: String): WhatsThatGame {
        if (id == "whats-that") return bundled.getWhatsThatGame(id)
        return database.withTransaction { read(id)?.playable() ?: error("Game unavailable") }
    }
    private suspend fun read(id: String): CustomGame? {
        val dao = database.games()
        val row = dao.find(id) ?: return null
        return CustomGame(id, row.name, dao.images(id).map { it.photoId },
            dao.questions(id).map { CustomQuestion(it.photoId, it.soundId) })
    }
    override suspend fun loadCustomGame(id: String): CustomGame {
        require(bundled.getWhatsThatGames().none { it.id == id }) { "Predefined games cannot be edited" }
        return database.withTransaction { read(id) ?: error("Game unavailable") }
    }
    override suspend fun updateCustomGame(game: CustomGame) {
        game.validate()
        require(bundled.getWhatsThatGames().none { it.id == game.id }) { "Predefined games cannot be edited" }
        database.withTransaction {
            checkNotNull(database.games().find(game.id)) { "Game unavailable" }
            check(game.imageIds.all { database.photos().find(it) != null }) { "A selected photo is unavailable" }
            check(game.questions.all { database.sounds().find(it.soundId) != null }) { "A selected sound is unavailable" }
            // Keep the original row/creation time. Cascaded question removal and
            // replacement references are visible together only after commit.
            database.games().rename(game.id, game.name.trim())
            database.games().clearImages(game.id)
            database.games().insertImages(game.imageIds.mapIndexed { i, id -> GameImageEntity(game.id, id, i) })
            database.games().insertQuestions(game.questions.mapIndexed { i, q -> GameQuestionEntity(game.id, i, q.imageId, q.soundId) })
        }
    }
    override suspend fun saveCustomGame(game: CustomGame) {
        game.validate()
        database.withTransaction {
            // A retry after an uncertain acknowledgement must never duplicate a game.
            read(game.id)?.let { check(it == game.copy(name = game.name.trim())); return@withTransaction }
            check(game.imageIds.all { database.photos().find(it) != null }) { "A selected photo is unavailable" }
            check(game.questions.all { database.sounds().find(it.soundId) != null }) { "A selected sound is unavailable" }
            database.games().insert(CustomGameEntity(game.id, game.name.trim(), System.currentTimeMillis()))
            database.games().insertImages(game.imageIds.mapIndexed { i, id -> GameImageEntity(game.id, id, i) })
            database.games().insertQuestions(game.questions.mapIndexed { i, q -> GameQuestionEntity(game.id, i, q.imageId, q.soundId) })
        }
    }
    override suspend fun deleteCustomGame(id: String) {
        require(id.startsWith("whats-that-") && bundled.getWhatsThatGames().none { it.id == id }) { "Predefined games cannot be deleted" }
        database.withTransaction { database.games().delete(id) }
    }
}
