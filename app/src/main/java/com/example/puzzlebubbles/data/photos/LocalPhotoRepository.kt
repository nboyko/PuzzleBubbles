package com.example.puzzlebubbles.data.photos

import androidx.room.withTransaction
import com.example.puzzlebubbles.domain.LibraryPhoto
import com.example.puzzlebubbles.domain.PhotoRepository
import com.example.puzzlebubbles.domain.PhotoCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class LocalPhotoRepository(
    private val database: PhotoDatabase,
    private val images: PhotoImageStore,
) : PhotoRepository {
    private val mutex = Mutex()
    private val dao get() = database.photos()
    private val content = MutableStateFlow<List<LibraryPhoto>>(emptyList())
    override val photos = content.asStateFlow()
    private var ready = false
    private val revisions = mutableMapOf<String, Long>()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            ready = false
            images.ensureDirectory()
            val rows = dao.all()
            val valid = rows.filter { row ->
                row.fileName == "${row.id}.webp" && runCatching { images.valid(row.id) }.getOrDefault(false)
            }
            database.withTransaction {
                (rows - valid.toSet()).forEach { dao.delete(it.id) }
            }
            val retained = valid.map { it.fileName }.toSet()
            val files = images.directory.listFiles() ?: throw IOException("Cannot read Library storage")
            files.filter { it.isFile && (it.name.endsWith(".tmp") ||
                (it.name.endsWith(".webp") && it.name !in retained)) }.forEach(images::remove)
            publish()
            ready = true
        }
    }

    override suspend fun save(draftAssetId: String, crop: PhotoCrop?): LibraryPhoto = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            check(ready) { "Library not ready" }
            val id = UUID.randomUUID().toString()
            val photo = PhotoEntity(id, "$id.webp", System.currentTimeMillis())
            check(dao.find(id) == null && !images.file(id).exists() && !images.staging(id).exists())
            try {
                val output = PhotoProcessor.render(images.draft(draftAssetId), crop)
                try { images.write(id, output) } finally { output.recycle() }
                images.promote(id)
                database.withTransaction { dao.insert(photo) }
            } catch (failure: Exception) {
                // If commit succeeded despite an error, do not remove the committed image.
                val committed = try { dao.find(id) != null } catch (lookup: Exception) {
                    ready = false
                    failure.addSuppressed(lookup)
                    throw failure
                }
                if (!committed) {
                    try {
                        images.remove(images.staging(id))
                        images.remove(images.file(id))
                    } catch (cleanup: Exception) {
                        ready = false
                        failure.addSuppressed(cleanup)
                    }
                    throw failure
                }
            }
            content.value = listOf(LibraryPhoto(photo.id, photo.createdAt)) + content.value
            images.discard(draftAssetId)
            LibraryPhoto(photo.id, photo.createdAt)
        }
    }

    override suspend fun replace(id: String, crop: PhotoCrop) = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            check(ready) { "Library not ready" }
            check(dao.find(id) != null && images.valid(id)) { "Photo unavailable" }
            val source = images.image("library-photo:$id", thumbnail = false) ?: throw IOException("Photo unavailable")
            try {
                val output = PhotoProcessor.render(source, crop)
                try { images.write(id, output) } finally { output.recycle() }
                images.replace(id)
            } catch (failure: Exception) {
                // Existing metadata and the previous final image must never be rolled back/deleted.
                try { images.remove(images.staging(id)) } catch (cleanup: Exception) {
                    ready = false
                    failure.addSuppressed(cleanup)
                }
                throw failure
            } finally { source.recycle() }
            val revision = (revisions[id] ?: 0L) + 1
            revisions[id] = revision
            content.value = content.value.map { if (it.id == id) it.copy(revision = revision) else it }
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            check(ready) { "Library not ready" }
            images.file(id) // Validate ID before any metadata mutation.
            database.withTransaction {
                if (database.games().usesPhoto(id)) throw com.example.puzzlebubbles.domain.MediaInUseException()
                dao.delete(id)
            }
            revisions.remove(id)
            content.value = content.value.filterNot { it.id == id }
            try { images.remove(images.file(id)) } catch (failure: Exception) {
                ready = false
                throw failure
            }
        }
    }

    private suspend fun publish() {
        content.value = dao.all().map { LibraryPhoto(it.id, it.createdAt, revisions[it.id] ?: 0) }
    }
}
