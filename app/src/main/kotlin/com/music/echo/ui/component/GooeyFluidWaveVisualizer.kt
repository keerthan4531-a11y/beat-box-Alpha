package iad1tya.echo.music.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

/**
 * A Gooey Fluid Wave Visualizer using Jetpack Compose Canvas.
 * Creates multiple animated overlapping sine waves to simulate fluid/liquid beats.
 */
@Composable
fun GooeyFluidWaveVisualizer(
    modifier: Modifier = Modifier,
    baseColor: Color,
    isPlaying: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_waves")

    // Phase shift for the waves to move them horizontally
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    // Amplitude modulation to simulate beat pulses
    val amplitudeMod by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude"
    )

    val activeAmplitude = if (isPlaying) amplitudeMod else 0.1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Define colors with varying opacities to create the "gooey" depth effect
        val color1 = baseColor.copy(alpha = 0.4f)
        val color2 = baseColor.copy(alpha = 0.6f)
        val color3 = baseColor.copy(alpha = 0.8f)

        fun drawWave(phase: Float, color: Color, amplitudeScale: Float, verticalOffsetRatio: Float, frequencyScale: Float) {
            val path = Path()
            val waveHeight = height * verticalOffsetRatio
            val baseAmplitude = height * 0.15f * activeAmplitude * amplitudeScale

            path.moveTo(0f, height)
            path.lineTo(0f, waveHeight)

            val step = width / 50
            for (x in 0..width.toInt() step step.toInt()) {
                // Combine two sine waves for organic movement
                val normalizedX = x / width
                val y = waveHeight + 
                        sin(normalizedX * PI.toFloat() * frequencyScale + phase) * baseAmplitude +
                        sin(normalizedX * PI.toFloat() * (frequencyScale * 1.5f) - phase * 0.5f) * (baseAmplitude * 0.5f)
                
                path.lineTo(x.toFloat(), y.toFloat())
            }

            path.lineTo(width, height)
            path.close()

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0f)),
                    startY = waveHeight - baseAmplitude,
                    endY = height
                )
            )
        }

        // Draw multiple layered waves
        drawWave(phase1, color1, 1.2f, 0.6f, 2f)
        drawWave(phase2, color2, 0.9f, 0.7f, 3f)
        drawWave(phase3, color3, 0.7f, 0.8f, 1.5f)
    }
}
