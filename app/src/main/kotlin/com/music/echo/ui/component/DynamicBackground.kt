package iad1tya.echo.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import iad1tya.echo.music.ui.theme.NeomorphDefaults

import iad1tya.echo.music.ui.theme.isAppInDarkTheme

/**
 * High-End Dynamic Background for Light & Dark Mode.
 * In Light Mode: Smooth sculpted porcelain canvas with soft accent glow.
 * In Dark Mode: Sleek obsidian canvas with ambient aura.
 */
@Composable
fun DynamicBackground(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isAppInDarkTheme()

    val backgroundBrush = remember(isDark, dominantColor) {
        if (isDark) {
            val darkBase1 = Color(0xFF08090E)
            val darkBase2 = Color(0xFF11131C)
            val blendedBg = if (dominantColor != Color.Unspecified) dominantColor.copy(alpha = 0.14f) else darkBase2
            Brush.verticalGradient(
                colors = listOf(darkBase1, blendedBg, darkBase2)
            )
        } else {
            val lightBase1 = Color(0xFFF5F7FA)
            val lightBase2 = Color(0xFFF8FAFD)
            val blendedBg = if (dominantColor != Color.Unspecified) dominantColor.copy(alpha = 0.05f) else Color(0xFFF5F7FA)
            Brush.verticalGradient(
                colors = listOf(lightBase1, blendedBg, lightBase2)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush)
        )
        content()
    }
}
