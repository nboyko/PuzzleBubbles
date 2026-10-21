package com.example.puzzlebubbles.presentation.library

import com.example.puzzlebubbles.domain.PhotoCrop
import kotlin.math.abs

/** Source coordinates survive viewport resizing; no bitmaps or screen pixels are retained. */
data class CropTransform(
    val width: Int,
    val height: Int,
    val zoom: Float = 1f,
    val centerX: Float = width / 2f,
    val centerY: Float = height / 2f,
) {
    init { require(width > 0 && height > 0) }
    val side: Float get() = minOf(width, height) / zoom
    val region: PhotoCrop get() = PhotoCrop(centerX - side / 2, centerY - side / 2, side)
    val unchanged: Boolean get() = abs(zoom - 1f) < .00001f &&
        abs(centerX - width / 2f) < .001f && abs(centerY - height / 2f) < .001f

    /** Focal point and pan are relative to the viewport center, in current screen pixels. */
    fun gesture(zoomChange: Float, panX: Float, panY: Float,
                focalX: Float, focalY: Float, viewportSide: Float): CropTransform {
        if (viewportSide <= 0 || zoomChange <= 0 ||
            !listOf(zoomChange, panX, panY, focalX, focalY, viewportSide).all { it.isFinite() }) return this
        val nextZoom = (zoom * zoomChange).coerceIn(1f, 4f)
        val nextSide = minOf(width, height) / nextZoom
        val x = centerX + focalX * (side - nextSide) / viewportSide - panX * nextSide / viewportSide
        val y = centerY + focalY * (side - nextSide) / viewportSide - panY * nextSide / viewportSide
        return copy(zoom = nextZoom,
            centerX = x.coerceIn(nextSide / 2, width - nextSide / 2),
            centerY = y.coerceIn(nextSide / 2, height - nextSide / 2))
    }
}
