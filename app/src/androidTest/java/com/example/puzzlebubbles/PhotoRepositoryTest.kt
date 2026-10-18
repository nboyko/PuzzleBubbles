package com.example.puzzlebubbles

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puzzlebubbles.data.photos.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var root: File
    private lateinit var database: PhotoDatabase
    private lateinit var store: FailingStore
    private lateinit var repository: LocalPhotoRepository

    private class FailingStore(root: File) : PhotoImageStore(root) {
        var failure: String? = null
        override fun write(id: String, bitmap: Bitmap) {
            if (failure == "write") { ensureDirectory(); staging(id).writeText("partial"); throw IOException("disk full") }
            super.write(id, bitmap)
        }
        override fun promote(id: String) {
            if (failure == "rename") throw IOException("rename")
            super.promote(id)
        }
        override fun remove(file: File) {
            if (failure == "remove") throw IOException("cleanup")
            super.remove(file)
        }
    }
    @Before fun setup() = runBlocking {
        root = File(context.cacheDir, "photo-test-${UUID.randomUUID()}")
        database = Room.databaseBuilder(context, PhotoDatabase::class.java, File(root.apply { mkdirs() }, "test.db").path).build()
        store = FailingStore(File(root, "images"))
        repository = LocalPhotoRepository(database, store)
        repository.refresh()
    }
    @After fun cleanup() {
        database.close()
        root.deleteRecursively() // Unique test-owned directory only.
    }
    private fun draft(): String = store.putDraft(UUID.randomUUID().toString(),
        Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) })

    @Test fun savesNormalizedWebpAndReloadsFromNewDatabaseInstance() = runBlocking {
        val result = repository.save(draft())
        assertEquals(1, repository.photos.value.size)
        assertTrue(store.valid(result.id))
        val bytes = store.file(result.id).readBytes()
        database.close()
        database = Room.databaseBuilder(context, PhotoDatabase::class.java, File(root, "test.db").path).build()
        repository = LocalPhotoRepository(database, store)
        repository.refresh()
        assertEquals(listOf(result), repository.photos.value)
        assertArrayEquals(bytes, store.file(result.id).readBytes())
        assertEquals(listOf("${result.id}.webp"), store.directory.listFiles()!!.map { it.name })
    }
    @Test fun partialWriteAndRenameFailureRemoveTheirFiles() = runBlocking {
        for (failure in listOf("write", "rename")) {
            store.failure = failure
            try { repository.save(draft()); fail("Expected failure") } catch (_: IOException) { }
            assertTrue(database.photos().all().isEmpty())
            assertTrue(store.directory.listFiles()!!.isEmpty())
        }
    }
    @Test fun metadataInsertFailureRollsBackAndRemovesPromotedFile() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_insert BEFORE INSERT ON photos BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { repository.save(draft()); fail("Expected failure") } catch (_: Exception) { }
        assertTrue(database.photos().all().isEmpty())
        assertTrue(store.directory.listFiles()!!.isEmpty())
    }
    @Test fun reconcileInterruptedSavesPreservesCommittedPhotosAndIsRepeatable() = runBlocking {
        val committed = repository.save(draft())
        val orphan = UUID.randomUUID().toString()
        store.write(orphan, store.draft(draft())); store.promote(orphan)
        val temporary = UUID.randomUUID().toString()
        store.staging(temporary).writeText("partial")
        val missing = UUID.randomUUID().toString()
        database.photos().insert(PhotoEntity(missing, "$missing.webp", 0))
        val corrupt = UUID.randomUUID().toString()
        store.file(corrupt).writeText("not an image")
        database.photos().insert(PhotoEntity(corrupt, "$corrupt.webp", 0))
        repeat(2) { repository.refresh() }
        assertEquals(listOf(committed), repository.photos.value)
        assertEquals(listOf("${committed.id}.webp"), store.directory.listFiles()!!.map { it.name })
    }
    @Test fun deleteDatabaseFailureLeavesPhotoAndImageUntouched() = runBlocking {
        val photo = repository.save(draft())
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_delete BEFORE DELETE ON photos BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { repository.delete(photo.id); fail("Expected failure") } catch (_: Exception) { }
        assertEquals(listOf(photo), repository.photos.value)
        assertTrue(store.file(photo.id).exists())
    }
    @Test fun failedFileDeletionNeverResurrectsEntryAndReconciliationRetries() = runBlocking {
        val photo = repository.save(draft())
        store.failure = "remove"
        try { repository.delete(photo.id); fail("Expected failure") } catch (_: IOException) { }
        assertTrue(repository.photos.value.isEmpty())
        assertTrue(database.photos().all().isEmpty())
        assertTrue(store.file(photo.id).exists())
        store.failure = null
        repository.refresh()
        repository.delete(photo.id) // Repeated delete remains harmless.
        assertFalse(store.file(photo.id).exists())
        assertTrue(repository.photos.value.isEmpty())
    }
    @Test fun unsafeMetadataNeverTouchesFilesOutsideOwnedDirectory() = runBlocking {
        val outside = File(root, "original.webp").apply { writeText("original") }
        val id = UUID.randomUUID().toString()
        database.photos().insert(PhotoEntity(id, "../original.webp", 0))
        repository.refresh()
        assertEquals("original", outside.readText())
        assertTrue(repository.photos.value.isEmpty())
    }
}
