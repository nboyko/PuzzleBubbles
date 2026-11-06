package com.example.puzzlebubbles

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.puzzlebubbles.data.photos.PhotoDatabase
import com.example.puzzlebubbles.data.sounds.*
import com.example.puzzlebubbles.domain.SoundDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SoundStorageTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var root: File
    private lateinit var database: PhotoDatabase
    private lateinit var store: FailingFiles
    private lateinit var repository: LocalSoundRepository
    private lateinit var fixture: File
    private class FailingFiles(directory: File) : SoundFileStore(directory) {
        var failure: String? = null
        override fun stage(id: String, source: File) {
            if (failure == "stage") { staging(id).writeText("partial"); throw IOException("disk full") }
            super.stage(id, source)
        }
        override fun promote(id: String) { if (failure == "promote") throw IOException("rename"); super.promote(id) }
        override fun remove(file: File) { if (failure == "cleanup") throw IOException("cleanup"); super.remove(file) }
    }
    @Before fun setup() = runBlocking {
        root = File(context.cacheDir, "sound-storage-${UUID.randomUUID()}").apply { mkdirs() }
        fixture = File(root, "fixture.m4a").also { aacFixture(it) }
        database = Room.databaseBuilder(context, PhotoDatabase::class.java, File(root, "test.db").path).build()
        store = FailingFiles(File(root, "audio"))
        repository = LocalSoundRepository(database, store)
        repository.refresh()
    }
    @After fun cleanup() {
        database.close()
        root.deleteRecursively() // This test's unique cache subtree only.
    }
    private fun draft(): SoundDraft {
        val (id, file) = store.newDraft()
        fixture.copyTo(file)
        return SoundDraft("sound-draft:$id", store.duration(file))
    }
    @Test fun nativeOutputToleranceIsBoundedAndActualDurationIsPreserved() = runBlocking {
        val (acceptedId, acceptedFile) = store.newDraft()
        aacFixture(acceptedFile, 5050)
        val duration = store.duration(acceptedFile)
        assertTrue("Expected bounded native overhead: $duration", duration in 5001..5100)
        val saved = repository.save(SoundDraft("sound-draft:$acceptedId", duration), "Short recording")
        assertEquals(duration, saved.durationMs)
        repository.refresh()
        assertEquals(listOf(saved), repository.sounds.value)

        val (rejectedId, rejectedFile) = store.newDraft()
        aacFixture(rejectedFile, 5200)
        try {
            repository.save(SoundDraft("sound-draft:$rejectedId", 5200), "Too long")
            fail("Output beyond 5100 ms must be rejected")
        } catch (_: IOException) { }
        assertEquals(listOf(saved), repository.sounds.value)
        assertNull(database.sounds().find(rejectedId))
        assertFalse(store.file(rejectedId).exists())
    }
    @Test fun draftIsNotListedUntilSaveAndValidRecordingSurvivesRestart() = runBlocking {
        val draft = draft()
        repository.refresh() // A live unsaved draft must survive refresh/rotation.
        assertTrue(repository.sounds.value.isEmpty())
        assertTrue(store.assetFile(draft.assetId).exists())
        val saved = repository.save(draft, "Track 1")
        assertEquals(draft.durationMs, saved.durationMs)
        assertEquals("Track 1", saved.name)
        assertEquals("library-sound:${saved.id}", saved.assetId)
        assertFalse(store.assetFile(draft.assetId).exists())
        database.close()
        database = Room.databaseBuilder(context, PhotoDatabase::class.java, File(root, "test.db").path).build()
        repository = LocalSoundRepository(database, SoundFileStore(store.directory))
        repository.refresh()
        assertEquals(listOf(saved), repository.sounds.value)
        assertTrue(store.file(saved.id).isFile)
    }
    @Test fun stageRenameAndMetadataFailuresKeepDraftForRetryWithoutBrokenEntries() = runBlocking {
        for (failure in listOf("stage", "promote", "metadata")) {
            val draft = draft()
            val id = draft.assetId.substringAfter(':')
            store.failure = failure
            if (failure == "metadata") database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_sound BEFORE INSERT ON sounds BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try { repository.save(draft, "Track 1"); fail("Expected $failure failure") } catch (_: Exception) { }
            assertTrue(database.sounds().all().isEmpty())
            assertFalse(store.file(id).exists()); assertFalse(store.staging(id).exists())
            assertTrue(store.assetFile(draft.assetId).exists())
            store.failure = null
            if (failure == "metadata") database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_sound")
            repository.save(draft, "Track 1")
            repository.delete(id)
        }
    }
    @Test fun explicitRetryAfterUncertainCommitCannotDuplicateAnAlreadySavedDraft() = runBlocking {
        val draft = draft()
        val saved = repository.save(draft, "Track 1")
        assertFalse(store.assetFile(draft.assetId).exists())
        repository.refresh()
        assertEquals(saved, repository.save(draft, "Not a rename"))
        assertEquals(listOf(saved), repository.sounds.value)
        assertEquals(1, database.sounds().all().size)
    }
    @Test fun recoveryCleansInterruptedSavesAndDeadDraftsButPreservesCommittedRecordings() = runBlocking {
        val saved = repository.save(draft(), "Track 1")
        val abandoned = draft()
        val orphan = UUID.randomUUID().toString()
        fixture.copyTo(store.file(orphan))
        store.staging(UUID.randomUUID().toString()).writeText("partial")
        val afterProcessDeath = SoundFileStore(store.directory)
        repository = LocalSoundRepository(database, afterProcessDeath)
        repeat(2) { repository.refresh() }
        assertEquals(listOf(saved), repository.sounds.value)
        assertTrue(store.file(saved.id).exists())
        assertFalse(store.file(orphan).exists())
        assertFalse(store.assetFile(abandoned.assetId).exists())
        assertTrue(store.directory.listFiles()!!.none { it.name.endsWith(".tmp") })
    }
    @Test fun namesAreTrimmedCanRepeatAndBlankUsesCurrentSavedCount() = runBlocking {
        val first = repository.save(draft(), "  Bird song  ")
        val second = repository.save(draft(), "Bird song")
        val third = repository.save(draft(), "   ")
        assertEquals("Bird song", first.name)
        assertEquals(first.name, second.name)
        assertNotEquals(first.id, second.id)
        assertEquals("Track 3", third.name)
        repository.delete(third.id)
        assertEquals("Track 3", repository.save(draft(), "").name)
        repository.refresh()
        assertEquals(2, repository.sounds.value.count { it.name == "Bird song" })
    }
    @Test fun deletionFailuresPreserveOrRemoveMetadataAtTheCorrectCommitBoundary() = runBlocking {
        val saved = repository.save(draft(), "Track 1")
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_sound_delete BEFORE DELETE ON sounds BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { repository.delete(saved.id); fail("Expected DB failure") } catch (_: Exception) { }
        assertEquals(1, database.sounds().all().size)
        assertTrue(store.file(saved.id).exists())
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_sound_delete")
        store.failure = "cleanup"
        try { repository.delete(saved.id); fail("Expected cleanup failure") } catch (_: IOException) { }
        assertTrue(database.sounds().all().isEmpty())
        assertTrue(repository.sounds.value.isEmpty())
        assertTrue(store.file(saved.id).exists())
        store.failure = null
        repeat(2) { repository.refresh() }
        assertFalse(store.file(saved.id).exists())
    }
    @Test fun invalidMetadataCannotDeleteOutsideOwnedStorageAndCorruptAudioIsNotListed() = runBlocking {
        val outside = File(root, "original.m4a").also { fixture.copyTo(it) }
        database.sounds().insert(SoundEntity(UUID.randomUUID().toString(), "../original.m4a", 1, 1, "Track 1"))
        val corrupt = UUID.randomUUID().toString()
        store.file(corrupt).writeText("not audio")
        database.sounds().insert(SoundEntity(corrupt, "$corrupt.m4a", 1, 1, "Track 1"))
        repository.refresh()
        assertTrue(outside.exists())
        assertTrue(repository.sounds.value.isEmpty())
        assertFalse(store.file(corrupt).exists())
    }
    @Test fun additiveMigrationPreservesPhotosAndCreatesSoundsWithoutDestructiveReset() = runBlocking {
        val file = File(root, "version-one.db")
        val id = UUID.randomUUID().toString()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE photos (id TEXT NOT NULL, fileName TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            old.execSQL("INSERT INTO photos VALUES (?, ?, ?)", arrayOf<Any>(id, "$id.webp", 1234))
            old.version = 1
        }
        val migrated = Room.databaseBuilder(context, PhotoDatabase::class.java, file.path)
            .addMigrations(PhotoDatabase.MIGRATION_1_2, PhotoDatabase.MIGRATION_2_3, PhotoDatabase.MIGRATION_3_4).build()
        try {
            assertEquals(id, migrated.photos().all().single().id)
            assertEquals(1234L, migrated.photos().all().single().createdAt)
            migrated.sounds().insert(SoundEntity(id, "$id.m4a", 800, 4321, "Track 1"))
            assertEquals(1, migrated.sounds().all().size)
            assertEquals(1, migrated.photos().all().size)
        } finally { migrated.close() }
    }
    @Test fun versionTwoMigrationNamesExistingSoundsWithoutChangingTheirIdentityOrFiles() = runBlocking {
        val file = File(root, "version-two.db")
        val olderId = UUID.randomUUID().toString()
        val newerId = UUID.randomUUID().toString()
        val audio = File(root, "$olderId.m4a").also { fixture.copyTo(it) }
        val original = audio.readBytes()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE photos (id TEXT NOT NULL, fileName TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            old.execSQL("CREATE TABLE sounds (id TEXT NOT NULL, fileName TEXT NOT NULL, durationMs INTEGER NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            old.execSQL("INSERT INTO photos VALUES (?, ?, ?)", arrayOf<Any>(olderId, "$olderId.webp", 1234))
            old.execSQL("INSERT INTO sounds VALUES (?, ?, ?, ?)", arrayOf<Any>(newerId, "$newerId.m4a", 900, 2000))
            old.execSQL("INSERT INTO sounds VALUES (?, ?, ?, ?)", arrayOf<Any>(olderId, "$olderId.m4a", 800, 1000))
            old.version = 2
        }
        val migrated = Room.databaseBuilder(context, PhotoDatabase::class.java, file.path)
            .addMigrations(PhotoDatabase.MIGRATION_2_3, PhotoDatabase.MIGRATION_3_4).build()
        try {
            val rows = migrated.sounds().all()
            assertEquals(listOf(newerId, olderId), rows.map { it.id })
            assertEquals(listOf("Track 2", "Track 1"), rows.map { it.name })
            assertEquals(listOf(900L, 800L), rows.map { it.durationMs })
            assertEquals(listOf(2000L, 1000L), rows.map { it.createdAt })
            assertEquals(listOf("$newerId.m4a", "$olderId.m4a"), rows.map { it.fileName })
            assertEquals(olderId, migrated.photos().all().single().id)
            assertArrayEquals(original, audio.readBytes())
        } finally { migrated.close() }
    }
}
