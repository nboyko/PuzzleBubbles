package eu.bfss.puzzlebubbles

import androidx.test.platform.app.InstrumentationRegistry
import eu.bfss.puzzlebubbles.data.LocalAudioAssetResolver
import eu.bfss.puzzlebubbles.data.LocalAudioPlayer
import eu.bfss.puzzlebubbles.data.sounds.SoundFileStore
import eu.bfss.puzzlebubbles.data.sounds.SoundLevels
import eu.bfss.puzzlebubbles.data.sounds.SoundNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SoundLevelsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun level(file: File): Float = SoundNormalizer.decode { it.setDataSource(file.path) }
        .let { SoundLevels.activeRms(it.samples, it.sampleRate, it.channels) }

    @Test
    fun quietRecordingIsRaisedToBundledContentLevel() {
        val file = File(context.cacheDir, "level-test/quiet.m4a")
        aacFixture(file, durationMs = 1_000) // Tone well below the content level.
        val before = level(file)
        val store = SoundFileStore(File(context.cacheDir, "level-test"))
        SoundNormalizer.normalize(file) { store.duration(it) }
        val after = level(file)
        assertTrue("Fixture must start quiet: $before", before < SoundLevels.TARGET_RMS * .8f)
        assertEquals(SoundLevels.TARGET_RMS, after, SoundLevels.TARGET_RMS * .15f)
        assertTrue(store.duration(file) in 900L..1_200L)
        assertTrue(File(context.cacheDir, "level-test").listFiles()!!.none { it.name.endsWith(".level") })
        file.delete()
    }

    @Test
    fun everyBundledSoundIsBalancedToTheCommonTarget() {
        for (id in LocalAudioAssetResolver.audioAssetIds) {
            val pcm = context.resources.openRawResourceFd(LocalAudioAssetResolver.resolveAudio(id)).use { fd ->
                SoundNormalizer.decode { it.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            }
            val rms = SoundLevels.level(pcm)
            // Playback can only turn files down; a new asset quieter than the target
            // would play too quietly and must be re-exported louder.
            assertTrue("$id level $rms is below the common target", rms >= SoundLevels.TARGET_RMS * .95f)
            assertEquals(id, SoundLevels.TARGET_RMS, rms * SoundLevels.playbackVolume(rms), SoundLevels.TARGET_RMS * .06f)
        }
    }

    @Test
    fun playerAppliesBalancedVolumesToBundledSounds() {
        val player = LocalAudioPlayer(context)
        try {
            val deadline = System.currentTimeMillis() + 10_000
            // Measurement runs in the background after creation.
            while (player.bundledVolume("cow-audio") == 1f && System.currentTimeMillis() < deadline) Thread.sleep(50)
            assertTrue(player.bundledVolume("cow-audio") < 1f) // Cow measured above target.
            assertTrue(player.bundledVolume("sfx-pop") < player.bundledVolume("cow-audio"))
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { player.release() }
        }
    }
}
