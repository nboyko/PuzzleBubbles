package com.example.puzzlebubbles.presentation.components

import com.example.puzzlebubbles.data.LocalImageAssetResolver
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.puzzlebubbles.data.photos.PhotoImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalPhotoImages = staticCompositionLocalOf<PhotoImageStore> { error("Photo image renderer not configured") }

/** File/bitmap lookup stays at the Android rendering boundary, never in screen models. */
@Composable
fun PhotoAssetImage(assetId: String, modifier: Modifier, thumbnail: Boolean, description: String? = null, revision: Long = 0) {
    val bitmap by photoBitmap(assetId, thumbnail, revision)
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), description, modifier, contentScale = ContentScale.Fit)
    else Box(modifier.background(Color(0xFFE7EFF8)))
}

@Preview(name = "Photo thumbnails", showBackground = true)
@Composable
private fun PhotoAssetImagePreview() {
    PuzzleBubblesTheme {
        Row {
            PhotoAssetImage("library-photo:cat-image", Modifier.size(128.dp), thumbnail = true)
            PhotoAssetImage("library-photo:dog-image", Modifier.size(128.dp), thumbnail = true)
        }
    }
}

@Composable
internal fun photoBitmap(assetId: String, thumbnail: Boolean, revision: Long = 0): androidx.compose.runtime.State<Bitmap?> {
    if (LocalInspectionMode.current) {
        val resources = LocalContext.current.resources
        // Bundled examples only in Layoutlib; never open private files from a preview.
        return remember(assetId) {
            val sample = if (assetId.endsWith("dog-image")) "dog-image" else "cat-image"
            mutableStateOf(BitmapFactory.decodeResource(
                resources, LocalImageAssetResolver.resolveImage(sample)))
        }
    }
    val images = LocalPhotoImages.current
    return produceState<Bitmap?>(null, images, assetId, thumbnail, revision) {
        value = withContext(Dispatchers.IO) { images.image(assetId, thumbnail) }
    }
}
