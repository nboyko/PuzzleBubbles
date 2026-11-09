package eu.bfss.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import eu.bfss.puzzlebubbles.domain.WhatsThatGame
import eu.bfss.puzzlebubbles.domain.WhatsThatPhase
import eu.bfss.puzzlebubbles.domain.WhatsThatRound
import eu.bfss.puzzlebubbles.domain.WhatsThatSessionEngine
import eu.bfss.puzzlebubbles.domain.WhatsThatSessionFactory

data class WhatsThatUiState(
    val round: WhatsThatRound,
    val roundNumber: Int,
    val roundCount: Int,
    val phase: WhatsThatPhase,
    val feedbackChoiceId: String?,
    val audioRequestId: Int,
)

class WhatsThatViewModel(
    game: WhatsThatGame,
    sessionFactory: WhatsThatSessionFactory = WhatsThatSessionFactory(),
) : ViewModel() {
    private val engine = WhatsThatSessionEngine(sessionFactory.create(game))
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
        if (engine.state.phase != WhatsThatPhase.GAME_COMPLETE) {
            audioRequestId += 1
            uiState = snapshot()
        }
    }

    private fun snapshot() = WhatsThatUiState(
        round = engine.currentRound,
        roundNumber = engine.state.roundIndex + 1,
        roundCount = engine.roundCount,
        phase = engine.state.phase,
        feedbackChoiceId = engine.state.feedbackChoiceId,
        audioRequestId = audioRequestId,
    )

    class Factory(private val game: WhatsThatGame) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(WhatsThatViewModel::class.java))
            return modelClass.cast(WhatsThatViewModel(game))!!
        }
    }
}
