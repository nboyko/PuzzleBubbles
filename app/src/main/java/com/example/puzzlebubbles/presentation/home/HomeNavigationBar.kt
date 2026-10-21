package com.example.puzzlebubbles.presentation.home

import androidx.compose.ui.tooling.preview.Preview
import com.example.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.R
import com.example.puzzlebubbles.presentation.components.AssetImage
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily
import kotlin.math.asin
import kotlin.math.sqrt

@Composable
internal fun HomeNavigationBar(
    librarySelected: Boolean,
    onGamesSelected: () -> Unit,
    onLibrarySelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().testTag("bottom-navigation")) {
        Canvas(Modifier.matchParentSize()) {
            val top = 24.dp.toPx()
            val inset = 12.dp.toPx()
            val selectedCenter = inset + (size.width - inset * 2) * if (librarySelected) .75f else .25f
            val bubbleCenterY = 40.dp.toPx()
            val cutoutRadius = 33.dp.toPx()
            val cornerRadius = 8.dp.toPx()
            val cornerY = top + cornerRadius
            val centerDistance = cutoutRadius + cornerRadius
            val verticalDistance = bubbleCenterY - cornerY
            val shoulder = sqrt(centerDistance * centerDistance - verticalDistance * verticalDistance)
            val tangentAngle = Math.toDegrees(asin((verticalDistance / centerDistance).toDouble())).toFloat()
            // Tangent circular arcs keep both shoulders smooth and the gap close to the bubble.
            val bar = Path().apply {
                moveTo(0f, top)
                lineTo(selectedCenter - shoulder, top)
                arcTo(
                    Rect(selectedCenter - shoulder - cornerRadius, top,
                        selectedCenter - shoulder + cornerRadius, top + cornerRadius * 2),
                    270f, 90f + tangentAngle, false,
                )
                arcTo(
                    Rect(selectedCenter - cutoutRadius, bubbleCenterY - cutoutRadius,
                        selectedCenter + cutoutRadius, bubbleCenterY + cutoutRadius),
                    180f + tangentAngle, -180f - tangentAngle * 2, false,
                )
                arcTo(
                    Rect(selectedCenter + shoulder - cornerRadius, top,
                        selectedCenter + shoulder + cornerRadius, top + cornerRadius * 2),
                    180f - tangentAngle, 90f + tangentAngle, false,
                )
                lineTo(size.width, top)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(bar, Brush.horizontalGradient(listOf(Color(0xFFDDEFFA), Color(0xFFEDE3F8))))
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            val destinations = listOf(
                Triple("ui-games", stringResource(R.string.home), !librarySelected),
                Triple("ui-library", stringResource(R.string.library), librarySelected),
            )
            destinations.forEachIndexed { index, (icon, title, active) ->
                val accent = if (index == 0) Color(0xFF166EC5) else Color(0xFF9251B7)
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val emphasis by animateFloatAsState(if (pressed) 1f else 0f, label = "navigation touch")
                val iconColor = lerp(accent, Color.White, emphasis * .25f)
                val contentEmphasis = Modifier.graphicsLayer {
                    scaleX = 1f + emphasis * .04f
                    scaleY = scaleX
                }
                Column(
                    modifier = Modifier.weight(1f).selectable(
                        selected = active, role = Role.Tab,
                        interactionSource = interaction, indication = null,
                        onClick = if (index == 0) onGamesSelected else onLibrarySelected,
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        if (active) SelectedNavigationBubble(Modifier.matchParentSize(), accent)
                        AssetImage(
                            icon, Modifier.size(32.dp).then(contentEmphasis)
                                .graphicsLayer { translationY = if (active) 0f else 12.dp.toPx() },
                            colorFilter = ColorFilter.tint(iconColor),
                        )
                    }
                    Text(
                        title, modifier = contentEmphasis, color = iconColor,
                        fontFamily = HomeHeadingFontFamily,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedNavigationBubble(modifier: Modifier, accent: Color) {
    Canvas(modifier) {
        val radius = size.minDimension / 2 - 3.dp.toPx()
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFFE5F8FF), accent.copy(alpha = .3f)),
                center = center - Offset(radius * .3f, radius * .35f), radius = radius * 1.6f,
            ), radius,
        )
        drawCircle(
            Brush.linearGradient(listOf(Color(0xFF88DCEB), Color.White, Color(0xFFD5B1F2), Color(0xFF86D4EA))),
            radius, style = Stroke(2.dp.toPx()),
        )
    }
}

@Preview(name = "Navigation - both selections", widthDp = 360, showBackground = true, backgroundColor = 0xFFEAF8FF)
@Composable
private fun HomeNavigationBarPreview() {
    PuzzleBubblesTheme {
        Column {
            HomeNavigationBar(false, {}, {})
            HomeNavigationBar(true, {}, {})
        }
    }
}
