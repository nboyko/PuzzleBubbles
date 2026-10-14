package com.example.puzzlebubbles.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puzzlebubbles.ui.theme.HomeHeadingFontFamily

val BubbleButtonMulticolorColors = listOf(Color(0xFF12E9FF), Color(0xFFF52CE5), Color(0xFF15E7FF))

/** One color gives a solid accent; multiple colors give a shared rim/icon gradient. */
@Composable
fun BubbleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconAssetId: String? = null,
    text: String? = null,
    accessibilityLabel: String? = null,
    colors: List<Color> = listOf(Color(0xFF166CAF)),
    textColor: Color = if (colors.size == 1) colors.first() else Color(0xFF24415F),
    showBorder: Boolean = true,
    neon: Boolean = false,
    enabled: Boolean = true,
    diameter: Dp = 56.dp,
) {
    require(colors.isNotEmpty()) { "A button needs at least one color" }
    require(iconAssetId != null || !text.isNullOrBlank()) { "A button needs an icon or text" }
    val iconOnly = text.isNullOrBlank()
    require(!iconOnly || !accessibilityLabel.isNullOrBlank()) { "Icon buttons need an accessibility label" }
    val shape = if (iconOnly) CircleShape else RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) .96f else 1f, label = "button press")
    val gradientColors = if (colors.size == 1) listOf(colors.first(), colors.first()) else colors
    val brush = Brush.linearGradient(gradientColors)
    val effectiveDiameter = diameter.coerceAtLeast(48.dp)
    Box(
        modifier = modifier
            .then(if (iconOnly) Modifier.size(effectiveDiameter) else Modifier.defaultMinSize(minWidth = 96.dp, minHeight = 56.dp))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(if (enabled) 1f else .45f)
            .semantics(mergeDescendants = true) {
                if (accessibilityLabel != null) contentDescription = accessibilityLabel
            }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled,
                role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (showBorder) Canvas(Modifier.matchParentSize()) {
            val inset = 6.dp.toPx()
            val origin = Offset(inset, inset)
            val frameSize = Size(size.width - inset * 2, size.height - inset * 2)
            val corner = CornerRadius(frameSize.height / 2)
            if (neon) {
                drawRoundRect(brush, origin, frameSize, corner, alpha = .07f, style = Stroke(6.dp.toPx()))
                drawRoundRect(brush, origin, frameSize, corner, alpha = .12f, style = Stroke(3.dp.toPx()))
            }
            drawRoundRect(brush, origin, frameSize, corner, style = Stroke(1.2.dp.toPx()))
            if (neon) drawRoundRect(Color.White.copy(alpha = .18f), origin, frameSize, corner,
                style = Stroke(.4.dp.toPx()))
        }
        Row(
            modifier = if (iconOnly) Modifier else Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (iconAssetId != null) {
                val iconModifier = Modifier.size(if (iconOnly) effectiveDiameter * .8f else 32.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush, blendMode = BlendMode.SrcIn)
                            }
                        }
                Box {
                    if (neon) AssetImage(
                        assetId = iconAssetId,
                        modifier = iconModifier.alpha(.25f).blur(2.dp, BlurredEdgeTreatment.Unbounded),
                    )
                    AssetImage(assetId = iconAssetId, modifier = iconModifier)
                }
            }
            if (!iconOnly) {
                Text(text = requireNotNull(text), color = textColor, fontFamily = HomeHeadingFontFamily,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    style = TextStyle(shadow = if (neon) Shadow(colors.first().copy(alpha = .25f), blurRadius = 4f) else null))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2EEFF)
@Composable
private fun BubbleButtonPreview() {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BubbleButton(onClick = {}, iconAssetId = "ui-back", accessibilityLabel = "Back", showBorder = false)
        BubbleButton(onClick = {}, iconAssetId = "ui-sound", accessibilityLabel = "Replay", neon = true)
        BubbleButton(onClick = {}, text = "Play", colors = listOf(Color(0xFF0879C7)))
        BubbleButton(onClick = {}, text = "Next", iconAssetId = "ui-back", colors = BubbleButtonMulticolorColors, neon = true)
    }
}
