package com.example.puzzlebubbles.data.photos

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.puzzlebubbles.data.sounds.SoundDao
import com.example.puzzlebubbles.data.sounds.SoundEntity
import com.example.puzzlebubbles.domain.defaultSoundName
import com.example.puzzlebubbles.data.*

@Entity(tableName = "photos")
data class PhotoEntity(@PrimaryKey val id: String, val fileName: String, val createdAt: Long)

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY createdAt DESC, id DESC")
    suspend fun all(): List<PhotoEntity>
    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun find(id: String): PhotoEntity?
    @Insert
    suspend fun insert(photo: PhotoEntity)
    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [PhotoEntity::class, SoundEntity::class, CustomGameEntity::class, GameImageEntity::class, GameQuestionEntity::class], version = 4, exportSchema = true)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun games(): CustomGameDao
    abstract fun photos(): PhotoDao
    abstract fun sounds(): SoundDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS custom_games (id TEXT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS game_images (gameId TEXT NOT NULL, photoId TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(gameId, photoId), FOREIGN KEY(gameId) REFERENCES custom_games(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(photoId) REFERENCES photos(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_images_photoId ON game_images(photoId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS game_questions (gameId TEXT NOT NULL, position INTEGER NOT NULL, photoId TEXT NOT NULL, soundId TEXT NOT NULL, PRIMARY KEY(gameId, position), FOREIGN KEY(gameId, photoId) REFERENCES game_images(gameId, photoId) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(soundId) REFERENCES sounds(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_questions_gameId_photoId ON game_questions(gameId, photoId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_questions_soundId ON game_questions(soundId)")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sounds` (`id` TEXT NOT NULL, `fileName` TEXT NOT NULL, " +
                    "`durationMs` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sounds` ADD COLUMN `name` TEXT NOT NULL DEFAULT ''")
                db.query("SELECT id FROM sounds ORDER BY createdAt ASC, id ASC").use { rows ->
                    var index = 0
                    while (rows.moveToNext()) {
                        db.execSQL("UPDATE sounds SET name = ? WHERE id = ?",
                            arrayOf(defaultSoundName(index++), rows.getString(0)))
                    }
                }
            }
        }
    }
}
