package eu.bfss.puzzlebubbles.presentation.games.matching

import androidx.compose.ui.tooling.preview.Preview
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import eu.bfss.puzzlebubbles.data.LocalGameRepository
import eu.bfss.puzzlebubbles.domain.GameType
import eu.bfss.puzzlebubbles.presentation.games.components.GamePlaceholderScreen

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import eu.bfss.puzzlebubbles.R
import eu.bfss.puzzlebubbles.domain.GameDefinition

@Composable
fun MatchingGameScreen(game: GameDefinition, onBack: () -> Unit, modifier: Modifier = Modifier) {
    GamePlaceholderScreen(
        game = game,
        description = stringResource(R.string.matching_placeholder),
        onBack = onBack,
        modifier = modifier,
    )
}

@Preview(name = "Match Up placeholder", widthDp = 360, heightDp = 800)
@Composable
private fun MatchingGamePreview() {
    PuzzleBubblesTheme {
        MatchingGameScreen(LocalGameRepository().getGameCatalog()
            .first { it.type == GameType.MATCHING }, {},
            Modifier.fillMaxSize())
    }
}
