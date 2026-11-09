package eu.bfss.puzzlebubbles

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import eu.bfss.puzzlebubbles.data.sounds.AndroidSoundRecorder
import eu.bfss.puzzlebubbles.data.sounds.SoundFileStore
import eu.bfss.puzzlebubbles.domain.RecordingDeadline
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

/** Exercises the native microphone/codec, rather than the screen tests' fake recorder. */
class AndroidSoundRecorderTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var activity: ActivityScenario<MainActivity>
    private lateinit var files: SoundFileStore
    private lateinit var recorder: AndroidSoundRecorder

    @Before fun setup() {
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
        activity = ActivityScenario.launch(MainActivity::class.java)
        files = SoundFileStore(File(context.cacheDir, "native-recording-${UUID.randomUUID()}"))
        recorder = AndroidSoundRecorder(context, files)
    }

    @After fun cleanup() = runBlocking {
        recorder.discard()
        activity.close()
        files.directory.deleteRecursively() // Unique test-owned cache directory only.
        Unit
    }

    @Test fun manualStopProducesPlayableAacAndDiscardRemovesTheDraft() = runBlocking {
        recorder.start({ fail("Unexpected recording limit") }, { fail("Native recording failed") })
        delay(1200)
        val draft = recorder.finish()
        assertTrue(draft.durationMs in 1..5000)
        assertTrue(files.assetFile(draft.assetId).length() > 0)
        assertEquals(draft.durationMs, files.duration(files.assetFile(draft.assetId)))
        recorder.discard()
        assertTrue(files.draftsDirectory.listFiles()!!.isEmpty())
    }

    @Test fun nativeFiveSecondLimitAllowsOnlyBoundedOutputOverhead() = runBlocking {
        val limit = CompletableDeferred<Unit>()
        recorder.start({ limit.complete(Unit) }, { limit.completeExceptionally(IllegalStateException("Native recording failed")) })
        withTimeout(8000) { limit.await() }
        val draft = recorder.finish()
        assertTrue("Recorded duration was ${draft.durationMs} ms",
            draft.durationMs in 4000..RecordingDeadline.MAX_OUTPUT_DURATION_MS)
    }

    @Test fun immediateStopEitherValidatesAudioOrCanBeDiscardedAndRetried() = runBlocking {
        recorder.start({}, {})
        val result = runCatching { recorder.finish() }
        result.getOrNull()?.let { assertTrue(it.durationMs in 1..5000) }
        recorder.discard()
        assertTrue(files.draftsDirectory.listFiles()!!.isEmpty())
        recorder.start({}, {})
        delay(600)
        assertTrue(recorder.finish().durationMs > 0)
    }
}
