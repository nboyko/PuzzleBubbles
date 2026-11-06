package com.example.puzzlebubbles

import android.graphics.Bitmap
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.puzzlebubbles.data.*
import com.example.puzzlebubbles.data.photos.*
import com.example.puzzlebubbles.data.sounds.*
import com.example.puzzlebubbles.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class CustomGameStorageTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var root: File
    private lateinit var db: PhotoDatabase
    private lateinit var games: LibraryGameRepository
    private lateinit var photos: LocalPhotoRepository
    private lateinit var sounds: LocalSoundRepository
    private lateinit var images: PhotoImageStore
    private lateinit var audio: SoundFileStore
    private val ids = List(3) { UUID.randomUUID().toString() }
    private val soundId = UUID.randomUUID().toString()
    private fun game(id: String = "whats-that-${UUID.randomUUID()}") = CustomGame(id, "Animals", ids, listOf(CustomQuestion(ids[0], soundId)))
    @Before fun setup() = runBlocking {
        root = File(context.cacheDir, "custom-game-${UUID.randomUUID()}").apply { mkdirs() }
        db = Room.databaseBuilder(context, PhotoDatabase::class.java, File(root, "test.db").path).build()
        images = PhotoImageStore(File(root, "images")); images.ensureDirectory()
        ids.forEach { id ->
            val bitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
            try { images.write(id, bitmap); images.promote(id) } finally { bitmap.recycle() }
            db.photos().insert(PhotoEntity(id, "$id.webp", 100))
        }
        audio = SoundFileStore(File(root, "audio")); audio.ensureDirectory()
        aacFixture(audio.file(soundId))
        db.sounds().insert(SoundEntity(soundId, "$soundId.m4a", audio.duration(audio.file(soundId)), 100, "Test sound"))
        photos = LocalPhotoRepository(db, images); photos.refresh()
        sounds = LocalSoundRepository(db, audio); sounds.refresh()
        games = LibraryGameRepository(db)
    }
    @After fun cleanup() { db.close(); root.deleteRecursively() }
    @Test fun savesReloadsAndDeletesReferencesWithoutTouchingFiles() = runBlocking {
        val first = game(); val second = game()
        games.saveCustomGame(first); games.saveCustomGame(first); games.saveCustomGame(second)
        val list = games.observeWhatsThatGames().first()
        assertEquals(3, list.size)
        assertTrue(list.first().isBuiltIn)
        assertEquals(listOf("Animals", "Animals", "Animals"), list.map { it.name })
        val restored = LibraryGameRepository(db).loadWhatsThatGame(first.id)
        assertEquals(first.playable(), restored)
        for (id in ids) {
            assertTrue(runCatching { photos.delete(id) }.exceptionOrNull() is MediaInUseException)
            assertTrue(images.file(id).exists())
        }
        assertTrue(runCatching { sounds.delete(soundId) }.exceptionOrNull() is MediaInUseException)
        photos.replace(ids[0], PhotoCrop(0f, 0f, 1024f))
        games.deleteCustomGame(first.id)
        assertTrue(runCatching { photos.delete(ids[2]) }.exceptionOrNull() is MediaInUseException)
        games.deleteCustomGame(second.id)
        assertTrue(images.file(ids[0]).exists()); assertTrue(audio.file(soundId).exists())
        photos.replace(ids[0], PhotoCrop(0f, 0f, 1024f))
        photos.delete(ids[2]); sounds.delete(soundId)
        assertFalse(images.file(ids[2]).exists()); assertFalse(audio.file(soundId).exists())
        assertTrue(runCatching { games.deleteCustomGame("whats-that") }.isFailure)
        assertEquals(10, games.loadWhatsThatGame("whats-that").questions.size)
    }
    @Test fun failedSaveRollsBackAndExplicitRetryUsesSameIdentity() = runBlocking {
        val game = game()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_game BEFORE INSERT ON game_questions BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertTrue(runCatching { games.saveCustomGame(game) }.isFailure)
        assertNull(db.games().find(game.id)); assertTrue(db.games().images(game.id).isEmpty())
        assertFalse(db.games().usesPhoto(ids[0]))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_game")
        games.saveCustomGame(game); games.saveCustomGame(game)
        assertEquals(2, games.observeWhatsThatGames().first().size)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_delete BEFORE DELETE ON custom_games BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
        assertTrue(runCatching { games.deleteCustomGame(game.id) }.isFailure)
        assertEquals(game.playable(), games.loadWhatsThatGame(game.id))
        assertTrue(db.games().usesPhoto(ids[2]))
    }
    @Test fun mediaDeleteRacingSaveCannotLeaveDanglingReferences() = runBlocking {
        val game = game()
        val save = async(Dispatchers.IO) { runCatching { games.saveCustomGame(game) } }
        val delete = async(Dispatchers.IO) { runCatching { photos.delete(ids[2]) } }
        val saved = save.await(); val deleted = delete.await()
        assertTrue(saved.isSuccess.xor(deleted.isSuccess))
        assertEquals(saved.isSuccess, db.games().usesPhoto(ids[2]))
        assertEquals(saved.isSuccess, db.photos().find(ids[2]) != null)
    }
    @Test fun versionThreeMigrationPreservesMediaAndSupportsGames() = runBlocking {
        val file = File(root, "old.db")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE photos (id TEXT NOT NULL, fileName TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            old.execSQL("CREATE TABLE sounds (id TEXT NOT NULL, fileName TEXT NOT NULL, durationMs INTEGER NOT NULL, createdAt INTEGER NOT NULL, name TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))")
            ids.forEach { old.execSQL("INSERT INTO photos VALUES (?, ?, ?)", arrayOf<Any>(it, "$it.webp", 100L)) }
            old.execSQL("INSERT INTO sounds VALUES (?, ?, ?, ?, ?)", arrayOf<Any>(soundId, "$soundId.m4a", 1000L, 100L, "Track"))
            old.version = 3
        }
        val migrated = Room.databaseBuilder(context, PhotoDatabase::class.java, file.path).addMigrations(PhotoDatabase.MIGRATION_3_4).build()
        try {
            assertEquals(3, migrated.photos().all().size)
            assertEquals("Track", migrated.sounds().all().single().name)
            val repository = LibraryGameRepository(migrated)
            val game = game(); repository.saveCustomGame(game)
            assertEquals(game.playable(), repository.loadWhatsThatGame(game.id))
        } finally { migrated.close() }
    }
}
