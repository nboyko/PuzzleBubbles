package com.example.puzzlebubbles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.puzzlebubbles.data.photos.PhotoProcessor
import com.example.puzzlebubbles.domain.PhotoCrop
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PhotoProcessorTest {
    private fun normalized(open: () -> java.io.InputStream): Bitmap {
        val source = PhotoProcessor().prepare(open)
        return try { PhotoProcessor.render(source) } finally { source.recycle() }
    }
    @Test fun centerCropUsesCenterForLandscapePortraitSquareAndSmallSources() {
        for ((width, height) in listOf(300 to 100, 100 to 300, 100 to 100, 20 to 10)) {
            val crop = PhotoProcessor.centerCrop(width, height)
            assertEquals(minOf(width, height), crop.width())
            assertEquals(crop.width(), crop.height())
            assertTrue(kotlin.math.abs(crop.centerX() - width / 2) <= 1)
            assertTrue(kotlin.math.abs(crop.centerY() - height / 2) <= 1)
            val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            source.eraseColor(Color.GREEN)
            val bytes = ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            val result = normalized { ByteArrayInputStream(bytes) }
            assertEquals(1024, result.width); assertEquals(1024, result.height)
            assertEquals(Color.GREEN, result.getPixel(512, 512))
            source.recycle(); result.recycle()
        }
    }
    @Test fun allEightExifOrientationsArePreservedVisually() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Each quadrant identifies how rotation and mirroring move the original corners.
        val expectedCorners = listOf(
            listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
            listOf(Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE),
            listOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED),
            listOf(Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN),
            listOf(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW),
            listOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN),
            listOf(Color.YELLOW, Color.GREEN, Color.BLUE, Color.RED),
            listOf(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE),
        )
        for (orientation in 1..8) {
            val file = File(context.cacheDir, "orientation-${UUID.randomUUID()}.jpg")
            val source = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
            for (y in 0 until 80) for (x in 0 until 80) source.setPixel(x, y,
                if (y < 40) { if (x < 40) Color.RED else Color.GREEN }
                else { if (x < 40) Color.BLUE else Color.YELLOW })
            try {
                file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                ExifInterface(file).apply { setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString()); saveAttributes() }
                val result = normalized { file.inputStream() }
                for ((index, point) in listOf(100 to 100, 900 to 100, 100 to 900, 900 to 900).withIndex()) {
                val actual = result.getPixel(point.first, point.second)
                val expected = expectedCorners[orientation - 1][index]
                assertTrue("orientation $orientation", listOf(Color.red(actual) - Color.red(expected),
                    Color.green(actual) - Color.green(expected), Color.blue(actual) - Color.blue(expected)).all { kotlin.math.abs(it) < 15 })
                }
                result.recycle()
            } finally { source.recycle(); file.delete() }
        }
    }
    @Test fun corruptInputFailsWithoutProducingAnImage() {
        try { PhotoProcessor().prepare { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }; fail("Expected rejection") }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun highResolutionInputUsesBoundedSampledDecode() {
        for ((width, height) in listOf(4032 to 3024, 8000 to 6000, 100000 to 10)) {
            val sample = PhotoProcessor.sampleSize(width, height)
            assertTrue(sample >= 1)
            assertTrue((width.toLong() / sample) * (height.toLong() / sample) <= 8_000_000)
        }
    }
    @Test fun normalCameraPhotoDecodesAtSampledResolution() {
        val source = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        val bytes = ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        source.recycle()
        val output = normalized { ByteArrayInputStream(bytes) }
        assertEquals(1024, output.width)
        assertEquals(1024, output.height)
        assertTrue(Color.green(output.getPixel(512, 512)) > 240)
        output.recycle()
    }
    @Test fun uncroppedSourceAllowsSavingAreasOutsideTheOriginalCenterSquare() {
        val input = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(input)
        val paint = android.graphics.Paint()
        for ((index, color) in listOf(Color.RED, Color.GREEN, Color.BLUE).withIndex()) {
            paint.color = color
            canvas.drawRect(index * 100f, 0f, (index + 1) * 100f, 100f, paint)
        }
        val bytes = ByteArrayOutputStream().also { input.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        input.recycle()
        val source = PhotoProcessor().prepare { ByteArrayInputStream(bytes) }
        assertEquals(300, source.width); assertEquals(100, source.height)
        for ((crop, color) in listOf(PhotoCrop(0f, 0f, 100f) to Color.RED,
            PhotoCrop(100f, 0f, 100f) to Color.GREEN, PhotoCrop(200f, 0f, 100f) to Color.BLUE,
            PhotoCrop(275f, 75f, 25f) to Color.BLUE)) {
            val output = PhotoProcessor.render(source, crop)
            assertEquals(1024, output.width); assertEquals(1024, output.height)
            for ((x, y) in listOf(10 to 10, 512 to 512, 1010 to 1010)) assertEquals(color, output.getPixel(x, y))
            output.recycle()
        }
        source.recycle()
    }
}
