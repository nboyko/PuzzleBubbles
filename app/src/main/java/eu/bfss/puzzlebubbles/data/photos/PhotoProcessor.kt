package eu.bfss.puzzlebubbles.data.photos

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import eu.bfss.puzzlebubbles.domain.PhotoCrop
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.io.InputStream

/** Bounded, uncropped editing source; normalization happens only on explicit Save. */
class PhotoProcessor {
    fun prepare(resolver: ContentResolver, uri: Uri): Bitmap = prepare {
        resolver.openInputStream(uri) ?: throw IOException("Image unavailable")
    }

    fun prepare(open: () -> InputStream): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid image" }
        require(bounds.outWidth <= 100_000 && bounds.outHeight <= 100_000) { "Unreasonable dimensions" }
        val orientation = open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight,
                minOf(16_000_000L, Runtime.getRuntime().maxMemory() / 16).coerceAtLeast(2_000_000L))
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = open().use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Cannot decode image")
        try {
            val matrix = orientationMatrix(orientation)
            val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (oriented !== decoded) decoded.recycle()
            return oriented
        } catch (failure: Throwable) {
            decoded.recycle()
            throw failure
        }
    }

    companion object {
        fun sampleSize(width: Int, height: Int, maxPixels: Long = 8_000_000L): Int {
            var sample = 1
            while (minOf(width, height) / (sample * 2) >= 4096 ||
                maxOf(width, height) / sample > 8192 ||
                (width.toLong() / sample) * (height.toLong() / sample) > maxPixels) sample *= 2
            return sample
        }
        fun render(source: Bitmap, crop: PhotoCrop? = null): Bitmap {
            val side = minOf(source.width, source.height).toFloat()
            val region = crop ?: PhotoCrop((source.width - side) / 2, (source.height - side) / 2, side)
            require(listOf(region.left, region.top, region.side).all { it.isFinite() } &&
                region.side > 0 && region.left >= -.001f && region.top >= -.001f &&
                region.left + region.side <= source.width + .001f &&
                region.top + region.side <= source.height + .001f) { "Invalid crop" }
            val output = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
            try {
                val scale = 1024f / region.side
                val matrix = Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(-region.left * scale, -region.top * scale)
                }
                Canvas(output).drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                return output
            } catch (failure: Throwable) { output.recycle(); throw failure }
        }
        fun centerCrop(width: Int, height: Int): Rect {
            val side = minOf(width, height)
            val left = (width - side) / 2
            val top = (height - side) / 2
            return Rect(left, top, left + side, top + side)
        }
        fun orientationMatrix(orientation: Int) = Matrix().apply {
            when (orientation) {
                2 -> setScale(-1f, 1f)
                3 -> setRotate(180f)
                4 -> setScale(1f, -1f)
                5 -> { setRotate(90f); postScale(-1f, 1f) }
                6 -> setRotate(90f)
                7 -> { setRotate(270f); postScale(-1f, 1f) }
                8 -> setRotate(270f)
            }
        }
    }
}
