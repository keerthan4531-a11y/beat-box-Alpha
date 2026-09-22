package iad1tya.echo.music.ui.component.glass

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.ui.theme.LiquidGlassDefaults
import iad1tya.echo.music.ui.theme.liquidGlassPressable
import iad1tya.echo.music.ui.theme.neomorphicGlass

/**
 * Liquid Glass & Neomorphic Button Component
 * Engineered for crystal playback buttons (Play/Pause, Skip, Shuffle, Repeat) and action chips.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCircle: Boolean = false,
    cornerRadius: Dp = LiquidGlassDefaults.ButtonCornerRadius,
    shape: Shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius),
    accentColor: Color = Color.Unspecified,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .neomorphicGlass(
                cornerRadius = if (isCircle) 999.dp else cornerRadius,
                shape = shape,
                accentColor = accentColor,
                isPressed = isPressed
            )
            .liquidGlassPressable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}
