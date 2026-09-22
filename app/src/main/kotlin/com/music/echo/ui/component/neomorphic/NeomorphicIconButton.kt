package com.music.echo.ui.component.neomorphic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.ui.theme.neomorphicCircleButton

@Composable
fun NeomorphicIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
    accentTint: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size)
            .neomorphicCircleButton(
                accentTint = accentTint,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
