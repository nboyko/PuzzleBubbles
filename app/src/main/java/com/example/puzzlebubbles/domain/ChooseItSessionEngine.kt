package com.example.puzzlebubbles.domain

enum class ChooseItPhase {
    WAITING_FOR_ANSWER,
    WRONG_FEEDBACK,
    CORRECT_FEEDBACK,
    GAME_COMPLETE,
}

data class ChooseItSessionState(
    val roundIndex: Int,
    val phase: ChooseItPhase,
    val feedbackChoiceId: String? = null,
)

class ChooseItSessionEngine(private val session: ChooseItSession) {
    init {
        require(session.rounds.isNotEmpty()) { "A session must contain at least one round" }
    }

    var state = ChooseItSessionState(roundIndex = 0, phase = ChooseItPhase.WAITING_FOR_ANSWER)
        private set

    val currentRound: ChooseItRound
        get() = session.rounds[state.roundIndex]

    val roundCount: Int
        get() = session.rounds.size

    fun answer(choiceId: String) {
        if (state.phase != ChooseItPhase.WAITING_FOR_ANSWER) return
        require(currentRound.choices.any { it.id == choiceId }) { "Choice does not belong to this round" }
        state = if (choiceId == currentRound.correctChoiceId) {
            state.copy(phase = ChooseItPhase.CORRECT_FEEDBACK, feedbackChoiceId = choiceId)
        } else {
            state.copy(phase = ChooseItPhase.WRONG_FEEDBACK, feedbackChoiceId = choiceId)
        }
    }

    fun finishFeedback() {
        state = when (state.phase) {
            ChooseItPhase.WRONG_FEEDBACK -> state.copy(
                phase = ChooseItPhase.WAITING_FOR_ANSWER,
                feedbackChoiceId = null,
            )
            ChooseItPhase.CORRECT_FEEDBACK -> {
                if (state.roundIndex == session.rounds.lastIndex) {
                    state.copy(phase = ChooseItPhase.GAME_COMPLETE, feedbackChoiceId = null)
                } else {
                    ChooseItSessionState(
                        roundIndex = state.roundIndex + 1,
                        phase = ChooseItPhase.WAITING_FOR_ANSWER,
                    )
                }
            }
            ChooseItPhase.WAITING_FOR_ANSWER,
            ChooseItPhase.GAME_COMPLETE,
            -> state
        }
    }
}
