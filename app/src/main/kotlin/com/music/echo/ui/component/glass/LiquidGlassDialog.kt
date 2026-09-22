package iad1tya.echo.music.ui.component.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import iad1tya.echo.music.ui.theme.LiquidGlassDefaults
import iad1tya.echo.music.ui.theme.liquidGlass

/**
 * Frosted Crystal Dialog with Specular Edge Refraction
 */
@Composable
fun LiquidGlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LiquidGlassDefaults.DialogCornerRadius,
    tintColor: Color = Color.Unspecified,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = modifier
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .liquidGlass(
                    cornerRadius = cornerRadius,
                    shape = RoundedCornerShape(cornerRadius),
                    glassAlpha = 0.88f,
                    tintColor = tintColor,
                    borderWidth = 1.dp,
                    elevation = 24.dp
                ),
            content = content
        )
    }
}
