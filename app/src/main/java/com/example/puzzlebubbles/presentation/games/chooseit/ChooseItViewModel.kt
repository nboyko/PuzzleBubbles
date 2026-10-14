package com.example.puzzlebubbles.presentation.games.chooseit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.puzzlebubbles.domain.ChooseItGame
import com.example.puzzlebubbles.domain.ChooseItPhase
import com.example.puzzlebubbles.domain.ChooseItRound
import com.example.puzzlebubbles.domain.ChooseItSessionEngine
import com.example.puzzlebubbles.domain.ChooseItSessionFactory

data class ChooseItUiState(
    val round: ChooseItRound,
    val roundNumber: Int,
    val roundCount: Int,
    val phase: ChooseItPhase,
    val feedbackChoiceId: String?,
    val audioRequestId: Int,
)

class ChooseItViewModel(
    game: ChooseItGame,
    sessionFactory: ChooseItSessionFactory = ChooseItSessionFactory(),
) : ViewModel() {
    private val engine = ChooseItSessionEngine(sessionFactory.create(game))
    private var audioRequestId = 1

    var uiState by mutableStateOf(snapshot())
        private set

    fun answer(choiceId: String) {
        engine.answer(choiceId)
        uiState = snapshot()
    }

    fun finishFeedback() {
        val previousRound = engine.state.roundIndex
        engine.finishFeedback()
        if (engine.state.roundIndex != previousRound) audioRequestId += 1
        uiState = snapshot()
    }

    fun replayAudio() {
        if (engine.state.phase != ChooseItPhase.GAME_COMPLETE) {
            audioRequestId += 1
            uiState = snapshot()
        }
    }

    private fun snapshot() = ChooseItUiState(
        round = engine.currentRound,
        roundNumber = engine.state.roundIndex + 1,
        roundCount = engine.roundCount,
        phase = engine.state.phase,
        feedbackChoiceId = engine.state.feedbackChoiceId,
        audioRequestId = audioRequestId,
    )

    class Factory(private val game: ChooseItGame) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChooseItViewModel::class.java))
            return modelClass.cast(ChooseItViewModel(game))!!
        }
    }
}
