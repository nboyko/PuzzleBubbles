package com.example.puzzlebubbles.data

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.example.puzzlebubbles.domain.SoundPlayback
import com.example.puzzlebubbles.domain.SoundPlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import com.example.puzzlebubbles.data.sounds.SoundLevels
import com.example.puzzlebubbles.data.sounds.SoundNormalizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocalAudioPlayer(context: Context, private val soundFile: ((String) -> File)? = null) : AudioPlayer, SoundPlayback {
    private val applicationContext = context.applicationContext
    private var player: MediaPlayer? = null
    private val playbackState = MutableStateFlow(SoundPlaybackState())
    override val playback = playbackState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progress: Job? = null
    private val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focus: AudioFocusRequest? = null
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    /** Playback volume per bundled asset, measured once so all sounds share one loudness. */
    private val bundledVolumes = ConcurrentHashMap<String, Float>()

    init {
        scope.launch(Dispatchers.Default) {
            for (id in LocalAudioAssetResolver.audioAssetIds) {
                runCatching {
                    applicationContext.resources.openRawResourceFd(LocalAudioAssetResolver.resolveAudio(id)).use { fd ->
                        SoundNormalizer.decode { it.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
                    }
                }.onSuccess { bundledVolumes[id] = SoundLevels.playbackVolume(SoundLevels.level(it)) }
            }
        }
    }

    /** Normalized volume for a bundled asset; effects sit [SFX_OFFSET] below content. */
    internal fun bundledVolume(assetId: String): Float =
        (bundledVolumes[assetId] ?: 1f) * if (assetId.startsWith("sfx-")) SFX_OFFSET else 1f

    override fun play(assetId: String) {
        if (assetId.startsWith("library-sound:")) {
            stop()
            toggle(assetId)
            return
        }
        stop()
        val created = MediaPlayer.create(applicationContext, LocalAudioAssetResolver.resolveAudio(assetId))
            ?: error("Unable to create audio player for asset: $assetId")
        player = created
        val volume = bundledVolume(assetId)
        created.setVolume(volume, volume)
        created.setOnCompletionListener { completed ->
            completed.release()
            if (player === completed) player = null
        }
        created.start()
    }

    override fun stop() {
        progress?.cancel()
        progress = null
        abandonFocus()
        player?.let { current ->
            try {
                current.setOnPreparedListener(null)
                current.setOnCompletionListener(null)
                current.setOnErrorListener(null)
                runCatching { if (current.isPlaying) current.stop() }
            } finally { current.release() }
        }
        player = null
        playbackState.value = SoundPlaybackState()
    }

    override fun toggle(assetId: String) {
        val state = playbackState.value
        if (state.preparing) return
        if (state.assetId == assetId && player != null) {
            if (state.playing) pauseSound() else startSound(checkNotNull(player))
            return
        }
        stop()
        playbackState.value = SoundPlaybackState(assetId = assetId, preparing = true)
        try {
            val created = MediaPlayer()
            player = created
            created.setAudioAttributes(attributes)
            created.setOnPreparedListener { ready ->
                if (player === ready) {
                    playbackState.value = SoundPlaybackState(assetId, durationMs = ready.duration.toLong())
                    startSound(ready)
                }
            }
            created.setOnCompletionListener { completed ->
                if (player === completed) {
                    val duration = playbackState.value.durationMs
                    stop()
                    playbackState.value = SoundPlaybackState(assetId, durationMs = duration)
                }
            }
            created.setOnErrorListener { failed, _, _ ->
                if (player === failed) soundFailed(assetId)
                true
            }
            created.setDataSource(checkNotNull(soundFile) { "Library sounds are not configured" }(assetId).path)
            created.prepareAsync()
        } catch (_: Exception) { soundFailed(assetId) }
    }

    private fun startSound(current: MediaPlayer) {
        try {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { change -> if (change < 0) pauseSound() }.build()
            focus = request
            if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                soundFailed(playbackState.value.assetId)
                return
            }
            current.start()
            playbackState.value = playbackState.value.copy(playing = true)
            progress?.cancel()
            progress = scope.launch {
                try {
                    while (player === current && playbackState.value.playing) {
                        playbackState.value = playbackState.value.copy(positionMs = current.currentPosition.toLong())
                        delay(100)
                    }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { if (player === current) soundFailed(playbackState.value.assetId) }
            }
        } catch (_: Exception) { soundFailed(playbackState.value.assetId) }
    }
    private fun pauseSound() {
        val current = player ?: return
        if (!playbackState.value.playing) return
        try {
            current.pause()
            progress?.cancel()
            playbackState.value = playbackState.value.copy(playing = false, positionMs = current.currentPosition.toLong())
            abandonFocus()
        } catch (_: Exception) { soundFailed(playbackState.value.assetId) }
    }
    private fun abandonFocus() {
        focus?.let { audioManager.abandonAudioFocusRequest(it) }
        focus = null
    }
    private fun soundFailed(assetId: String?) {
        stop()
        playbackState.value = SoundPlaybackState(assetId, failed = true)
    }

    override suspend fun playToEnd(assetId: String) = suspendCancellableCoroutine<Unit> { continuation ->
        play(assetId)
        val current = requireNotNull(player)
        current.setOnCompletionListener { completed ->
            completed.release()
            if (player === completed) player = null
            if (continuation.isActive) continuation.resume(Unit)
        }
        current.setOnErrorListener { failed, _, _ ->
            failed.release()
            if (player === failed) player = null
            if (continuation.isActive) continuation.resume(Unit)
            true
        }
        continuation.invokeOnCancellation {
            if (player === current) stop()
        }
    }

    override fun release() { stop(); scope.cancel() }

    private companion object {
        /** Short, bright feedback effects sound louder at equal measured level: -6 dB. */
        const val SFX_OFFSET = 0.5f
    }
}
