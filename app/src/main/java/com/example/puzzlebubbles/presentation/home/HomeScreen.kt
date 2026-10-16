package com.example.puzzlebubbles.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameType
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import com.example.puzzlebubbles.ui.theme.GameTitleFontFamily
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

private val HeadingBlue = Color(0xFF166EC5)
private val InkBlue = Color(0xFF18345B)

@Composable
fun HomeScreen(
    games: List<GameDefinition>,
    onGameSelected: (GameDefinition) -> Unit,
    modifier: Modifier = Modifier,
    onLibrarySelected: () -> Unit = {},
) {
    HomePage(
        librarySelected = false,
        onGamesSelected = {},
        onLibrarySelected = onLibrarySelected,
        modifier = modifier,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { HomeIntro() }
        items(games, key = { it.id }) { game ->
            GameCard(game = game, onClick = { onGameSelected(game) })
        }
    }
}

@Composable
internal fun HomePage(
    librarySelected: Boolean,
    onGamesSelected: () -> Unit,
    onLibrarySelected: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val gridState = rememberLazyGridState()
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFFF0FCFF), Color(0xFFF2EEFF), Color(0xFFFFF8F0))),
        ),
    ) {
        BubbleBackdrop(Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        )) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                val wide = maxWidth >= 700.dp
                val pageWidth = minOf(maxWidth, if (wide) 760.dp else 380.dp)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (wide) 2 else 1),
                    state = gridState,
                    modifier = Modifier.widthIn(max = if (wide) 760.dp else 380.dp).fillMaxSize()
                        .testTag("home-content"),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HomeHeading(viewportWidth = pageWidth.value)
                    }
                    content()
                }
            }
            HomeNavigationBar(librarySelected, onGamesSelected, onLibrarySelected)
        }
    }
}

@Composable
private fun HomeHeading(viewportWidth: Float) {
    // CSS rem uses a 16px base; map CSS px to dp/sp and vw to the page width.
    val titleSize = (viewportWidth * .10f).coerceIn(36f, 55.2f)
    val logoHeight = 44.dp
    val logoWidth = logoHeight
    val brandGap = 4.dp
    val shadowOffset = with(LocalDensity.current) { 3.dp.toPx() }
    val headingStyle = TextStyle(
        fontFamily = HomeHeadingFontFamily,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy((-16).dp),
    ) {
        Text(
            text = stringResource(R.string.home_welcome),
            style = headingStyle,
            fontSize = 12.48.sp,
            lineHeight = 18.sp,
            letterSpacing = .14.em,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF5B77A0),
            modifier = Modifier.padding(start = logoWidth + brandGap + 3.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = logoHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(brandGap),
        ) {
            AssetImage(assetId = "app-logo", modifier = Modifier.size(width = logoWidth, height = logoHeight))
            Text(
                text = stringResource(R.string.app_name),
                style = headingStyle.copy(
                    shadow = Shadow(Color.White, Offset(0f, shadowOffset), blurRadius = 0f),
                ),
                fontSize = titleSize.sp,
                lineHeight = titleSize.sp,
                letterSpacing = (-.045).em,
                fontWeight = FontWeight.ExtraBold,
                color = HeadingBlue,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
    }
}

@Composable
private fun HomeIntro() {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val questionSize = (maxWidth.value * .06f).coerceIn(23.2f, 32f)
        val headingStyle = TextStyle(
            fontFamily = HomeHeadingFontFamily,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_question),
                style = headingStyle,
                fontSize = questionSize.sp,
                lineHeight = (questionSize * 1.15f).sp,
                fontWeight = FontWeight.Bold,
                color = InkBlue,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_hint),
                style = headingStyle,
                fontSize = 15.2.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF6A7F9E),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun GameCard(game: GameDefinition, onClick: () -> Unit) {
    val barColors = when (game.type) {
        GameType.CHOOSE_IT -> listOf(Color(0xFF0879C7), Color(0xFF19BCE8))
        GameType.MATCHING -> listOf(Color(0xFF864ACD), Color(0xFFD568D6))
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            AssetImage(
                assetId = game.coverImageAssetId,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Fit,
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(barColors))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = GameTitleFontFamily,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                // Decorative play cue; the whole card is the single accessible action.
                AssetImage(
                    assetId = "ui-play",
                    modifier = Modifier.size(48.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFFFE790)),
                )
            }
        }
    }
}

@Composable
private fun BubbleBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bubbles = listOf(
            Triple(.06f, .09f, 9f), Triple(.94f, .17f, 7f),
            Triple(.09f, .34f, 14f), Triple(.96f, .45f, 10f),
            Triple(.03f, .58f, 7f), Triple(.92f, .71f, 15f),
            Triple(.11f, .87f, 8f), Triple(.97f, .94f, 6f),
        )
        bubbles.forEach { (x, y, radius) ->
            drawBubble(Offset(size.width * x, size.height * y), radius.dp.toPx(), .22f)
        }
    }
}

private fun DrawScope.drawBubble(center: Offset, radius: Float, alpha: Float = 1f) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFDCF9FF), Color(0xFF8DE5FA), Color(0xFFB9BAF7)),
            center = center - Offset(radius * .3f, radius * .3f),
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = center,
        alpha = alpha,
    )
    drawCircle(Color.White, radius, center, alpha = alpha * .85f, style = Stroke(1.5.dp.toPx()))
    drawCircle(Color.White, radius * .19f, center - Offset(radius * .35f, radius * .4f), alpha = alpha)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
private fun HomeScreenPreview() {
    PuzzleBubblesTheme {
        HomeScreen(LocalGameRepository().getGameCatalog(), onGameSelected = {}, modifier = Modifier.fillMaxSize())
    }
}
