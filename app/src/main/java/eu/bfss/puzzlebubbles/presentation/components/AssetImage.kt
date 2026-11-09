package eu.bfss.puzzlebubbles.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import eu.bfss.puzzlebubbles.data.LocalImageAssetResolver

/** Android rendering boundary: screens only supply stable asset IDs. */
@Composable
fun AssetImage(
    assetId: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    colorFilter: ColorFilter? = null,
) {
    if (assetId.startsWith("library-photo:")) {
        PhotoAssetImage(assetId, modifier, thumbnail = false, description = contentDescription, contentScale = contentScale)
        return
    }
    Image(
        painter = painterResource(LocalImageAssetResolver.resolveImage(assetId)),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        colorFilter = colorFilter,
    )
}

@Preview(name = "Artwork and icons", showBackground = true, backgroundColor = 0xFFEAF8FF)
@Composable
private fun AssetImagePreview() {
    PuzzleBubblesTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssetImage("app-logo", Modifier.size(72.dp))
            AssetImage("cat-image", Modifier.size(72.dp))
            AssetImage("ui-library", Modifier.size(56.dp), colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color(0xFF166EC5)))
        }
    }
}
