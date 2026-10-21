package com.example.puzzlebubbles.presentation.library

import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.presentation.components.BubbleButton
import com.example.puzzlebubbles.presentation.home.HomeNavigationBar
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily

@Composable
fun SoundsScreen(onBack: () -> Unit, onHome: () -> Unit, modifier: Modifier = Modifier) {
    EmptyLibraryScreen(
        title = stringResource(R.string.sounds),
        message = stringResource(R.string.no_sounds_yet),
        iconAssetId = "ui-sound", accent = Color(0xFF9251B7),
        onBack = onBack, onHome = onHome, modifier = modifier,
    )
}

@Composable
private fun EmptyLibraryScreen(
    title: String,
    message: String,
    iconAssetId: String,
    accent: Color,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF6F0FF))))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BubbleButton(
                    onClick = onBack, iconAssetId = "ui-back",
                    accessibilityLabel = stringResource(R.string.back),
                    colors = listOf(Color(0xFF18345B)), showBorder = false,
                )
                Text(
                    title, modifier = Modifier.weight(1f).semantics { heading() },
                    fontFamily = HomeHeadingFontFamily, fontWeight = FontWeight.Bold,
                    fontSize = 32.sp, color = Color(0xFF18345B),
                )
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                AssetImage(iconAssetId, Modifier.size(112.dp), colorFilter = ColorFilter.tint(accent))
                Text(
                    message, fontFamily = HomeHeadingFontFamily, fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF5B77A0),
                    textAlign = TextAlign.Center,
                )
            }
        }
        HomeNavigationBar(
            librarySelected = true,
            onGamesSelected = onHome,
            onLibrarySelected = onBack,
        )
    }
}

@Preview(name = "Sounds - empty", widthDp = 360, heightDp = 800)
@Composable
private fun SoundsScreenPreview() {
    PuzzleBubblesTheme {
        SoundsScreen({}, {}, Modifier.fillMaxSize())
    }
}
