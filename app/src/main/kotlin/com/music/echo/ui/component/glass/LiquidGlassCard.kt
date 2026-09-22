package iad1tya.echo.music.ui.component.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.ui.theme.LiquidGlassDefaults
import iad1tya.echo.music.ui.theme.liquidGlass
import iad1tya.echo.music.ui.theme.liquidGlassGlow
import iad1tya.echo.music.ui.theme.liquidGlassPressable

/**
 * Ultra-Luxury Liquid Glass Card Component
 * Combines frosted translucent surface, specular refraction border, dynamic ambient glow,
 * and tactile spring bounce.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LiquidGlassDefaults.CardCornerRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    tintColor: Color = Color.Unspecified,
    glowColor: Color? = null,
    glassAlpha: Float = 0.75f,
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var cardModifier = modifier

    if (glowColor != null) {
        cardModifier = cardModifier.liquidGlassGlow(
            glowColor = glowColor,
            cornerRadius = cornerRadius,
            glowRadius = 12.dp
        )
    }

    cardModifier = cardModifier.liquidGlass(
        cornerRadius = cornerRadius,
        shape = shape,
        glassAlpha = glassAlpha,
        tintColor = tintColor,
        borderWidth = 1.dp,
        elevation = elevation
    )

    if (onClick != null) {
        cardModifier = cardModifier.liquidGlassPressable(
            onClick = onClick
        )
    }

    Box(
        modifier = cardModifier,
        content = content
    )
}
