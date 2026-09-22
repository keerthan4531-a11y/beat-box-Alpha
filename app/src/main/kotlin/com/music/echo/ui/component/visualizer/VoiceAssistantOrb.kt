package com.music.echo.ui.component.visualizer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    RESPONDING
}

/**
 * Ultra-Luxury Siri / Gemini Style Glowing Dynamic Voice Orb
 * Reacts dynamically to IDLE, LISTENING, THINKING, and RESPONDING voice states.
 */
@Composable
fun VoiceAssistantOrb(
    state: AssistantVoiceState,
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbTransition")

    // Rotation angle for swirling colors
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantVoiceState.THINKING -> 2000
                    AssistantVoiceState.LISTENING -> 4000
                    AssistantVoiceState.RESPONDING -> 3000
                    AssistantVoiceState.IDLE -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbRotation"
    )

    // Pulsing breathing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = when (state) {
            AssistantVoiceState.LISTENING -> 1.25f
            AssistantVoiceState.RESPONDING -> 1.18f
            AssistantVoiceState.THINKING -> 1.10f
            AssistantVoiceState.IDLE -> 1.02f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantVoiceState.LISTENING -> 600
                    AssistantVoiceState.RESPONDING -> 750
                    AssistantVoiceState.THINKING -> 500
                    AssistantVoiceState.IDLE -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    // Outer glow aura expansion
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = when (state) {
            AssistantVoiceState.LISTENING -> 0.85f
            AssistantVoiceState.RESPONDING -> 0.75f
            AssistantVoiceState.THINKING -> 0.65f
            AssistantVoiceState.IDLE -> 0.40f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantVoiceState.LISTENING) 700 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbGlow"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() / 2.6f

            // 1. Outermost Ambient Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when (state) {
                            AssistantVoiceState.LISTENING -> Color(0xFF00E5FF).copy(alpha = glowAlpha * 0.6f)
                            AssistantVoiceState.THINKING -> Color(0xFFFFD600).copy(alpha = glowAlpha * 0.6f)
                            AssistantVoiceState.RESPONDING -> Color(0xFFFF007F).copy(alpha = glowAlpha * 0.6f)
                            AssistantVoiceState.IDLE -> Color(0xFF8A2BE2).copy(alpha = glowAlpha * 0.5f)
                        },
                        Color(0xFF7C4DFF).copy(alpha = glowAlpha * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                center = center,
                radius = baseRadius * 1.55f
            )

            // 2. Multi-color Swirling Siri/Gemini Dynamic Energy Orbs
            val rad = Math.toRadians(rotation.toDouble())
            val offset1 = Offset(
                center.x + (cos(rad) * baseRadius * 0.35f).toFloat(),
                center.y + (sin(rad) * baseRadius * 0.35f).toFloat()
            )
            val offset2 = Offset(
                center.x + (cos(rad + Math.PI) * baseRadius * 0.35f).toFloat(),
                center.y + (sin(rad + Math.PI) * baseRadius * 0.35f).toFloat()
            )
            val offset3 = Offset(
                center.x + (sin(rad * 1.5) * baseRadius * 0.30f).toFloat(),
                center.y + (cos(rad * 1.5) * baseRadius * 0.30f).toFloat()
            )

            // Cyan Energy Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00F5FF).copy(alpha = 0.95f),
                        Color(0xFF0088FF).copy(alpha = 0.60f),
                        Color.Transparent
                    ),
                    center = offset1,
                    radius = baseRadius * 0.95f
                ),
                center = offset1,
                radius = baseRadius * 0.95f,
                blendMode = BlendMode.Screen
            )

            // Magenta / Pink Energy Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF007F).copy(alpha = 0.95f),
                        Color(0xFF9900FF).copy(alpha = 0.60f),
                        Color.Transparent
                    ),
                    center = offset2,
                    radius = baseRadius * 0.95f
                ),
                center = offset2,
                radius = baseRadius * 0.95f,
                blendMode = BlendMode.Screen
            )

            // Golden / Purple Shimmer
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD700).copy(alpha = 0.85f),
                        Color(0xFF7B1FA2).copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    center = offset3,
                    radius = baseRadius * 0.75f
                ),
                center = offset3,
                radius = baseRadius * 0.75f,
                blendMode = BlendMode.Screen
            )

            // 3. Specular Crystal Glass Highlights
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                    radius = baseRadius * 0.50f
                ),
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                radius = baseRadius * 0.50f
            )
        }
    }
}
