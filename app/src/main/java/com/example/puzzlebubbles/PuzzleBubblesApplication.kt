package com.example.puzzlebubbles

import android.app.Application
import androidx.room.Room
import com.example.puzzlebubbles.data.photos.LocalPhotoRepository
import com.example.puzzlebubbles.data.photos.PhotoDatabase
import com.example.puzzlebubbles.data.photos.PhotoImageStore
import com.example.puzzlebubbles.data.photos.PhotoProcessor
import java.io.File

class PuzzleBubblesApplication : Application() {
    val photoImages by lazy { PhotoImageStore(File(filesDir, "library/images")) }
    val photoProcessor by lazy { PhotoProcessor() }
    val photoRepository by lazy {
        LocalPhotoRepository(Room.databaseBuilder(this, PhotoDatabase::class.java, "photos.db").build(), photoImages)
    }
}
