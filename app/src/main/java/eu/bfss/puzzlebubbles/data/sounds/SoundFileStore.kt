package eu.bfss.puzzlebubbles.data.sounds

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import eu.bfss.puzzlebubbles.domain.RecordingDeadline
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Owns only library/audio. Draft leases survive rotation, never process death. */
open class SoundFileStore(val directory: File) {
    private val liveDrafts = ConcurrentHashMap.newKeySet<String>()
    val draftsDirectory get() = File(directory, "drafts")
    fun file(id: String): File {
        require(id.matches(Regex("[a-f0-9-]{36}"))) { "Invalid sound ID" }
        return File(directory, "$id.m4a")
    }
    fun staging(id: String) = File(directory, "${file(id).name}.tmp")
    fun draft(id: String) = File(draftsDirectory, file(id).name)
    fun newDraft(): Pair<String, File> {
        ensureDirectory()
        val id = UUID.randomUUID().toString()
        liveDrafts.add(id)
        return id to draft(id)
    }
    fun assetFile(assetId: String): File = when {
        assetId.startsWith("library-sound:") -> file(assetId.removePrefix("library-sound:"))
        assetId.startsWith("sound-draft:") -> draft(assetId.removePrefix("sound-draft:"))
        else -> throw IOException("Unknown sound")
    }
    fun ensureDirectory() {
        if (!draftsDirectory.isDirectory && !draftsDirectory.mkdirs()) throw IOException("Sound storage unavailable")
    }
    open fun stage(id: String, source: File) {
        ensureDirectory()
        source.inputStream().use { input -> FileOutputStream(staging(id)).use { output ->
            input.copyTo(output)
            output.fd.sync()
        } }
    }
    open fun promote(id: String) {
        if (!staging(id).renameTo(file(id))) throw IOException("Sound rename failed")
    }
    open fun remove(file: File) {
        val parent = file.parentFile?.canonicalFile ?: throw IllegalArgumentException("Unowned file")
        require(parent == directory.canonicalFile || parent == draftsDirectory.canonicalFile)
        if (file.exists() && !file.delete()) throw IOException("Sound cleanup failed")
    }
    fun discard(id: String) {
        liveDrafts.remove(id)
        remove(draft(id))
    }
    fun cleanDrafts() {
        val files = draftsDirectory.listFiles() ?: throw IOException("Cannot read sound drafts")
        files.filter { it.isFile && it.nameWithoutExtension !in liveDrafts }.forEach(::remove)
    }
    open fun duration(file: File): Long {
        if (!file.isFile || file.length() == 0L) throw IOException("Empty recording")
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == "audio/mp4a-latm"
            } ?: throw IOException("Not an AAC recording")
            extractor.selectTrack(track)
            if (extractor.sampleTime < 0) throw IOException("Recording has no samples")
        } finally { extractor.release() }
        val metadata = MediaMetadataRetriever()
        try {
            metadata.setDataSource(file.path)
            return metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?.takeIf { it in 1..RecordingDeadline.MAX_OUTPUT_DURATION_MS }
                ?: throw IOException("Invalid recording duration")
        } finally { metadata.release() }
    }
}
