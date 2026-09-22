package iad1tya.echo.music.ui.screens.equalizer.axion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.ui.theme.isAppInDarkTheme
import iad1tya.echo.music.ui.theme.neomorphicCard
import kotlin.math.*

/**
 * 3D Tactile Neumorphism Studio Equalizer Rotary Knob
 * Brushed metal/porcelain 3D dial with glowing circular LED arc meters & smooth gesture rotation.
 */
@Composable
fun StudioRotaryKnob(
    label: String,
    value: Float, // Range: -10f to +10f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val isDark = isAppInDarkTheme()
    val minVal = -10f
    val maxVal = 10f
    val sweepAngleTotal = 270f
    val startAngle = 135f // 135 to 405 (bottom gap)

    val normalized = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
    val animatedAngle by animateFloatAsState(
        targetValue = startAngle + normalized * sweepAngleTotal,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 600f),
        label = "knobAngle"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .neomorphicCard(cornerRadius = 24.dp, elevation = 5.dp)
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity: upward/rightward drag increases, downward/leftward decreases
                        val delta = (-dragAmount.y + dragAmount.x) * 0.12f
                        val newVal = (value + delta).coerceIn(minVal, maxVal)
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.width / 2f - 6.dp.toPx()
                val knobRadius = outerRadius - 12.dp.toPx()

                // 1. Inactive LED Track Arc
                drawArc(
                    color = if (isDark) Color(0xFF232838) else Color(0xFFDDE3EC),
                    startAngle = startAngle,
                    sweepAngle = sweepAngleTotal,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 2. Active Glowing LED Arc
                val activeSweep = normalized * sweepAngleTotal
                if (activeSweep > 0) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.5f),
                                accentColor
                            )
                        ),
                        startAngle = startAngle,
                        sweepAngle = activeSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // 3. 3D Convex Metallic / Porcelain Dial Body
                val knobBodyBrush = if (isDark) {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2E3D), Color(0xFF161922)),
                        center = Offset(center.x - knobRadius * 0.3f, center.y - knobRadius * 0.3f),
                        radius = knobRadius * 1.5f
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFEDF2F7)),
                        center = Offset(center.x - knobRadius * 0.3f, center.y - knobRadius * 0.3f),
                        radius = knobRadius * 1.5f
                    )
                }

                // Knob Drop Shadow
                drawCircle(
                    color = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF94A3B8).copy(alpha = 0.35f),
                    radius = knobRadius + 1.5.dp.toPx(),
                    center = Offset(center.x + 1.5.dp.toPx(), center.y + 2.dp.toPx())
                )

                // Main Dial
                drawCircle(
                    brush = knobBodyBrush,
                    radius = knobRadius,
                    center = center
                )

                // Specular Refractive Border Rim
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = if (isDark) listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                        else listOf(Color.White, Color(0xFFCBD5E1)),
                        start = Offset(center.x - knobRadius, center.y - knobRadius),
                        end = Offset(center.x + knobRadius, center.y + knobRadius)
                    ),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 4. Tactile Pointer Notch / Indicator
                val angleRad = Math.toRadians(animatedAngle.toDouble())
                val indicatorStartX = center.x + (knobRadius * 0.35f) * cos(angleRad).toFloat()
                val indicatorStartY = center.y + (knobRadius * 0.35f) * sin(angleRad).toFloat()
                val indicatorEndX = center.x + (knobRadius * 0.82f) * cos(angleRad).toFloat()
                val indicatorEndY = center.y + (knobRadius * 0.82f) * sin(angleRad).toFloat()

                drawLine(
                    color = accentColor,
                    start = Offset(indicatorStartX, indicatorStartY),
                    end = Offset(indicatorEndX, indicatorEndY),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val formattedValue = if (value > 0) "+%.1f dB".format(value) else "%.1f dB".format(value)
        Text(
            text = formattedValue,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (value != 0f) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
