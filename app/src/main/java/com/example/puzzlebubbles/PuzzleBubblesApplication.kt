package com.example.puzzlebubbles

import android.app.Application
import androidx.room.Room
import com.example.puzzlebubbles.data.photos.LocalPhotoRepository
import com.example.puzzlebubbles.data.photos.PhotoDatabase
import com.example.puzzlebubbles.data.photos.PhotoImageStore
import com.example.puzzlebubbles.data.photos.PhotoProcessor
import java.io.File
import com.example.puzzlebubbles.data.sounds.SoundFileStore
import com.example.puzzlebubbles.data.sounds.LocalSoundRepository

class PuzzleBubblesApplication : Application() {
    val photoImages by lazy { PhotoImageStore(File(filesDir, "library/images")) }
    val photoProcessor by lazy { PhotoProcessor() }
    private val libraryDatabase by lazy {
        Room.databaseBuilder(this, PhotoDatabase::class.java, "photos.db")
            .addMigrations(PhotoDatabase.MIGRATION_1_2, PhotoDatabase.MIGRATION_2_3).build()
    }
    val photoRepository by lazy {
        LocalPhotoRepository(libraryDatabase, photoImages)
    }
    val soundFiles by lazy { SoundFileStore(File(filesDir, "library/audio")) }
    val soundRepository by lazy { LocalSoundRepository(libraryDatabase, soundFiles) }
}
