package com.example.puzzlebubbles.presentation.library

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puzzlebubbles.data.photos.PhotoImageStore
import com.example.puzzlebubbles.data.photos.PhotoProcessor
import com.example.puzzlebubbles.domain.LibraryPhoto
import com.example.puzzlebubbles.domain.PhotoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class PhotoModal(val assetId: String, val savedId: String? = null)
enum class PhotoError { LOAD, IMPORT, SAVE, DELETE }

class PhotosViewModel(
    private val repository: PhotoRepository,
    private val prepare: suspend (Uri) -> String,
    private val discard: (String) -> Unit,
) : ViewModel() {
    var photos by mutableStateOf<List<LibraryPhoto>>(emptyList()); private set
    var modal by mutableStateOf<PhotoModal?>(null); private set
    var busy by mutableStateOf(false); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<PhotoError?>(null); private set
    var focusId by mutableStateOf<String?>(null); private set
    var picking by mutableStateOf(false); private set

    init {
        viewModelScope.launch { repository.photos.collect { photos = it } }
        reload()
    }
    fun reload() {
        if (busy) return
        loading = true
        busy = true
        viewModelScope.launch {
            try { repository.refresh(); error = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = PhotoError.LOAD }
            finally { loading = false; busy = false }
        }
    }
    fun beginPick(): Boolean {
        if (busy || modal != null || picking) return false
        picking = true
        return true
    }
    fun pickerFailed() { picking = false; error = PhotoError.IMPORT }
    fun selected(uri: Uri?) {
        picking = false
        if (uri == null || busy || modal != null) return
        busy = true
        error = null
        viewModelScope.launch {
            var draft: String? = null
            try {
                draft = prepare(uri)
                modal = PhotoModal(draft)
            } catch (e: CancellationException) { draft?.let(discard); throw e }
            catch (_: Exception) { error = PhotoError.IMPORT }
            catch (_: OutOfMemoryError) { error = PhotoError.IMPORT }
            finally { busy = false }
        }
    }
    fun open(photo: LibraryPhoto) {
        if (busy || modal != null) return
        error = null
        modal = PhotoModal(photo.assetId, photo.id)
    }
    fun close() {
        if (busy) return
        val current = modal ?: return
        modal = null // Clear synchronously; rapid taps cannot process this modal twice.
        error = null
        if (current.savedId == null) discard(current.assetId)
    }
    fun save() {
        if (busy) return
        val current = modal ?: return
        if (current.savedId != null) { close(); return }
        busy = true
        error = null
        viewModelScope.launch {
            try {
                repository.refresh()
                val saved = repository.save(current.assetId)
                focusId = saved.id
                modal = null
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = PhotoError.SAVE }
            finally { busy = false }
        }
    }
    fun delete() {
        if (busy) return
        val id = modal?.savedId ?: return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                repository.refresh()
                repository.delete(id)
                modal = null
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = PhotoError.DELETE }
            finally { busy = false }
        }
    }
    fun focused() { focusId = null }
    override fun onCleared() {
        modal?.takeIf { it.savedId == null }?.let { discard(it.assetId) }
    }

    class Factory(
        private val repository: PhotoRepository,
        private val images: PhotoImageStore,
        private val processor: PhotoProcessor,
        private val resolver: ContentResolver,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PhotosViewModel::class.java))
            val vm = PhotosViewModel(repository, prepare = { uri ->
                val bitmap = withContext(Dispatchers.IO) { processor.prepare(resolver, uri) }
                images.putDraft(UUID.randomUUID().toString(), bitmap)
            }, discard = images::discard)
            return modelClass.cast(vm)!!
        }
    }
}
