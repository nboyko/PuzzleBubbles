package com.example.puzzlebubbles.data

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocalAudioPlayer(context: Context) : AudioPlayer {
    private val applicationContext = context.applicationContext
    private var player: MediaPlayer? = null

    override fun play(assetId: String) {
        stop()
        val created = MediaPlayer.create(applicationContext, LocalAudioAssetResolver.resolveAudio(assetId))
            ?: error("Unable to create audio player for asset: $assetId")
        player = created
        created.setOnCompletionListener { completed ->
            completed.release()
            if (player === completed) player = null
        }
        created.start()
    }

    override fun stop() {
        player?.let { current ->
            current.setOnCompletionListener(null)
            if (current.isPlaying) current.stop()
            current.release()
        }
        player = null
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

    override fun release() = stop()
}
