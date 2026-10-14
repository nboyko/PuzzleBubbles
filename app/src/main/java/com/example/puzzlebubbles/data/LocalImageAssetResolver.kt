package com.example.puzzlebubbles.data

import androidx.annotation.DrawableRes
import com.example.puzzlebubbles.R

object LocalImageAssetResolver {
    @DrawableRes
    fun resolveImage(assetId: String): Int = when (assetId) {
        "ui-back" -> R.drawable.ic_back_arrow
        "ui-sound" -> R.drawable.ic_sound
        "ui-settings" -> R.drawable.ic_settings
        "ui-play" -> R.drawable.ic_play
        "ui-games" -> R.drawable.ic_games
        "ui-trophy" -> R.drawable.ic_trophy
        "completion-background" -> R.drawable.bubbles_bg
        "choose-it-cover" -> R.drawable.game_cover_whats_that
        "matching-cover" -> R.drawable.game_cover_match_it_up
        "cat-image" -> R.drawable.cat
        "cow-image" -> R.drawable.cow
        "dog-image" -> R.drawable.dog
        "goat-image" -> R.drawable.goat
        "monkey-image" -> R.drawable.monkey
        "mouse-image" -> R.drawable.mouse
        "parrot-image" -> R.drawable.parrot
        "pig-image" -> R.drawable.pig
        "rabbit-image" -> R.drawable.rabbit
        "sheep-image" -> R.drawable.sheep
        else -> error("Unknown image asset ID: $assetId")
    }
}
