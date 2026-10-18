package com.example.puzzlebubbles.data.photos

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

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

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = true)
abstract class PhotoDatabase : RoomDatabase() {
    abstract fun photos(): PhotoDao
}
