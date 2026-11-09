package eu.bfss.puzzlebubbles

import android.app.Application
import androidx.room.Room
import eu.bfss.puzzlebubbles.data.photos.LocalPhotoRepository
import eu.bfss.puzzlebubbles.data.photos.PhotoDatabase
import eu.bfss.puzzlebubbles.data.photos.PhotoImageStore
import eu.bfss.puzzlebubbles.data.photos.PhotoProcessor
import java.io.File
import eu.bfss.puzzlebubbles.data.sounds.SoundFileStore
import eu.bfss.puzzlebubbles.data.sounds.LocalSoundRepository

class PuzzleBubblesApplication : Application() {
    val photoImages by lazy { PhotoImageStore(File(filesDir, "library/images")) }
    val photoProcessor by lazy { PhotoProcessor() }
    private val libraryDatabase by lazy {
        Room.databaseBuilder(this, PhotoDatabase::class.java, "photos.db")
            .addMigrations(PhotoDatabase.MIGRATION_1_2, PhotoDatabase.MIGRATION_2_3, PhotoDatabase.MIGRATION_3_4).build()
    }
    val photoRepository by lazy {
        LocalPhotoRepository(libraryDatabase, photoImages)
    }
    val soundFiles by lazy { SoundFileStore(File(filesDir, "library/audio")) }
    val soundRepository by lazy { LocalSoundRepository(libraryDatabase, soundFiles) }
    val gameRepository by lazy { eu.bfss.puzzlebubbles.data.LibraryGameRepository(libraryDatabase) }
}
