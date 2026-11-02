package com.example.puzzlebubbles.domain

data class ChooseItGame(val id: String, val title: String, val items: List<ChooseItItem>)

data class ChooseItGameSummary(val id: String, val name: String, val isBuiltIn: Boolean = false)

data class ChooseItItem(
    val id: String,
    val imageAssetId: String,
    val audioAssetId: String,
)

data class ChooseItChoice(val id: String, val imageAssetId: String)

data class ChooseItRound(
    val id: String,
    val targetItemId: String,
    val audioAssetId: String,
    val choices: List<ChooseItChoice>,
    val correctChoiceId: String,
)

data class ChooseItSession(val rounds: List<ChooseItRound>)
