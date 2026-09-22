package iad1tya.echo.music.ui.component.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.ui.theme.LiquidGlassDefaults
import iad1tya.echo.music.ui.theme.liquidGlass

/**
 * Top-level Liquid Glass Surface
 * Used for floating navigation bars, bottom sheets, mini players, and modal containers.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LiquidGlassDefaults.PillCornerRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    glassAlpha: Float = 0.82f,
    tintColor: Color = Color.Unspecified,
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            cornerRadius = cornerRadius,
            shape = shape,
            glassAlpha = glassAlpha,
            tintColor = tintColor,
            borderWidth = 1.dp,
            elevation = elevation
        ),
        content = content
    )
}
