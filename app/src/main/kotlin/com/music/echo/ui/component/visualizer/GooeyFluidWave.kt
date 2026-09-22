package iad1tya.echo.music.ui.component.visualizer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.sin

@Composable
fun GooeyFluidWave(modifier: Modifier = Modifier, color: Color = Color(0xFF8A2BE2)) {
    val infiniteTransition = rememberInfiniteTransition(label = "FluidWave")
    
    // Animate phase for wave movement
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Simulate some audio reactivity via a slow amplitude throb
    val amplitudeMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val path = Path()
        val amplitude = 50f * amplitudeMultiplier
        val frequency = 2f // Two wave peaks

        path.moveTo(0f, height)
        path.lineTo(0f, height * 0.7f)

        // Draw dynamic wave using sine function
        for (x in 0..width.toInt() step 5) {
            val normalizedX = x / width
            val yOffset = sin(normalizedX * frequency * Math.PI.toFloat() * 2f + phase) * amplitude
            // Add a secondary overlapping frequency for gooey effect
            val gooeyOffset = sin(normalizedX * 4f * Math.PI.toFloat() + phase * 1.5f) * (amplitude * 0.5f)
            
            path.lineTo(x.toFloat(), height * 0.7f + yOffset + gooeyOffset)
        }

        path.lineTo(width, height)
        path.close()

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.1f)),
                startY = height * 0.5f,
                endY = height
            ),
            style = Fill
        )
    }
}
