package com.example.puzzlebubbles.data

import androidx.annotation.DrawableRes
import com.example.puzzlebubbles.R

object LocalImageAssetResolver {
    @DrawableRes
    fun resolveImage(assetId: String): Int = when (assetId) {
        "choose-it-cover" -> R.drawable.choose_it
        "matching-cover" -> R.drawable.matching
        else -> error("Unknown image asset ID: $assetId")
    }
}
