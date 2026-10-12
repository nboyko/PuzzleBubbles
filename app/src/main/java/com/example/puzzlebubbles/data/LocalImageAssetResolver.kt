package com.example.puzzlebubbles.data

import androidx.annotation.DrawableRes
import com.example.puzzlebubbles.R

object LocalImageAssetResolver {
    @DrawableRes
    fun resolveImage(assetId: String): Int = when (assetId) {
        "choose-it-cover" -> R.drawable.game_cover_whats_that
        "matching-cover" -> R.drawable.game_cover_match_it_up
        else -> error("Unknown image asset ID: $assetId")
    }
}
