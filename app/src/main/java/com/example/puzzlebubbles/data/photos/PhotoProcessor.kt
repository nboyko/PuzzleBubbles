package com.example.puzzlebubbles.data.photos

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.io.InputStream

/** Bounded Android decoding; the same normalized bitmap is previewed and encoded. */
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
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = open().use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Cannot decode image")
        var oriented: Bitmap? = null
        try {
            val matrix = orientationMatrix(orientation)
            oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val output = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
            Canvas(output).drawBitmap(oriented, centerCrop(oriented.width, oriented.height),
                Rect(0, 0, 1024, 1024), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            return output
        } finally {
            if (oriented !== decoded) oriented?.recycle()
            decoded.recycle()
        }
    }

    companion object {
        fun sampleSize(width: Int, height: Int): Int {
            var sample = 1
            while (minOf(width, height) / (sample * 2) >= 1024 ||
                (width.toLong() / sample) * (height.toLong() / sample) > 8_000_000L) sample *= 2
            return sample
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
