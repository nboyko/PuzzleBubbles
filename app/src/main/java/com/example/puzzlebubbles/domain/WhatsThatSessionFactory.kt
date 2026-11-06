package com.example.puzzlebubbles.domain

import kotlin.random.Random

class WhatsThatSessionFactory(private val random: Random = Random.Default) {
    fun create(game: WhatsThatGame): WhatsThatSession {
        require(game.questions.size in 1..SESSION_SIZE)
        require(game.imagePool.size >= 2 && game.imagePool.map { it.id }.distinct().size == game.imagePool.size)
        require(game.questions.map { it.id }.distinct().size == game.questions.size)
        require(game.questions.all { q -> game.imagePool.any { it.id == q.imageId } })

        val rounds = game.questions.shuffled(random).mapIndexed { index, question ->
            val target = game.imagePool.single { it.id == question.imageId }
            val incorrect = game.imagePool.filterNot { it.id == target.id }.random(random)
            val choices = listOf(target, incorrect)
                .shuffled(random)
                .map { WhatsThatChoice(id = it.id, imageAssetId = it.imageAssetId) }
            WhatsThatRound(
                id = "round-${index + 1}-${target.id}",
                targetItemId = target.id,
                audioAssetId = question.audioAssetId,
                choices = choices,
                correctChoiceId = target.id,
            )
        }
        return WhatsThatSession(rounds)
    }

    companion object {
        const val SESSION_SIZE = 10
    }
}
