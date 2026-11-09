package eu.bfss.puzzlebubbles

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import eu.bfss.puzzlebubbles.data.photos.*
import eu.bfss.puzzlebubbles.domain.PhotoCrop
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
        var failCleanup = false
        override fun write(id: String, bitmap: Bitmap) {
            if (failure == "write") { ensureDirectory(); staging(id).writeText("partial"); throw IOException("disk full") }
            super.write(id, bitmap)
        }
        override fun promote(id: String) {
            if (failure == "rename") throw IOException("rename")
            super.promote(id)
        }
        override fun remove(file: File) {
            if (failure == "remove" || failCleanup) throw IOException("cleanup")
            super.remove(file)
        }
        override fun replace(id: String) {
            if (failure == "replace") throw IOException("replacement")
            super.replace(id)
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
    private suspend fun patternedPhoto(): eu.bfss.puzzlebubbles.domain.LibraryPhoto {
        val source = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(source)
        val paint = android.graphics.Paint().apply { color = Color.RED }
        canvas.drawRect(0f, 0f, 512f, 1024f, paint)
        paint.color = Color.GREEN
        canvas.drawRect(512f, 0f, 1024f, 1024f, paint)
        return repository.save(store.putDraft(UUID.randomUUID().toString(), source))
    }
    @Test fun replacementPreservesMetadataOrderAndPublishesFreshImageRevision() = runBlocking {
        val photo = patternedPhoto()
        repository.save(draft())
        val rows = database.photos().all()
        val order = repository.photos.value.map { it.id }
        val before = store.file(photo.id).readBytes()
        repository.replace(photo.id, PhotoCrop(512f, 0f, 512f))
        assertEquals(rows, database.photos().all())
        assertEquals(order, repository.photos.value.map { it.id })
        assertFalse(before.contentEquals(store.file(photo.id).readBytes()))
        val updated = repository.photos.value.single { it.id == photo.id }
        assertEquals(photo.createdAt, updated.createdAt)
        assertTrue(updated.revision > photo.revision)
        val image = store.image(photo.assetId, false)!!
        assertTrue(Color.green(image.getPixel(512, 512)) > 230)
        image.recycle()
        repository.refresh()
        assertEquals(updated, repository.photos.value.single { it.id == photo.id })
        repository.replace(photo.id, PhotoCrop(0f, 0f, 512f)) // Re-edit the current stored image.
        assertTrue(repository.photos.value.single { it.id == photo.id }.revision > updated.revision)
    }
    @Test fun failedReplacementAndCleanupKeepPreviousBytesAndAllowRetry() = runBlocking {
        val photo = patternedPhoto()
        val before = store.file(photo.id).readBytes()
        val rows = database.photos().all()
        for (failure in listOf("write", "replace")) {
            store.failure = failure
            store.failCleanup = true
            try { repository.replace(photo.id, PhotoCrop(512f, 0f, 512f)); fail("Expected failure") }
            catch (_: IOException) { }
            assertArrayEquals(before, store.file(photo.id).readBytes())
            assertEquals(rows, database.photos().all())
            store.failure = null; store.failCleanup = false
            repeat(2) { repository.refresh() }
            assertFalse(store.staging(photo.id).exists())
        }
        repository.replace(photo.id, PhotoCrop(512f, 0f, 512f))
        assertTrue(store.valid(photo.id))
    }
    @Test fun interruptedReplacementRecoversCompleteOldOrNewImage() = runBlocking {
        val photo = patternedPhoto()
        val before = store.file(photo.id).readBytes()
        val green = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        store.write(photo.id, green) // Interruption before the swap.
        repository = LocalPhotoRepository(database, store)
        repeat(2) { repository.refresh() }
        assertArrayEquals(before, store.file(photo.id).readBytes())
        assertFalse(store.staging(photo.id).exists())
        store.write(photo.id, green); store.replace(photo.id) // Interruption immediately after the swap.
        val committed = store.file(photo.id).readBytes()
        repository = LocalPhotoRepository(database, store)
        repeat(2) { repository.refresh() }
        assertArrayEquals(committed, store.file(photo.id).readBytes())
        assertEquals(listOf(photo.id), repository.photos.value.map { it.id })
        assertTrue(store.valid(photo.id))
        green.recycle()
    }
}
