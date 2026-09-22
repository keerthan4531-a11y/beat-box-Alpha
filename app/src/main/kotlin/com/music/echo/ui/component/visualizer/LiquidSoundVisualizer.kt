package iad1tya.echo.music.ui.component.visualizer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.sin

@Composable
fun LiquidSoundVisualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
) {
    val infiniteTransition = rememberInfiniteTransition()

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 2000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.1f,
        animationSpec = tween(1000)
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        val path = Path().apply {
            moveTo(0f, centerY)
            for (x in 0..width.toInt() step 5) {
                val normalizedX = x / width
                val frequency = 3f // How many waves fit in the width
                val yOffset = sin((normalizedX * 2f * Math.PI.toFloat() * frequency) + phase) * (30f * amplitudeMultiplier)
                lineTo(x.toFloat(), centerY + yOffset)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Fill
        )
    }
}
