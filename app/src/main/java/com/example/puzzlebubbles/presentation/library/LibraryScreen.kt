package com.example.puzzlebubbles.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.puzzlebubbles.presentation.home.HomePage
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily

@Composable
fun LibraryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    HomePage(
        librarySelected = true,
        onGamesSelected = onBack,
        onLibrarySelected = {},
        modifier = modifier,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AssetImage("ui-library", Modifier.size(88.dp), colorFilter = ColorFilter.tint(Color(0xFF9251B7)))
                Text(
                    text = stringResource(R.string.library),
                    fontFamily = HomeHeadingFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color(0xFF18345B),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.library_placeholder),
                    fontFamily = HomeHeadingFontFamily,
                    color = Color(0xFF6A7F9E),
                    textAlign = TextAlign.Center,
                )
                BubbleButton(
                    onClick = onBack,
                    iconAssetId = "ui-back",
                    text = stringResource(R.string.back),
                    showBorder = false,
                )
            }
        }
    }
}
