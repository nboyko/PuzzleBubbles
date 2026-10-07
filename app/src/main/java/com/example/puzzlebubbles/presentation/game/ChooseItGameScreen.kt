package com.example.puzzlebubbles.presentation.game

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.domain.GameDefinition

@Composable
fun ChooseItGameScreen(game: GameDefinition, onBack: () -> Unit, modifier: Modifier = Modifier) {
    GamePlaceholderScreen(
        game = game,
        description = stringResource(R.string.choose_it_placeholder),
        onBack = onBack,
        modifier = modifier,
    )
}
