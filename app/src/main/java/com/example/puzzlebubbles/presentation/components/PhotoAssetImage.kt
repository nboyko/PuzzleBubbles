package com.example.puzzlebubbles.presentation.components

import android.graphics.Bitmap
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
fun PhotoAssetImage(assetId: String, modifier: Modifier, thumbnail: Boolean, description: String? = null) {
    val images = LocalPhotoImages.current
    val bitmap by produceState<Bitmap?>(null, assetId, thumbnail) {
        value = withContext(Dispatchers.IO) { images.image(assetId, thumbnail) }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), description, modifier, contentScale = ContentScale.Fit)
    else Box(modifier.background(Color(0xFFE7EFF8)))
}
