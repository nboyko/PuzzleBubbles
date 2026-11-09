package eu.bfss.puzzlebubbles.data.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Owns only library/images; no caller supplies filesystem paths. */
open class PhotoImageStore(val directory: File) {
    private val drafts = ConcurrentHashMap<String, Bitmap>()
    fun putDraft(id: String, bitmap: Bitmap): String = "photo-draft:$id".also { drafts[it] = bitmap }
    fun discard(assetId: String) { drafts.remove(assetId) }
    fun draft(assetId: String): Bitmap = drafts[assetId] ?: throw IOException("Preview expired")
    fun file(id: String): File {
        require(id.matches(Regex("[a-f0-9-]{36}"))) { "Invalid photo ID" }
        return File(directory, "$id.webp")
    }
    fun staging(id: String) = File(directory, "${file(id).name}.tmp")
    fun ensureDirectory() {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Image storage unavailable")
    }
    open fun write(id: String, bitmap: Bitmap) {
        ensureDirectory()
        FileOutputStream(staging(id)).use { output ->
            val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else {
                @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            }
            if (!bitmap.compress(format, 85, output)) throw IOException("Image encoding failed")
            output.fd.sync()
        }
    }
    open fun promote(id: String) {
        if (!staging(id).renameTo(file(id))) throw IOException("Image rename failed")
    }
    /** Same-directory atomic rename: the previous complete file remains until the swap. */
    open fun replace(id: String) {
        if (!validFile(staging(id))) throw IOException("Invalid replacement image")
        Os.rename(staging(id).path, file(id).path)
    }
    open fun remove(file: File) {
        require(file.parentFile?.canonicalFile == directory.canonicalFile)
        if (file.exists() && !file.delete()) throw IOException("Image cleanup failed")
    }
    fun valid(id: String): Boolean = validFile(file(id))
    private fun validFile(input: File): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(input.path, bounds)
        if (bounds.outWidth != 1024 || bounds.outHeight != 1024 || bounds.outMimeType != "image/webp") return false
        val probe = BitmapFactory.decodeFile(input.path, BitmapFactory.Options().apply { inSampleSize = 8 })
            ?: return false
        probe.recycle()
        return true
    }
    fun image(assetId: String, thumbnail: Boolean): Bitmap? {
        if (assetId.startsWith("photo-draft:")) return drafts[assetId]
        require(assetId.startsWith("library-photo:"))
        return BitmapFactory.decodeFile(file(assetId.removePrefix("library-photo:")).path,
            BitmapFactory.Options().apply { inSampleSize = if (thumbnail) 4 else 1 })
    }
}
