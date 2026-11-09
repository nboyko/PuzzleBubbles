package eu.bfss.puzzlebubbles

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import eu.bfss.puzzlebubbles.data.LocalGameRepository
import eu.bfss.puzzlebubbles.data.LocalImageAssetResolver
import eu.bfss.puzzlebubbles.data.LocalAudioAssetResolver
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

    @Test
    fun everyWhatsThatItemHasRuntimeImageAndAudio() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val game = LocalGameRepository().getWhatsThatGame("whats-that")
        for (item in game.imagePool) {
            val bitmap = BitmapFactory.decodeResource(
                resources,
                LocalImageAssetResolver.resolveImage(item.imageAssetId),
            )
            assertNotNull(item.imageAssetId, bitmap)
            requireNotNull(bitmap)
            assertTrue(bitmap.width in 1..512 && bitmap.height in 1..512)
            bitmap.recycle()

        }
        for (item in game.questions) {
            resources.openRawResource(LocalAudioAssetResolver.resolveAudio(item.audioAssetId)).use { audio ->
                assertTrue(item.audioAssetId, audio.available() > 0)
            }
        }
        resources.openRawResource(LocalAudioAssetResolver.resolveAudio("sfx-pop")).use { audio ->
            assertTrue(audio.available() > 0)
        }
    }
}
