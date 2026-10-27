package com.example.puzzlebubbles.data.sounds

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "sounds")
data class SoundEntity(
    @PrimaryKey val id: String, val fileName: String, val durationMs: Long, val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val name: String,
)

@Dao
interface SoundDao {
    @Query("SELECT * FROM sounds ORDER BY createdAt DESC, id DESC")
    suspend fun all(): List<SoundEntity>
    @Query("SELECT * FROM sounds WHERE id = :id")
    suspend fun find(id: String): SoundEntity?
    @Insert
    suspend fun insert(sound: SoundEntity)
    @Query("DELETE FROM sounds WHERE id = :id")
    suspend fun delete(id: String)
}
