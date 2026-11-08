package com.example.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
    onEdit: (String) -> Unit = {},
    enabled: Boolean = true,
    loadError: Boolean = false,
    onRetry: () -> Unit = {},
    deletingGameId: String? = null,
) {
    BlueGameTheme {
    val screenDescription = stringResource(R.string.whats_that_games)
    val deletionDescription = stringResource(R.string.game_pending_delete)
    var openId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(enabled, games.map { it.id }) {
        if (!enabled || games.none { it.id == openId }) openId = null
    }
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openId = null }
    BackHandler(enabled && openId != null) { openId = null }
    Column(modifier.fillMaxSize().testTag("whats-that-selection").semantics { paneTitle = screenDescription }
        .background(Brush.verticalGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF0F7FF))))
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        if (loadError) {
            Text(stringResource(R.string.game_load_error), Modifier.padding(16.dp))
            androidx.compose.material3.TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
        Box(Modifier.fillMaxWidth().weight(1f).pointerInput(Unit) { detectTapGestures { openId = null } }) {
            LazyColumn(Modifier.fillMaxSize().testTag("whats-that-game-list"), state = listState,
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(games, key = { it.id }) { game ->
                    SelectionGameRow(game, enabled, openId == game.id,
                        if (game.id == deletingGameId) deletionDescription else null,
                        onOpen = { openId = if (it) game.id else null },
                        onPlay = { if (openId != null) openId = null else onPlay(game.id) },
                        onEdit = { openId = null; onEdit(game.id) },
                        onDelete = { openId = null; onDelete(game.id) })
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

@Composable
private fun SelectionGameRow(game: WhatsThatGameSummary, enabled: Boolean, open: Boolean,
    deletingDescription: String?, onOpen: (Boolean) -> Unit, onPlay: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val playLabel = stringResource(R.string.play_game, game.name)
    val editLabel = stringResource(R.string.edit_game_action)
    val deleteLabel = stringResource(R.string.delete_game_action)
    val openLabel = stringResource(R.string.game_actions_open)
    val currentOnOpen by rememberUpdatedState(onOpen)
    BoxWithConstraints(Modifier.fillMaxWidth().clip(shape)) {
        val revealWidth = minOf(160.dp, maxWidth / 2)
        val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
        var dragging by remember { mutableStateOf(false) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        val offset by animateFloatAsState(if (dragging) dragOffset else if (open) -revealPx else 0f,
            animationSpec = if (dragging) snap() else tween(180), label = "game row reveal")
        val visibleWidth = maxWidth + with(LocalDensity.current) { offset.toDp() }
        if (!game.isBuiltIn && (open || dragging || offset < -.5f)) {
            Box(Modifier.matchParentSize()) {
                Row(Modifier.align(Alignment.CenterEnd).width(revealWidth).fillMaxHeight()
                    .then(if (!open || dragging || !enabled) Modifier.clearAndSetSemantics {} else Modifier)) {
                    ManagementAction("ui-edit", Color(0xFF46A58E).copy(alpha = .09f), Color(0xFF3E867B),
                        stringResource(R.string.edit_game_name, game.name), Modifier.weight(1f), enabled && open && !dragging, onEdit)
                    ManagementAction("ui-delete", Color(0xFFD67891).copy(alpha = .09f), Color(0xFFB76B81),
                        stringResource(R.string.delete_game, game.name), Modifier.weight(1f), enabled && open && !dragging, onDelete)
                }
            }
        }
        val color = when {
            deletingDescription != null -> Color(0xFFFFEBEE)
            game.isBuiltIn -> Color(0xFFDDEEFF)
            else -> Color.White
        }
        val swipe = if (!enabled) Modifier else Modifier.pointerInput(game.id, revealPx) {
            detectHorizontalDragGestures(
                onDragStart = { if (!game.isBuiltIn) { dragOffset = offset; dragging = true; currentOnOpen(false) } },
                onHorizontalDrag = { _, delta -> if (!game.isBuiltIn) dragOffset = (dragOffset + delta).coerceIn(-revealPx, 0f) },
                onDragEnd = { if (!game.isBuiltIn) currentOnOpen(dragOffset < -revealPx * .4f); dragging = false },
                onDragCancel = { currentOnOpen(false); dragging = false },
            )
        }
        Card(onClick = onPlay, enabled = enabled,
            modifier = Modifier.offset { IntOffset(offset.roundToInt(), 0) }.fillMaxWidth()
                .testTag("game-row-${game.id}").then(swipe).semantics {
                    role = Role.Button
                    contentDescription = playLabel
                    if (deletingDescription != null) stateDescription = deletingDescription
                    else if (open) stateDescription = openLabel
                    if (!game.isBuiltIn && enabled) customActions = listOf(
                        CustomAccessibilityAction(editLabel) { onEdit(); true },
                        CustomAccessibilityAction(deleteLabel) { onDelete(); true })
                },
            shape = if (offset < 0f) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp,
                topEnd = 0.dp, bottomEnd = 0.dp) else shape,
            colors = CardDefaults.cardColors(containerColor = color, disabledContainerColor = color,
                contentColor = SelectionInk, disabledContentColor = SelectionInk)) {
            // Keep the icon/name in the visible portion as the card's trailing
            // edge slides left. Short names must not disappear behind the clip.
            Row(Modifier.offset { IntOffset(-offset.roundToInt(), 0) }.wrapContentWidth(Alignment.Start).width(visibleWidth)
                .heightIn(min = 96.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssetImage("ui-games", Modifier.size(56.dp), colorFilter = ColorFilter.tint(SelectionAccent))
                Text(game.name, Modifier.weight(1f).padding(8.dp),
                    color = if (game.isBuiltIn) SelectionAccent else SelectionInk,
                    fontFamily = HomeHeadingFontFamily, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (game.isBuiltIn) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ManagementAction(icon: String, background: Color, tint: Color, label: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(modifier.fillMaxHeight().background(background).semantics { contentDescription = label }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(4.dp), contentAlignment = Alignment.Center) {
        AssetImage(icon, Modifier.size(if (icon == "ui-delete") 36.dp else 28.dp), colorFilter = ColorFilter.tint(tint))
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

@Preview(name = "Custom game actions open", widthDp = 360, heightDp = 160)
@Preview(name = "Custom game actions enlarged text", widthDp = 320, heightDp = 180, fontScale = 1.5f)
@Composable
private fun ManagementActionsPreview() {
    PuzzleBubblesTheme {
        SelectionGameRow(WhatsThatGameSummary("preview", "Family game"), true, true, null, {}, {}, {}, {})
    }
}
