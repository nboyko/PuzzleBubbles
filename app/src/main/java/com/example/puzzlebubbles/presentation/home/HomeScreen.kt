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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.data.LocalGameRepository
import com.example.puzzlebubbles.domain.GameDefinition
import com.example.puzzlebubbles.domain.GameType
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import com.example.puzzlebubbles.ui.theme.GameTitleFontFamily
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme

private val HeadingBlue = Color(0xFF166CAF)
private val InkBlue = Color(0xFF24415F)

@Composable
fun HomeScreen(
    games: List<GameDefinition>,
    onGameSelected: (GameDefinition) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFFF0FCFF), Color(0xFFF2EEFF), Color(0xFFFFF8F0))),
        ),
    ) {
        BubbleBackdrop(Modifier.matchParentSize())
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val wide = maxWidth >= 700.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (wide) 2 else 1),
                modifier = Modifier.widthIn(max = if (wide) 760.dp else 380.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HomeHeading()
                }
                items(games, key = { it.id }) { game ->
                    GameCard(game = game, onClick = { onGameSelected(game) })
                }
            }
        }
    }
}

@Composable
private fun HomeHeading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(44.dp).padding(end = 8.dp)) {
                drawBubble(Offset(size.width * .62f, size.height * .58f), size.minDimension * .35f)
                drawBubble(Offset(size.width * .23f, size.height * .23f), size.minDimension * .18f)
            }
            Column {
                Text(
                    text = stringResource(R.string.home_welcome),
                    fontFamily = HomeHeadingFontFamily,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlue.copy(alpha = .7f),
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = HomeHeadingFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeadingBlue,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        Text(
            text = stringResource(R.string.home_question),
            fontFamily = HomeHeadingFontFamily,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.home_hint),
            fontFamily = HomeHeadingFontFamily,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
        )
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
                Canvas(Modifier.size(36.dp)) {
                    drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE879), Color(0xFFFFBA24))))
                    drawCircle(Color.White.copy(alpha = .55f), style = Stroke(1.5.dp.toPx()))
                    val triangle = Path().apply {
                        moveTo(size.width * .42f, size.height * .28f)
                        lineTo(size.width * .72f, size.height * .5f)
                        lineTo(size.width * .42f, size.height * .72f)
                        close()
                    }
                    drawPath(triangle, Color.White)
                }
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
