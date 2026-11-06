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

data class PhotoSource(val assetId: String, val width: Int, val height: Int)
data class PhotoModal(val source: PhotoSource, val savedId: String? = null,
                      val crop: CropTransform = CropTransform(source.width, source.height)) {
    val assetId: String get() = source.assetId
}
enum class PhotoError { LOAD, IMPORT, SAVE, DELETE, IN_USE }

class PhotosViewModel(
    private val repository: PhotoRepository,
    private val prepare: suspend (Uri) -> PhotoSource,
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
                val source = prepare(uri)
                draft = source.assetId
                modal = PhotoModal(source)
            } catch (e: CancellationException) { draft?.let(discard); throw e }
            catch (_: Exception) { error = PhotoError.IMPORT }
            catch (_: OutOfMemoryError) { error = PhotoError.IMPORT }
            finally { busy = false }
        }
    }
    fun open(photo: LibraryPhoto) {
        if (busy || modal != null) return
        error = null
        modal = PhotoModal(PhotoSource(photo.assetId, 1024, 1024), photo.id)
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
        if (current.savedId != null && current.crop.unchanged) { close(); return }
        busy = true
        error = null
        viewModelScope.launch {
            try {
                repository.refresh()
                if (current.savedId == null) {
                    val saved = repository.save(current.assetId, current.crop.region)
                    focusId = saved.id
                } else repository.replace(current.savedId, current.crop.region)
                modal = null
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = PhotoError.SAVE }
            catch (_: OutOfMemoryError) { error = PhotoError.SAVE }
            finally { busy = false }
        }
    }
    fun transform(zoomChange: Float, panX: Float, panY: Float, focalX: Float, focalY: Float, viewportSide: Float) {
        if (busy) return
        val current = modal ?: return
        modal = current.copy(crop = current.crop.gesture(zoomChange, panX, panY, focalX, focalY, viewportSide))
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
            catch (_: com.example.puzzlebubbles.domain.MediaInUseException) { error = PhotoError.IN_USE }
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
                PhotoSource(images.putDraft(UUID.randomUUID().toString(), bitmap), bitmap.width, bitmap.height)
            }, discard = images::discard)
            return modelClass.cast(vm)!!
        }
    }
}
