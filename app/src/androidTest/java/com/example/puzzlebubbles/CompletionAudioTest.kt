package com.example.puzzlebubbles

import android.media.MediaPlayer
import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.puzzlebubbles.data.AudioPlayer
import com.example.puzzlebubbles.data.LocalAudioAssetResolver
import com.example.puzzlebubbles.data.LocalAudioPlayer
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.ChooseItPhase
import com.example.puzzlebubbles.domain.ChooseItSessionFactory
import com.example.puzzlebubbles.presentation.games.chooseit.ChooseItGameScreen
import com.example.puzzlebubbles.presentation.games.chooseit.ChooseItUiState
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CompletionAudioTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun finalScreenPlaysOnceAndWaitsBeyondThreeSecondsForSound() {
        val player = PendingAudioPlayer()
        val returnsHome = AtomicInteger()
        showCompletion(player, returnsHome)
        compose.waitUntil { player.requests.size == 1 }
        val start = SystemClock.elapsedRealtime()
        compose.waitUntil(timeoutMillis = 5_000) { SystemClock.elapsedRealtime() - start >= 3_150 }
        compose.runOnIdle {
            assertEquals(listOf("sfx-game-complete"), player.requests)
            assertEquals(0, returnsHome.get())
            player.finished.complete(Unit)
        }
        compose.waitUntil { returnsHome.get() == 1 }
    }

    @Test
    fun leavingCompletionCancelsSoundAndAutomaticReturn() {
        val player = PendingAudioPlayer()
        val returnsHome = AtomicInteger()
        val visible = mutableStateOf(true)
        showCompletion(player, returnsHome, visible)
        compose.waitUntil { player.requests.size == 1 }
        compose.runOnIdle { visible.value = false }
        compose.waitUntil { player.cancelled && player.stops > 0 }
        val start = SystemClock.elapsedRealtime()
        compose.waitUntil(timeoutMillis = 5_000) { SystemClock.elapsedRealtime() - start >= 3_150 }
        assertEquals(0, returnsHome.get())
    }

    @Test
    fun bundledYayDecodesAndPlaysToItsEnd() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val probe = requireNotNull(MediaPlayer.create(context, LocalAudioAssetResolver.resolveAudio("sfx-game-complete")))
        val durationMs = probe.duration
        probe.release()
        assertTrue("Expected the complete supplied yay clip", durationMs in 3_900..4_300)
        val player = LocalAudioPlayer(context)
        try {
            val start = SystemClock.elapsedRealtime()
            withTimeout(8_000) {
                withContext(Dispatchers.Main) { player.playToEnd("sfx-game-complete") }
            }
            assertTrue("Yay playback ended early", SystemClock.elapsedRealtime() - start >= durationMs - 200)
        } finally {
            withContext(Dispatchers.Main) { player.release() }
        }
    }

    private fun showCompletion(
        player: AudioPlayer,
        returnsHome: AtomicInteger,
        visible: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(true),
    ) {
        val rounds = ChooseItSessionFactory().create(LocalGameRepository().getChooseItGame("whats-that")).rounds
        val state = ChooseItUiState(
            round = rounds.last(), roundNumber = 10, roundCount = 10,
            phase = ChooseItPhase.GAME_COMPLETE, feedbackChoiceId = null, audioRequestId = 10,
        )
        compose.setContent {
            PuzzleBubblesTheme {
                if (visible.value) {
                    ChooseItGameScreen(
                        title = "What's That?", uiState = state, audioPlayer = player,
                        onAnswer = {}, onReplay = {}, onFeedbackFinished = {}, onBack = {},
                        onGameComplete = { returnsHome.incrementAndGet() },
                    )
                } else Text("Home")
            }
        }
    }

    private class PendingAudioPlayer : AudioPlayer {
        val finished = CompletableDeferred<Unit>()
        val requests = mutableListOf<String>()
        var stops = 0
        var cancelled = false
        override fun play(assetId: String) = error("Content audio must not start on completion")
        override suspend fun playToEnd(assetId: String) {
            requests += assetId
            try {
                finished.await()
            } finally {
                cancelled = !finished.isCompleted
            }
        }
        override fun stop() { stops++ }
        override fun release() = stop()
    }
}
