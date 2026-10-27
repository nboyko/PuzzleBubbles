package com.example.puzzlebubbles.data.sounds

import androidx.room.withTransaction
import com.example.puzzlebubbles.data.photos.PhotoDatabase
import com.example.puzzlebubbles.domain.LibrarySound
import com.example.puzzlebubbles.domain.SoundDraft
import com.example.puzzlebubbles.domain.SoundRepository
import com.example.puzzlebubbles.domain.defaultSoundName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class LocalSoundRepository(private val database: PhotoDatabase, private val files: SoundFileStore) : SoundRepository {
    private val mutex = Mutex()
    private val dao get() = database.sounds()
    private val content = MutableStateFlow<List<LibrarySound>>(emptyList())
    override val sounds = content.asStateFlow()
    private var ready = false

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            ready = false
            files.ensureDirectory()
            val rows = dao.all()
            val valid = rows.filter { row -> row.fileName == "${row.id}.m4a" &&
                runCatching { files.duration(files.file(row.id)) == row.durationMs }.getOrDefault(false) }
            database.withTransaction { (rows - valid.toSet()).forEach { dao.delete(it.id) } }
            val retained = valid.map { it.fileName }.toSet()
            val disk = files.directory.listFiles() ?: throw IOException("Cannot read Sounds storage")
            disk.filter { it.isFile && (it.name.endsWith(".tmp") ||
                (it.name.endsWith(".m4a") && it.name !in retained)) }.forEach(files::remove)
            files.cleanDrafts()
            content.value = valid.map { it.model() }
            ready = true
        }
    }

    override suspend fun save(draft: SoundDraft, name: String): LibrarySound = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            check(ready)
            require(draft.assetId.startsWith("sound-draft:"))
            val id = draft.assetId.removePrefix("sound-draft:")
            val finalFile = files.file(id)
            // An explicit retry can follow an uncertain commit acknowledgement.
            // The draft's generated ID makes that retry idempotent, even if its
            // temporary file was already cleaned after the original commit.
            dao.find(id)?.let { existing ->
                check(existing.fileName == finalFile.name && files.duration(finalFile) == existing.durationMs)
                if (content.value.none { it.id == id }) content.value = listOf(existing.model()) + content.value
                runCatching { files.discard(id) }.onFailure { ready = false }
                return@withLock existing.model()
            }
            val source = files.draft(id)
            val row = SoundEntity(id, finalFile.name, files.duration(source), System.currentTimeMillis(),
                name.trim().ifBlank { defaultSoundName(dao.all().size) })
            try {
                files.stage(id, source)
                check(files.duration(files.staging(id)) == row.durationMs)
                files.promote(id)
                database.withTransaction { dao.insert(row) }
            } catch (failure: Exception) {
                val committed = try { dao.find(id) != null } catch (lookup: Exception) {
                    ready = false; failure.addSuppressed(lookup); throw failure
                }
                if (!committed) {
                    try { files.remove(files.staging(id)); files.remove(files.file(id)) }
                    catch (cleanup: Exception) { ready = false; failure.addSuppressed(cleanup) }
                    throw failure
                }
            }
            content.value = listOf(row.model()) + content.value
            // The file and row have committed; draft cleanup failure must not invite another Save.
            runCatching { files.discard(id) }.onFailure { ready = false }
            row.model()
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            check(ready)
            files.file(id)
            database.withTransaction { dao.delete(id) }
            content.value = content.value.filterNot { it.id == id }
            try { files.remove(files.file(id)) } catch (failure: Exception) { ready = false; throw failure }
        }
    }
    private fun SoundEntity.model() = LibrarySound(id, durationMs, createdAt, name)
}
