package com.example.puzzlebubbles.presentation.library

import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.presentation.home.HomeNavigationBar
import com.example.puzzlebubbles.ui.theme.GameTitleFontFamily
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onPhotosSelected: () -> Unit,
    onSoundsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFFF0FCFF), Color(0xFFF2EEFF), Color(0xFFFFF8F0))),
        ).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BubbleButton(
                onClick = onBack,
                iconAssetId = "ui-back",
                accessibilityLabel = stringResource(R.string.back),
                colors = listOf(Color(0xFF166CAF)),
                showBorder = false,
            )
            Text(
                text = stringResource(R.string.my_library),
                fontFamily = GameTitleFontFamily,
                fontSize = 32.sp,
                color = Color(0xFF166CAF),
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            val wide = maxWidth >= 700.dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (wide) 2 else 1),
                modifier = Modifier.widthIn(max = if (wide) 760.dp else 380.dp).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    LibrarySectionCard(
                        title = stringResource(R.string.photos), iconAssetId = "ui-image",
                        accent = Color(0xFF166EC5), onClick = onPhotosSelected,
                    )
                }
                item {
                    LibrarySectionCard(
                        title = stringResource(R.string.sounds), iconAssetId = "ui-sound",
                        accent = Color(0xFF9251B7), onClick = onSoundsSelected,
                    )
                }
            }
        }
        HomeNavigationBar(
            librarySelected = true,
            onGamesSelected = onBack,
            onLibrarySelected = {},
        )
    }
}

@Composable
private fun LibrarySectionCard(title: String, iconAssetId: String, accent: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(72.dp).background(accent.copy(alpha = .09f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AssetImage(iconAssetId, Modifier.size(52.dp), colorFilter = ColorFilter.tint(accent))
            }
            Text(
                title, fontFamily = HomeHeadingFontFamily, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, color = Color(0xFF18345B), modifier = Modifier.weight(1f),
            )
            AssetImage(
                "ui-back", Modifier.size(24.dp).graphicsLayer { scaleX = -1f },
                colorFilter = ColorFilter.tint(accent),
            )
        }
    }
}

@Preview(name = "My Library", widthDp = 360, heightDp = 800)
@Preview(name = "My Library - landscape", widthDp = 800, heightDp = 360)
@Composable
private fun LibraryScreenPreview() {
    PuzzleBubblesTheme {
        LibraryScreen({}, {}, {}, Modifier.fillMaxSize())
    }
}