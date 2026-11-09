package com.example.puzzlebubbles.data.sounds

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.puzzlebubbles.domain.RecordingDeadline
import com.example.puzzlebubbles.domain.SoundDraft
import com.example.puzzlebubbles.domain.SoundRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AndroidSoundRecorder(private val context: Context, private val files: SoundFileStore) : SoundRecorder {
    private val mutex = Mutex()
    @Volatile private var recorder: MediaRecorder? = null
    @Volatile private var cancelled = false
    private var draftId: String? = null
    override fun cancelPending() { cancelled = true }

    override suspend fun start(onLimit: () -> Unit, onError: () -> Unit) = mutex.withLock {
        cancelled = false
        withContext(Dispatchers.IO + NonCancellable) {
            check(recorder == null && draftId == null)
            val (id, output) = files.newDraft()
            draftId = id
            // Native callbacks require a Looper; preparation/file I/O stays off the UI thread.
            val created = withContext(Dispatchers.Main.immediate) {
                if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context.applicationContext)
                else { @Suppress("DEPRECATION") MediaRecorder() }
            }
            recorder = created
            try {
                created.setAudioSource(MediaRecorder.AudioSource.MIC)
                created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                created.setAudioChannels(1)
                created.setAudioSamplingRate(44_100)
                created.setAudioEncodingBitRate(64_000)
                created.setMaxDuration(RecordingDeadline.LIMIT_MS.toInt())
                created.setOutputFile(output.path)
                created.setOnInfoListener { source, what, _ ->
                    if (source === recorder && what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onLimit()
                }
                created.setOnErrorListener { source, _, _ -> if (source === recorder) onError() }
                created.prepare()
                check(!cancelled) { "Recording abandoned" }
                created.start()
                check(!cancelled) { "Recording abandoned" }
            } catch (failure: Exception) {
                releaseRecorder()
                runCatching { files.discard(id) }
                draftId = null
                throw failure
            }
        }
    }

    override suspend fun finish(): SoundDraft = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            val id = checkNotNull(draftId)
            val current = checkNotNull(recorder)
            // A max-duration callback may arrive before or after native stopping. Always
            // release before validating the finished file; never preview just on the callback.
            try { runCatching { current.stop() } } finally { releaseRecorder() }
            val draft = files.draft(id)
            // Phone microphones record far below bundled sounds; calibrate before preview
            // so preview and gameplay play the same file. A failure keeps the original.
            runCatching { SoundNormalizer.normalize(draft) { files.duration(it) } }
            SoundDraft("sound-draft:$id", files.duration(draft))
        }
    }

    override suspend fun discard() = mutex.withLock {
        withContext(Dispatchers.IO + NonCancellable) {
            recorder?.let { runCatching { it.stop() } }
            releaseRecorder()
            draftId?.let { files.discard(it) }
            draftId = null
        }
    }
    private fun releaseRecorder() {
        val current = recorder
        recorder = null
        current?.setOnInfoListener(null)
        current?.setOnErrorListener(null)
        current?.release()
    }
}
