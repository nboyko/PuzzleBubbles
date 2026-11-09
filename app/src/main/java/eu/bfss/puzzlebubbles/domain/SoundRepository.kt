package eu.bfss.puzzlebubbles.domain

import kotlinx.coroutines.flow.StateFlow

data class LibrarySound(val id: String, val durationMs: Long, val createdAt: Long, val name: String) {
    val assetId: String get() = "library-sound:$id"
}

data class SoundDraft(val assetId: String, val durationMs: Long)

fun defaultSoundName(savedCount: Int) = "Track ${savedCount + 1}"

interface SoundRepository {
    val sounds: StateFlow<List<LibrarySound>>
    suspend fun refresh()
    suspend fun save(draft: SoundDraft, name: String): LibrarySound
    suspend fun delete(id: String)
}

interface SoundRecorder {
    fun cancelPending() {}
    suspend fun start(onLimit: () -> Unit, onError: () -> Unit)
    suspend fun finish(): SoundDraft
    suspend fun discard()
}

data class SoundPlaybackState(
    val assetId: String? = null, val positionMs: Long = 0, val durationMs: Long = 0,
    val playing: Boolean = false, val preparing: Boolean = false, val failed: Boolean = false,
)

interface SoundPlayback {
    val playback: StateFlow<SoundPlaybackState>
    fun toggle(assetId: String)
    fun stop()
    fun release()
}
