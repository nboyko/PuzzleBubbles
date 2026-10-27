package com.example.puzzlebubbles.presentation.library

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.puzzlebubbles.PuzzleBubblesApplication
import com.example.puzzlebubbles.data.LocalAudioPlayer
import com.example.puzzlebubbles.data.sounds.AndroidSoundRecorder
import com.example.puzzlebubbles.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RecordingStep { IDLE, PERMISSION, STARTING, RECORDING, STOPPING, PREVIEW, SAVING }
enum class SoundError { LOAD, PERMISSION, RECORD, SAVE, DELETE, CLEANUP }
data class RecordingModal(
    val step: RecordingStep = RecordingStep.IDLE, val draft: SoundDraft? = null,
    val elapsedMs: Long = 0, val error: SoundError? = null, val settingsRequired: Boolean = false,
    val name: String = "",
)
data class SoundsUiState(
    val sounds: List<LibrarySound> = emptyList(), val loading: Boolean = true,
    val modal: RecordingModal? = null, val error: SoundError? = null,
    val deleteId: String? = null, val deleting: Boolean = false, val cleaning: Boolean = false,
    val permissionPending: Boolean = false,
) {
    val committing get() = deleting || modal?.step == RecordingStep.SAVING
    val blocked get() = loading || committing || cleaning
}

