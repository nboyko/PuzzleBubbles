package com.example.puzzlebubbles

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzlebubbles.data.LocalAudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LibrarySoundPlaybackTest {
    private lateinit var activity: ActivityScenario<MainActivity>
    private lateinit var root: File
    private lateinit var player: LocalAudioPlayer
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        root = File(context.cacheDir, "sound-playback-${UUID.randomUUID()}").apply { mkdirs() }
        aacFixture(File(root, "tone.m4a"), 2000)
        activity = ActivityScenario.launch(MainActivity::class.java)
        instrumentation.runOnMainSync {
            player = LocalAudioPlayer(context) { id ->
                if (id == "missing") File(root, "missing.m4a") else File(root, "tone.m4a")
            }
        }
    }
    @After fun cleanup() {
        instrumentation.runOnMainSync { player.release() }
        activity.close()
        root.deleteRecursively() // Unique test-owned cache files only.
    }
    private fun toggle(id: String) = instrumentation.runOnMainSync { player.toggle(id) }
    private suspend fun until(condition: () -> Boolean) {
        repeat(100) { if (condition()) return; delay(50) }
        fail("Timed out: ${player.playback.value}")
    }
    @Test fun pauseResumeSwitchCompletionAndReleaseUseOnePlayer() = runBlocking {
        toggle("first")
        until { player.playback.value.playing }
        // start() can precede the audio device's first rendered frame. Wait for
        // real progress, not an assumed hardware startup delay, before pausing.
        until { player.playback.value.positionMs >= 100 }
        toggle("first")
        val paused = player.playback.value
        assertFalse(paused.playing)
        assertTrue(paused.positionMs > 0)
        delay(150)
        assertEquals(paused.positionMs, player.playback.value.positionMs)
        toggle("first")
        until { player.playback.value.playing }
        toggle("second")
        until { player.playback.value.assetId == "second" && player.playback.value.playing }
        until { !player.playback.value.playing && !player.playback.value.preparing }
        assertEquals(0L, player.playback.value.positionMs)
        assertEquals("second", player.playback.value.assetId)
        toggle("first")
        until { player.playback.value.playing }
        instrumentation.runOnMainSync { player.stop() }
        assertNull(player.playback.value.assetId)
    }
    @Test fun invalidFileReportsFailureWithoutCrashingAndCanRetryAnotherSound() = runBlocking {
        toggle("missing")
        until { player.playback.value.failed }
        toggle("first")
        until { player.playback.value.playing }
        assertFalse(player.playback.value.failed)
    }
    @Test fun closingDuringPreparationPreventsLateAudioAndAllowsAnotherPlayback() = runBlocking {
        instrumentation.runOnMainSync { player.toggle("first"); player.stop() }
        delay(300)
        assertNull(player.playback.value.assetId)
        assertFalse(player.playback.value.playing)
        toggle("second")
        until { player.playback.value.playing }
        assertEquals("second", player.playback.value.assetId)
    }
}
