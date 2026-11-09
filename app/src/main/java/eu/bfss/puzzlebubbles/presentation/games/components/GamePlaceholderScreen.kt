package eu.bfss.puzzlebubbles.presentation.games.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.bfss.puzzlebubbles.R
import eu.bfss.puzzlebubbles.domain.GameDefinition

@Composable
internal fun GamePlaceholderScreen(
    game: GameDefinition,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = game.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.game_coming_soon), textAlign = TextAlign.Center)
        Text(text = description, textAlign = TextAlign.Center)
        Button(onClick = onBack, modifier = Modifier.heightIn(min = 56.dp)) {
            Text(text = stringResource(R.string.back), modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}
