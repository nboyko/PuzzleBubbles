package com.example.puzzlebubbles.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import com.example.puzzlebubbles.domain.PhotoCrop

/** Bitmap access stays at the rendering boundary; drawing and export use the same source region. */
@Composable
fun PhotoCropViewport(
    assetId: String, crop: PhotoCrop, enabled: Boolean, description: String,
    onTransform: (Float, Float, Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap by photoBitmap(assetId, thumbnail = false)
    val transform by rememberUpdatedState(onTransform)
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().testTag("photo-crop-viewport")
            .semantics { contentDescription = description }
            .pointerInput(enabled, assetId) {
                if (enabled) detectTransformGestures { centroid, pan, zoom, _ ->
                    val side = minOf(size.width, size.height) * .84f
                    transform(zoom, pan.x, pan.y, centroid.x - size.width / 2f,
                        centroid.y - size.height / 2f, side)
                }
            }) {
            val side = minOf(size.width, size.height) * .84f
            val left = (size.width - side) / 2
            val top = (size.height - side) / 2
            drawRect(Color(0xFFEAF8FF))
            bitmap?.let { source ->
                val scale = side / crop.side
                withTransform({
                    translate(left - crop.left * scale, top - crop.top * scale)
                    scale(scale, scale, pivot = Offset.Zero)
                }) { drawImage(source.asImageBitmap(), filterQuality = FilterQuality.Medium) }
            }
            val scrim = Color(0xFF14223A).copy(alpha = .6f)
            drawRect(scrim, Offset.Zero, Size(size.width, top))
            drawRect(scrim, Offset(0f, top + side), Size(size.width, top))
            drawRect(scrim, Offset(0f, top), Size(left, side))
            drawRect(scrim, Offset(left + side, top), Size(left, side))
            drawRect(Color.White.copy(alpha = .9f), Offset(left, top), Size(side, side), style = Stroke(2.dp.toPx()))
        }
        if (bitmap == null) CircularProgressIndicator()
    }
}

@Preview(name = "Crop viewport - zoomed", showBackground = true)
@Composable
private fun PhotoCropViewportPreview() {
    PuzzleBubblesTheme {
        PhotoCropViewport("photo-draft:cat-image", PhotoCrop(128f, 128f, 256f), enabled = true,
            description = "Photo crop", onTransform = { _, _, _, _, _, _ -> },
            modifier = Modifier.size(320.dp, 440.dp))
    }
}
