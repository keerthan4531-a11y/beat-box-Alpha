package iad1tya.echo.music.ui.player.futuristic

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Neomorphic Turntable Disc with Multi-Layer Bezel, Circular Progress Track,
 * Rotating Center Album Art, and Timestamp Indicator.
 */
@Composable
fun FuturisticTurntableDisc(
    thumbnailUrl: String?,
    isPlaying: Boolean,
    progress: Float, // 0.0f to 1.0f
    durationString: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    discSize: Dp = 310.dp
) {
    val context = LocalContext.current

    // Rotation animation for the center vinyl album art
    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscRotation"
    )

    var currentAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            currentAngle = rotationAngle
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val effectiveProgress = if (isDragging) dragProgress else progress

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(discSize)
            .aspectRatio(1f)
    ) {
        // Multi-Layer Neomorphic Metallic Rim & Circular Progress Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angleRad = atan2(offset.y - center.y, offset.x - center.x)
                            var deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                            if (deg < 0) deg += 360f
                            dragProgress = (deg / 360f).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angleRad = atan2(change.position.y - center.y, change.position.x - center.x)
                            var deg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                            if (deg < 0) deg += 360f
                            dragProgress = (deg / 360f).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek(dragProgress)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val canvasCenter = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f - 8.dp.toPx()
            val bezelThickness = 38.dp.toPx()
            val innerDiscRadius = outerRadius - bezelThickness

            // 1. Draw Outer Physical Ambient Drop Shadow
            drawContext.canvas.nativeCanvas.apply {
                val shadowPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    setShadowLayer(
                        36.dp.toPx(),
                        0f,
                        16.dp.toPx(),
                        android.graphics.Color.argb(190, 0, 0, 0)
                    )
                    isAntiAlias = true
                }
                drawCircle(canvasCenter.x, canvasCenter.y, outerRadius, shadowPaint)
            }

            // 2. Beveled Dark Metallic Rim (Outer Base)
            val metallicRimBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF2C3038),
                    Color(0xFF131519),
                    Color(0xFF383E48),
                    Color(0xFF181A20),
                    Color(0xFF2C3038)
                ),
                center = canvasCenter
            )
            drawCircle(
                brush = metallicRimBrush,
                radius = outerRadius,
                center = canvasCenter
            )

            // 3. Outer Chamfer Highlight Border
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF5A6272).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(canvasCenter.x - outerRadius * 0.4f, canvasCenter.y - outerRadius * 0.4f),
                    radius = outerRadius * 1.2f
                ),
                radius = outerRadius,
                center = canvasCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Recessed Inner Shadow for Deep Vinyl Groove
            val grooveGradient = Brush.radialGradient(
                colors = listOf(Color(0xFF0B0C0E), Color(0xFF1E2128)),
                center = canvasCenter,
                radius = outerRadius
            )
            drawCircle(
                brush = grooveGradient,
                radius = outerRadius - 4.dp.toPx(),
                center = canvasCenter
            )

            // 5. Circular Track Background (Subtle Track Guide)
            val trackRadius = outerRadius - 14.dp.toPx()
            val trackStrokeWidth = 3.5.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = trackRadius,
                center = canvasCenter,
                style = Stroke(width = trackStrokeWidth)
            )

            // 6. Circular Active Progress Sweep Arc
            val sweepAngle = effectiveProgress * 360f
            if (sweepAngle > 0f) {
                val progressBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF60A5FA),
                        Color(0xFF38BDF8),
                        Color(0xFFE0F2FE),
                        Color(0xFF60A5FA)
                    ),
                    center = canvasCenter
                )
                drawArc(
                    brush = progressBrush,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(canvasCenter.x - trackRadius, canvasCenter.y - trackRadius),
                    size = Size(trackRadius * 2f, trackRadius * 2f),
                    style = Stroke(width = trackStrokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
                )

                // Glowing Scrubber Head Node
                val headAngleRad = Math.toRadians((sweepAngle - 90.0)).toFloat()
                val headX = canvasCenter.x + trackRadius * cos(headAngleRad)
                val headY = canvasCenter.y + trackRadius * sin(headAngleRad)

                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(headX, headY)
                )
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                    radius = 9.dp.toPx(),
                    center = Offset(headX, headY)
                )
            }

            // 7. Inner Vinyl Bezel Ring Border
            drawCircle(
                color = Color(0xFF0F1014),
                radius = innerDiscRadius + 3.dp.toPx(),
                center = canvasCenter
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF4B5563), Color(0xFF111827)),
                    start = Offset(canvasCenter.x, canvasCenter.y - innerDiscRadius),
                    end = Offset(canvasCenter.x, canvasCenter.y + innerDiscRadius)
                ),
                radius = innerDiscRadius + 1.5.dp.toPx(),
                center = canvasCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 8. Rotating Circular Vinyl Center Album Artwork
        val innerArtSize = discSize - 84.dp
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(innerArtSize)
                .clip(CircleShape)
                .rotate(if (isPlaying) rotationAngle else 0f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Art Disc",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Vinyl Texture Specular Sheen Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f

                // Gloss Reflection Arcs
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    startAngle = 200f,
                    sweepAngle = 60f,
                    useCenter = true
                )
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        start = Offset(size.width, size.height),
                        end = Offset(0f, 0f)
                    ),
                    startAngle = 20f,
                    sweepAngle = 60f,
                    useCenter = true
                )

                // Vinyl Spindle Hole in Center
                drawCircle(
                    color = Color(0xFF15181E),
                    radius = 12.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color(0xFF374151),
                    radius = 12.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = Color.Black,
                    radius = 4.dp.toPx(),
                    center = center
                )
            }
        }

        // 9. Duration Timestamp Tag at Bottom Center of Disc
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = durationString,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
