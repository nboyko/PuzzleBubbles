package com.example.puzzlebubbles.presentation.games.matching

import com.example.puzzlebubbles.presentation.games.components.GamePlaceholderScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.domain.GameDefinition

@Composable
fun MatchingGameScreen(game: GameDefinition, onBack: () -> Unit, modifier: Modifier = Modifier) {
    GamePlaceholderScreen(
        game = game,
        description = stringResource(R.string.matching_placeholder),
        onBack = onBack,
        modifier = modifier,
    )
}
