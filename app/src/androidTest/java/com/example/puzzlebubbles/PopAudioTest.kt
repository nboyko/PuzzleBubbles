package com.example.puzzlebubbles

import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzlebubbles.data.LocalAudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class PopAudioTest {
    @Test
    fun waitsForCompletePopBeforeReturning() = runBlocking {
        val player = LocalAudioPlayer(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            val start = System.nanoTime()
            withTimeout(5_000) {
                withContext(Dispatchers.Main) { player.playToEnd("sfx-pop") }
            }
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            assertTrue("Pop was cut short after $elapsedMs ms", elapsedMs >= 1_000)
        } finally {
            withContext(Dispatchers.Main) { player.release() }
        }
    }
}
