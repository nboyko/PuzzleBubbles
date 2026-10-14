package com.example.puzzlebubbles.domain

import kotlin.random.Random

class ChooseItSessionFactory(private val random: Random = Random.Default) {
    fun create(game: ChooseItGame): ChooseItSession {
        require(game.items.size == SESSION_SIZE) { "What's That? requires exactly $SESSION_SIZE items" }
        require(game.items.map { it.id }.distinct().size == game.items.size) { "Item IDs must be unique" }

        val rounds = game.items.shuffled(random).mapIndexed { index, target ->
            val incorrect = game.items.filterNot { it.id == target.id }.random(random)
            val choices = listOf(target, incorrect)
                .shuffled(random)
                .map { ChooseItChoice(id = it.id, imageAssetId = it.imageAssetId) }
            ChooseItRound(
                id = "round-${index + 1}-${target.id}",
                targetItemId = target.id,
                audioAssetId = target.audioAssetId,
                choices = choices,
                correctChoiceId = target.id,
            )
        }
        return ChooseItSession(rounds)
    }

    companion object {
        const val SESSION_SIZE = 10
    }
}
