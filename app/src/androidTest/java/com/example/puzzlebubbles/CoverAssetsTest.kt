package com.example.puzzlebubbles

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.data.LocalImageAssetResolver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverAssetsTest {
    @Test
    fun everyCatalogCoverDecodesAsAnOptimizedImage() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        for (game in LocalGameRepository().getGameCatalog()) {
            val bitmap = BitmapFactory.decodeResource(
                resources,
                LocalImageAssetResolver.resolveImage(game.coverImageAssetId),
            )
            assertNotNull(game.coverImageAssetId, bitmap)
            requireNotNull(bitmap)
            assertTrue(bitmap.width in 1..512 && bitmap.height in 1..512)
            bitmap.recycle()
        }
    }
}
