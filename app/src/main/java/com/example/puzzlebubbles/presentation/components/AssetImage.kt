package com.example.puzzlebubbles.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.example.puzzlebubbles.data.LocalImageAssetResolver

/** Android rendering boundary: screens only supply stable asset IDs. */
@Composable
fun AssetImage(
    assetId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    colorFilter: ColorFilter? = null,
) {
    Image(
        painter = painterResource(LocalImageAssetResolver.resolveImage(assetId)),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        colorFilter = colorFilter,
    )
}
