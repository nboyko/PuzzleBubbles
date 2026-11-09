package com.example.puzzlebubbles.data

import androidx.annotation.RawRes
import com.example.puzzlebubbles.R

object LocalAudioAssetResolver {
    val audioAssetIds = listOf(
        "cat-audio", "cow-audio", "dog-audio", "goat-audio", "monkey-audio", "mouse-audio",
        "parrot-audio", "pig-audio", "rabbit-audio", "sheep-audio", "sfx-pop", "sfx-game-complete",
    )

    @RawRes
    fun resolveAudio(assetId: String): Int = when (assetId) {
        "cat-audio" -> R.raw.content_cat
        "cow-audio" -> R.raw.content_cow
        "dog-audio" -> R.raw.content_dog
        "goat-audio" -> R.raw.content_goat
        "monkey-audio" -> R.raw.content_monkey
        "mouse-audio" -> R.raw.content_mouse
        "parrot-audio" -> R.raw.content_parrot
        "pig-audio" -> R.raw.content_pig
        "rabbit-audio" -> R.raw.content_rabbit
        "sheep-audio" -> R.raw.content_sheep
        "sfx-pop" -> R.raw.sfx_pop
        "sfx-game-complete" -> R.raw.sfx_game_complete
        else -> error("Unknown audio asset ID: $assetId")
    }
}
