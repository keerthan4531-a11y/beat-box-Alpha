package com.music.echo.ui.component.neomorphic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.ui.theme.NeomorphDefaults
import iad1tya.echo.music.ui.theme.neomorphicConvex

@Composable
fun NeomorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = NeomorphDefaults.CardRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    elevation: Dp = 6.dp,
    accentTint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val baseModifier = modifier.neomorphicConvex(
        cornerRadius = cornerRadius,
        shape = shape,
        elevation = elevation,
        accentTint = accentTint
    )

    val clickableModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        baseModifier
    }

    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.Center,
        content = content
    )
}
