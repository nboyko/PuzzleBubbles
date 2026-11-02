package com.example.puzzlebubbles.presentation.games.chooseit

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.domain.ChooseItGameSummary
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.presentation.home.HomeNavigationBar
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

private val SelectionInk = Color(0xFF18345B)
private val SelectionAccent = Color(0xFF166EC5)

@Composable
fun WhatsThatGameSelectionScreen(
    games: List<ChooseItGameSummary>,
    onPlay: (String) -> Unit,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenDescription = stringResource(R.string.whats_that_games)
    Column(modifier.fillMaxSize().testTag("whats-that-selection").semantics { paneTitle = screenDescription }
        .background(Brush.verticalGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF6F0FF))))
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(Modifier.fillMaxSize().testTag("whats-that-game-list"),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(games, key = { it.id }) { game ->
                    Card(onClick = { onPlay(game.id) }, modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
                        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(
                            containerColor = if (game.isBuiltIn) Color(0xFFDDEEFF) else Color.White)) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AssetImage("ui-games", Modifier.size(56.dp), colorFilter = ColorFilter.tint(SelectionAccent))
                            Text(game.name, Modifier.weight(1f),
                                color = if (game.isBuiltIn) SelectionAccent else SelectionInk,
                                fontFamily = HomeHeadingFontFamily, fontSize = 24.sp,
                                fontWeight = if (game.isBuiltIn) FontWeight.Bold else FontWeight.Normal)
                            BubbleButton(onClick = { onPlay(game.id) }, iconAssetId = "ui-play",
                                accessibilityLabel = stringResource(R.string.play_game, game.name),
                                colors = listOf(SelectionAccent), showBorder = false)
                        }
                    }
                }
            }
            BubbleButton(onClick = {}, iconAssetId = "ui-plus", enabled = false, diameter = 72.dp,
                accessibilityLabel = stringResource(R.string.add_game_coming_soon),
                colors = listOf(Color.White), containerColors = listOf(SelectionAccent), showBorder = false,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 8.dp).testTag("add-game-fab"))
        }
        HomeNavigationBar(librarySelected = false, onGamesSelected = onHome, onLibrarySelected = onLibrary)
    }
}

@Preview(name = "What's That - selection", widthDp = 360, heightDp = 800)
@Preview(name = "What's That - selection landscape", widthDp = 800, heightDp = 360)
@Preview(name = "What's That - selection large text", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun GameSelectionPreview() {
    PuzzleBubblesTheme {
        WhatsThatGameSelectionScreen(listOf(ChooseItGameSummary("whats-that", "Animals", isBuiltIn = true)), {}, {}, {})
    }
}

@Preview(name = "What's That - multiple games", widthDp = 360, heightDp = 800)
@Composable
private fun MultipleGamesPreview() {
    PuzzleBubblesTheme {
        WhatsThatGameSelectionScreen(listOf(
            ChooseItGameSummary("whats-that", "Animals", isBuiltIn = true),
            ChooseItGameSummary("whats-that-preview-a", "My Animals"),
            ChooseItGameSummary("whats-that-preview-b", "A longer family game name"),
        ), {}, {}, {})
    }
}
