package eu.bfss.puzzlebubbles.domain

enum class WhatsThatPhase {
    WAITING_FOR_ANSWER,
    WRONG_FEEDBACK,
    CORRECT_FEEDBACK,
    GAME_COMPLETE,
}

data class WhatsThatSessionState(
    val roundIndex: Int,
    val phase: WhatsThatPhase,
    val feedbackChoiceId: String? = null,
)

class WhatsThatSessionEngine(private val session: WhatsThatSession) {
    init {
        require(session.rounds.isNotEmpty()) { "A session must contain at least one round" }
    }

    var state = WhatsThatSessionState(roundIndex = 0, phase = WhatsThatPhase.WAITING_FOR_ANSWER)
        private set

    val currentRound: WhatsThatRound
        get() = session.rounds[state.roundIndex]

    val roundCount: Int
        get() = session.rounds.size

    fun answer(choiceId: String) {
        if (state.phase != WhatsThatPhase.WAITING_FOR_ANSWER) return
        require(currentRound.choices.any { it.id == choiceId }) { "Choice does not belong to this round" }
        state = if (choiceId == currentRound.correctChoiceId) {
            state.copy(phase = WhatsThatPhase.CORRECT_FEEDBACK, feedbackChoiceId = choiceId)
        } else {
            state.copy(phase = WhatsThatPhase.WRONG_FEEDBACK, feedbackChoiceId = choiceId)
        }
    }

    fun finishFeedback() {
        state = when (state.phase) {
            WhatsThatPhase.WRONG_FEEDBACK -> state.copy(
                phase = WhatsThatPhase.WAITING_FOR_ANSWER,
                feedbackChoiceId = null,
            )
            WhatsThatPhase.CORRECT_FEEDBACK -> {
                if (state.roundIndex == session.rounds.lastIndex) {
                    state.copy(phase = WhatsThatPhase.GAME_COMPLETE, feedbackChoiceId = null)
                } else {
                    WhatsThatSessionState(
                        roundIndex = state.roundIndex + 1,
                        phase = WhatsThatPhase.WAITING_FOR_ANSWER,
                    )
                }
            }
            WhatsThatPhase.WAITING_FOR_ANSWER,
            WhatsThatPhase.GAME_COMPLETE,
            -> state
        }
    }
}