class SoundsViewModel(
    private val repository: SoundRepository, private val recorder: SoundRecorder,
    private val player: SoundPlayback, private val now: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    var uiState by mutableStateOf(SoundsUiState()); private set
    var playback by mutableStateOf(SoundPlaybackState()); private set
    private var generation = 0L
    private var permissionToken: Long? = null
    private var foreground = false
    private var timer: Job? = null
    private var saving: Job? = null
    private var limitPending = false
    private var suggestedName = ""

    init {
        viewModelScope.launch { repository.sounds.collect { uiState = uiState.copy(sounds = it) } }
        viewModelScope.launch { player.playback.collect { playback = it } }
        reload()
    }
    fun reload() {
        if (uiState.committing || uiState.cleaning) return
        uiState = uiState.copy(loading = true, error = null)
        viewModelScope.launch {
            try { repository.refresh() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { uiState = uiState.copy(error = SoundError.LOAD) }
            finally { uiState = uiState.copy(loading = false) }
        }
    }
    fun foreground() { foreground = true }
    fun open() {
        if (!foreground || uiState.blocked || uiState.error == SoundError.CLEANUP || uiState.modal != null || uiState.deleteId != null) return
        player.stop()
        generation++
        suggestedName = defaultSoundName(uiState.sounds.size)
        uiState = uiState.copy(modal = RecordingModal(), error = null)
    }
    /** True means the UI should launch exactly one platform permission request. */
    fun record(permissionGranted: Boolean): Boolean {
        if (!foreground || uiState.blocked || permissionToken != null || uiState.modal?.step != RecordingStep.IDLE) return false
        if (permissionGranted) start(generation)
        else {
            permissionToken = generation
            uiState = uiState.copy(permissionPending = true, modal = RecordingModal(RecordingStep.PERMISSION))
            return true
        }
        return false
    }
    fun permissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        val token = permissionToken ?: return
        permissionToken = null
        uiState = uiState.copy(permissionPending = false)
        if (token != generation || !foreground || uiState.modal?.step != RecordingStep.PERMISSION) return
        if (granted) start(token)
        else uiState = uiState.copy(modal = RecordingModal(error = SoundError.PERMISSION, settingsRequired = permanentlyDenied))
    }
    private fun start(token: Long) {
        player.stop()
        limitPending = false
        uiState = uiState.copy(modal = RecordingModal(RecordingStep.STARTING))
        viewModelScope.launch {
            try {
                recorder.start(onLimit = {
                    viewModelScope.launch {
                        if (token == generation) {
                            if (uiState.modal?.step == RecordingStep.STARTING) limitPending = true else stopRecording()
                        }
                    }
                }, onError = { viewModelScope.launch { failRecording(token) } })
                // The cancellation owner queues cleanup; stale completions must not
                // discard a newer session after that cleanup has finished.
                if (token != generation || !foreground) return@launch
                val deadline = RecordingDeadline(now())
                uiState = uiState.copy(modal = RecordingModal(RecordingStep.RECORDING))
                if (limitPending) stopRecording()
                else timer = viewModelScope.launch {
                    while (token == generation && uiState.modal?.step == RecordingStep.RECORDING) {
                        uiState = uiState.copy(modal = uiState.modal?.copy(elapsedMs = deadline.elapsed(now())))
                        if (deadline.expired(now())) { stopRecording(); break }
                        delay(50)
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failRecording(token) }
        }
    }
    fun stopRecording() {
        if (uiState.modal?.step != RecordingStep.RECORDING) return
        val token = generation
        timer?.cancel()
        uiState = uiState.copy(modal = uiState.modal?.copy(step = RecordingStep.STOPPING))
        viewModelScope.launch {
            try {
                val draft = recorder.finish()
                if (token == generation && foreground) uiState = uiState.copy(modal = RecordingModal(RecordingStep.PREVIEW, draft, name = suggestedName))
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failRecording(token) }
        }
    }
    private fun failRecording(token: Long) {
        if (token != generation || uiState.modal == null || uiState.committing) return
        generation++
        timer?.cancel()
        uiState = uiState.copy(modal = RecordingModal(error = SoundError.RECORD))
        cleanRecorder()
    }
    fun close() {
        if (uiState.committing) return
        if (uiState.modal == null) return
        generation++
        timer?.cancel()
        player.stop()
        uiState = uiState.copy(modal = null)
        cleanRecorder()
    }
    private fun cleanRecorder() {
        recorder.cancelPending()
        uiState = uiState.copy(cleaning = true)
        viewModelScope.launch {
            try {
                recorder.discard()
                if (uiState.error == SoundError.CLEANUP) uiState = uiState.copy(error = null)
            }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { uiState = uiState.copy(modal = null, error = SoundError.CLEANUP) }
            finally { uiState = uiState.copy(cleaning = false) }
        }
    }
    fun retry() { if (uiState.error == SoundError.CLEANUP) cleanRecorder() else reload() }
    fun nameChanged(name: String) {
        val modal = uiState.modal ?: return
        if (modal.step == RecordingStep.PREVIEW && !uiState.blocked) {
            uiState = uiState.copy(modal = modal.copy(name = name))
        }
    }
    fun save() {
        val modal = uiState.modal ?: return
        val draft = modal.draft ?: return
        if (modal.step != RecordingStep.PREVIEW || uiState.blocked) return
        player.stop()
        uiState = uiState.copy(modal = modal.copy(step = RecordingStep.SAVING, error = null))
        saving = viewModelScope.launch {
            try {
                repository.refresh()
                repository.save(draft, modal.name.trim().ifBlank { suggestedName })
                uiState = uiState.copy(modal = null)
                cleanRecorder()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                uiState = uiState.copy(modal = modal.copy(error = SoundError.SAVE))
                // A Save may finish after genuine backgrounding. On failure there
                // is still only an unsaved draft, which must now be discarded.
                if (!foreground) {
                    uiState = uiState.copy(error = SoundError.SAVE)
                    close()
                }
            }
        }
    }
    fun toggle(assetId: String) {
        if (!foreground || uiState.blocked || uiState.deleteId != null) return
        val modal = uiState.modal
        if (modal != null && (modal.step != RecordingStep.PREVIEW || modal.draft?.assetId != assetId)) return
        player.toggle(assetId)
    }
    fun requestDelete(id: String) {
        if (!foreground || uiState.blocked || uiState.modal != null || uiState.deleteId != null || uiState.sounds.none { it.id == id }) return
        player.stop()
        uiState = uiState.copy(deleteId = id, error = null)
    }
    fun cancelDelete() { if (!uiState.deleting) uiState = uiState.copy(deleteId = null, error = null) }
    fun delete() {
        val id = uiState.deleteId ?: return
        if (uiState.blocked) return
        uiState = uiState.copy(deleting = true, error = null)
        viewModelScope.launch {
            try {
                repository.refresh()
                repository.delete(id)
                uiState = uiState.copy(deleteId = null)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { uiState = uiState.copy(error = SoundError.DELETE) }
            finally { uiState = uiState.copy(deleting = false) }
        }
    }
    fun background() {
        foreground = false
        player.stop()
        if (uiState.committing) return
        close()
        cancelDelete()
    }
    override fun onCleared() {
        generation++
        timer?.cancel()
        player.release()
        recorder.cancelPending()
        // A committed Save may still be copying the draft in NonCancellable I/O.
        // Let it finish before removing its source, even when the owner is destroyed.
        val pendingSave = saving
        CoroutineScope(NonCancellable + Dispatchers.Main.immediate).launch {
            pendingSave?.join()
            runCatching { recorder.discard() }
        }
    }
    class Factory(private val app: PuzzleBubblesApplication) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SoundsViewModel::class.java))
            return modelClass.cast(SoundsViewModel(app.soundRepository,
                AndroidSoundRecorder(app, app.soundFiles), LocalAudioPlayer(app, app.soundFiles::assetFile)))!!
        }
    }
}
