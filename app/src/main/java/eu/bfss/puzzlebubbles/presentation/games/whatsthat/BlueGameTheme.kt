package eu.bfss.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlueGameColors = lightColorScheme(
    primary = Color(0xFF166CAF), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEEFF), onPrimaryContainer = Color(0xFF18345B),
    secondary = Color(0xFF286DAD), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F0FF), onSecondaryContainer = Color(0xFF18345B),
    tertiary = Color(0xFF3F78AD), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5F2FF), onTertiaryContainer = Color(0xFF18345B),
    background = Color(0xFFF0F7FF), onBackground = Color(0xFF18345B),
    surface = Color(0xFFF0F7FF), onSurface = Color(0xFF18345B),
    surfaceVariant = Color(0xFFE2EEFA), onSurfaceVariant = Color(0xFF496581),
    surfaceTint = Color(0xFF166CAF), outline = Color(0xFF7595B5), outlineVariant = Color(0xFFBDCEE0),
    surfaceDim = Color(0xFFD8E6F4), surfaceBright = Color(0xFFF5FAFF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F7FF),
    surfaceContainer = Color(0xFFEAF3FC), surfaceContainerHigh = Color(0xFFE2EEFA),
    surfaceContainerHighest = Color(0xFFDAE8F6),
    error = Color(0xFF185D95), onError = Color.White,
    errorContainer = Color(0xFFDDEEFF), onErrorContainer = Color(0xFF18345B),
)

/** Scope setup/list controls to blue without changing the Home or Library palettes. */
@Composable
internal fun BlueGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BlueGameColors, typography = MaterialTheme.typography, content = content)
}
