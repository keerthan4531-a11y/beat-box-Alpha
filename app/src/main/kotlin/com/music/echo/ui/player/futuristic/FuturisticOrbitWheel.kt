package iad1tya.echo.music.ui.player.futuristic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Data class for items in the orbit wheel.
 */
data class SimilarSongItem(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationString: String = ""
)

/**
 * Premium 3D Curved Orbital Wheel Carousel for "Similar" recommendations.
 *
 * Technical Implementation:
 * - Items travel along a trigonometric circular arc (crescent path)
 * - Arc center is placed far to the right of screen, creating a deep inward curve
 * - Each item's (x, y) is computed as: x = cx - r*cos(θ), y = cy + r*sin(θ)
 * - Dynamic 3D transforms: scale, alpha, rotationX, rotationY based on distance from center
 * - Physics-based fling with spring snap-to-nearest-item
 */
@Composable
fun FuturisticSimilarOrbitWheel(
    similarSongs: List<SimilarSongItem>,
    currentSong: MediaMetadata?,
    onSongSelected: (SimilarSongItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Scroll state: offset in "item units" (0 = first item centered)
    val scrollOffset = remember { Animatable(0f) }
    // Angular spacing between items on the arc (in radians)
    val angularSpacing = 0.18f // ~10.3 degrees between items

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val screenCenterY = screenHeightPx / 2f

        // =============================================================
        // ARC GEOMETRY: Virtual circle whose crescent creates the path
        // =============================================================
        // Place the arc center far to the RIGHT of screen → items curve inward (left)
        val arcCenterX = screenWidthPx * 1.6f
        val arcCenterY = screenCenterY
        val arcRadius = screenWidthPx * 1.35f

        // The "base angle" where the center item sits on the circle
        // PI (180°) = directly left of arc center = roughly center-screen
        val baseAngle = PI.toFloat()

        // Item visual size
        val itemSizeDp = 78.dp
        val itemSizePx = with(density) { itemSizeDp.toPx() }

        // =============================================================
        // GESTURE HANDLING: Vertical drag with physics fling + snap
        // =============================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(similarSongs.size) {
                    if (similarSongs.isEmpty()) return@pointerInput
                    detectVerticalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                // Snap to nearest integer index
                                val maxIdx = (similarSongs.size - 1).coerceAtLeast(0)
                                val targetIdx = scrollOffset.value
                                    .roundToInt()
                                    .coerceIn(0, maxIdx)
                                scrollOffset.animateTo(
                                    targetValue = targetIdx.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                // Convert drag pixels to item-unit offset
                                // Negative drag (up) = scroll forward (higher index)
                                val sensitivity = 130f // pixels per item
                                val delta = -dragAmount / sensitivity
                                val newOffset = scrollOffset.value + delta
                                val maxOffset = (similarSongs.size - 1).toFloat()
                                // Allow slight overscroll for rubber-band feel
                                scrollOffset.snapTo(
                                    newOffset.coerceIn(-0.5f, maxOffset + 0.5f)
                                )
                            }
                        }
                    )
                }
        ) {
            // =============================================================
            // 1. SUBTLE ARC GUIDE PATH (visual crescent line)
            // =============================================================
            Canvas(modifier = Modifier.fillMaxSize()) {
                val guideArcCenter = Offset(arcCenterX, arcCenterY)
                // Draw a subtle arc from top to bottom
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.01f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.01f)
                        )
                    ),
                    startAngle = 140f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(
                        guideArcCenter.x - arcRadius,
                        guideArcCenter.y - arcRadius
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        arcRadius * 2f,
                        arcRadius * 2f
                    ),
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // =============================================================
            // 2. RENDER ORBIT ITEMS along the circular arc
            // =============================================================
            val currentCenterIndex = scrollOffset.value

            similarSongs.forEachIndexed { index, song ->
                // Distance from center (in item-units, fractional)
                val distFromCenter = index.toFloat() - currentCenterIndex

                // Only render items within visible arc range
                if (abs(distFromCenter) > 5f) return@forEachIndexed

                // ---- TRIGONOMETRIC ARC POSITION ----
                // Each item sits at baseAngle + (distFromCenter * angularSpacing)
                val itemAngle = baseAngle + (distFromCenter * angularSpacing)

                // Parametric circle equation:
                // x = cx - r * cos(θ)  (subtract because we want items on LEFT side)
                // y = cy - r * sin(θ)  (standard circle)
                val itemX = arcCenterX - arcRadius * cos(itemAngle)
                val itemY = arcCenterY - arcRadius * sin(itemAngle)

                // ---- DYNAMIC 3D TRANSFORMS ----
                val absDist = abs(distFromCenter)
                // Normalized distance [0, 1] where 0 = center, 1 = 4+ items away
                val normalizedDist = (absDist / 4f).coerceIn(0f, 1f)

                // Scale: 1.0 at center → 0.45 at edges
                val itemScale = 1.0f - (normalizedDist * 0.55f)
                // Alpha: 1.0 at center → 0.15 at edges
                val itemAlpha = 1.0f - (normalizedDist * 0.85f)
                // 3D Rotation Y: items tilt away from viewer at edges
                val rotY = normalizedDist * -30f
                // 3D Rotation X: items above center tilt down, below tilt up
                val rotX = normalizedDist * 20f * sign(distFromCenter)

                if (itemAlpha > 0.08f) {
                    val isCenter = absDist < 0.45f

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (itemX - (itemSizePx * itemScale) / 2f).roundToInt(),
                                    (itemY - (itemSizePx * itemScale) / 2f).roundToInt()
                                )
                            }
                            .size(itemSizeDp)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemAlpha
                                rotationY = rotY
                                rotationX = rotX
                                // Add camera distance for better 3D perspective
                                cameraDistance = 12f * density.density
                            }
                            .shadow(
                                elevation = if (isCenter) 24.dp else 6.dp,
                                shape = CircleShape,
                                spotColor = if (isCenter)
                                    Color(0xFF60A5FA).copy(alpha = 0.4f)
                                else
                                    Color.Black.copy(alpha = 0.6f)
                            )
                            .clip(CircleShape)
                            .border(
                                width = if (isCenter) 2.5.dp else 1.dp,
                                color = if (isCenter)
                                    Color(0xFF93C5FD)
                                else
                                    Color.White.copy(alpha = 0.20f),
                                shape = CircleShape
                            )
                            .clickable {
                                if (isCenter) {
                                    onSongSelected(song)
                                } else {
                                    // Scroll to this item
                                    coroutineScope.launch {
                                        scrollOffset.animateTo(
                                            targetValue = index.toFloat(),
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(song.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Song title label for the center item
                    if (isCenter) {
                        val labelX = itemX + itemSizePx * 0.7f
                        val labelY = itemY - itemSizePx * 0.1f

                        Column(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        labelX.roundToInt(),
                                        labelY.roundToInt()
                                    )
                                }
                                .width(140.dp)
                                .graphicsLayer {
                                    alpha = (1f - absDist * 2f).coerceIn(0f, 1f)
                                }
                        ) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.artist,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // =============================================================
            // 3. HEADER: "Similar" title & Close button
            // =============================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp)
            ) {
                // "Similar" hero label on left
                Text(
                    text = "Similar",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 8.dp)
                )

                // Close button (top right)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
