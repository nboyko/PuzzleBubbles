package com.example.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.domain.WhatsThatGameSummary
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.presentation.home.HomeNavigationBar
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

private val SelectionInk = Color(0xFF18345B)
private val SelectionAccent = Color(0xFF166EC5)

@Composable
fun WhatsThatGameSelectionScreen(
    games: List<WhatsThatGameSummary>,
    onPlay: (String) -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit = {},
    onDelete: (String) -> Unit = {},
    enabled: Boolean = true,
    loadError: Boolean = false,
    onRetry: () -> Unit = {},
    deletingGameId: String? = null,
) {
    BlueGameTheme {
    val screenDescription = stringResource(R.string.whats_that_games)
    val deletionDescription = stringResource(R.string.game_pending_delete)
    Column(modifier.fillMaxSize().testTag("whats-that-selection").semantics { paneTitle = screenDescription }
        .background(Brush.verticalGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF0F7FF))))
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        if (loadError) {
            Text(stringResource(R.string.game_load_error), Modifier.padding(16.dp))
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().testTag("whats-that-game-list"),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(games, key = { it.id }) { game ->
                    val deleting = game.id == deletingGameId
                    val cardColor = when {
                        deleting -> Color(0xFFFFEBEE)
                        game.isBuiltIn -> Color(0xFFDDEEFF)
                        else -> Color.White
                    }
                    Card(onClick = { onPlay(game.id) }, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("game-row-${game.id}").semantics {
                            role = Role.Button
                            if (deleting) stateDescription = deletionDescription
                        },
                        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(
                            containerColor = cardColor, disabledContainerColor = cardColor,
                            contentColor = SelectionInk, disabledContentColor = SelectionInk)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            AssetImage("ui-games", Modifier.size(56.dp), colorFilter = ColorFilter.tint(SelectionAccent))
                            Text(game.name, Modifier.weight(1f).padding(8.dp),
                                color = if (game.isBuiltIn) SelectionAccent else SelectionInk,
                                fontFamily = HomeHeadingFontFamily, fontSize = 22.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                fontWeight = if (game.isBuiltIn) FontWeight.Bold else FontWeight.Normal)
                            if (!game.isBuiltIn) BubbleButton(onClick = { onDelete(game.id) }, enabled = enabled,
                                iconAssetId = "ui-delete", accessibilityLabel = stringResource(R.string.delete_game, game.name),
                                colors = listOf(SelectionAccent), showBorder = false)
                            BubbleButton(onClick = { onPlay(game.id) }, enabled = enabled, iconAssetId = "ui-play",
                                accessibilityLabel = stringResource(R.string.play_game, game.name),
                                colors = listOf(SelectionAccent), showBorder = false)
                        }
                    }
                }
            }
            BubbleButton(onClick = onAdd, iconAssetId = "ui-plus", enabled = enabled, diameter = 72.dp,
                accessibilityLabel = stringResource(R.string.add_game),
                colors = listOf(Color.White), containerColors = listOf(SelectionAccent), showBorder = false,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp).testTag("add-game-fab"))
        }
        HomeNavigationBar(librarySelected = false, homeSelected = false, onGamesSelected = onHome, onLibrarySelected = onLibrary,
            accentColor = SelectionAccent)
    }
    }
}

@Preview(name = "What's That - selection", widthDp = 360, heightDp = 800)
@Preview(name = "What's That - selection landscape", widthDp = 800, heightDp = 360)
@Preview(name = "What's That - selection large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun GameSelectionPreview() {
    PuzzleBubblesTheme {
        WhatsThatGameSelectionScreen(listOf(WhatsThatGameSummary("whats-that", "Animals", isBuiltIn = true)), {}, {}, {})
    }
}

@Preview(name = "What's That - multiple games", widthDp = 360, heightDp = 800)
@Composable
private fun MultipleGamesPreview() {
    PuzzleBubblesTheme {
        WhatsThatGameSelectionScreen(listOf(
            WhatsThatGameSummary("whats-that", "Animals", isBuiltIn = true),
            WhatsThatGameSummary("whats-that-preview-a", "My Animals"),
            WhatsThatGameSummary("whats-that-preview-b", "A longer family game name"),
        ), {}, {}, {})
    }
}
