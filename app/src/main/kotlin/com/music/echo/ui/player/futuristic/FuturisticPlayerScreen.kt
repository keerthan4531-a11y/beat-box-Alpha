package iad1tya.echo.music.ui.player.futuristic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.models.MediaMetadata
import iad1tya.echo.music.utils.makeTimeString

/**
 * Master Futuristic Player Screen with Shared Element Anchor Transition.
 *
 * Key Behavior:
 * - Main state: Full 3D Turntable disc centered, controls visible, "Next Songs" at bottom
 * - On left swipe: Disc smoothly translates to the LEFT edge (becomes the anchor ring),
 *   controls fade out, Similar orbit wheel fades in on the right side
 * - On right swipe / close: Disc returns to center, controls fade back in
 *
 * This uses a SINGLE layout with animated properties rather than AnimatedContent
 * to achieve the "shared element" feel where the disc never disappears.
 */
@Composable
fun FuturisticPlayerScreen(
    mediaMetadata: MediaMetadata?,
    playbackState: Int,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onCollapse: () -> Unit,
    onShowMenu: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()

    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }

    // ===================================================================
    // STATE: Whether the "Similar" orbit wheel is shown
    // ===================================================================
    var showSimilar by remember { mutableStateOf(false) }

    // Cumulative horizontal drag for triggering the transition
    var horizontalDragAccumulator by remember { mutableFloatStateOf(0f) }

    // ===================================================================
    // ANIMATED VALUES for shared element transition
    // ===================================================================
    // Disc X translation: 0 = centered, negative = shifted left
    val discTranslateX by animateFloatAsState(
        targetValue = if (showSimilar) -(screenWidthPx * 0.34f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "discTranslateX"
    )

    // Disc scale when anchored (slightly smaller to act as ring)
    val discScale by animateFloatAsState(
        targetValue = if (showSimilar) 0.55f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "discScale"
    )

    // Controls alpha (fade out when showing similar)
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showSimilar) 0f else 1f,
        animationSpec = spring(stiffness = 400f),
        label = "controlsAlpha"
    )

    // Carousel alpha (fade in when showing similar)
    val carouselAlpha by animateFloatAsState(
        targetValue = if (showSimilar) 1f else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "carouselAlpha"
    )

    // Derive upcoming next song from queue
    val nextItem = remember(queueWindows) {
        queueWindows.getOrNull(1)?.mediaItem
    }

    // Build similar songs list from queue
    val similarSongs = remember(queueWindows, mediaMetadata) {
        if (queueWindows.isNotEmpty()) {
            queueWindows.mapNotNull { window ->
                val item = window.mediaItem
                SimilarSongItem(
                    id = item.mediaId,
                    title = item.mediaMetadata.title?.toString() ?: "Unknown Title",
                    artist = item.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                    thumbnailUrl = item.mediaMetadata.artworkUri?.toString()
                        ?: mediaMetadata?.thumbnailUrl,
                    durationString = ""
                )
            }
        } else {
            listOf(
                SimilarSongItem("1", mediaMetadata?.title ?: "Current",
                    mediaMetadata?.artists?.firstOrNull()?.name ?: "Artist",
                    mediaMetadata?.thumbnailUrl),
                SimilarSongItem("2", "Arcane", "Ryan Taubert", mediaMetadata?.thumbnailUrl),
                SimilarSongItem("3", "Passage", "Roary", mediaMetadata?.thumbnailUrl),
                SimilarSongItem("4", "Beautiful Now", "Zedd", mediaMetadata?.thumbnailUrl),
                SimilarSongItem("5", "Skin", "Roary", mediaMetadata?.thumbnailUrl)
            )
        }
    }

    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val durationText = if (duration > 0) makeTimeString(duration - position) else "0:00"

    // ===================================================================
    // MAIN LAYOUT: Single composable with animated properties
    // ===================================================================
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07090F),
                        Color(0xFF0F1522),
                        Color(0xFF080A10)
                    )
                )
            )
            .statusBarsPadding()
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    horizontalDragAccumulator += delta
                    if (!showSimilar && horizontalDragAccumulator < -80f) {
                        showSimilar = true
                        horizontalDragAccumulator = 0f
                    } else if (showSimilar && horizontalDragAccumulator > 80f) {
                        showSimilar = false
                        horizontalDragAccumulator = 0f
                    }
                },
                onDragStopped = {
                    horizontalDragAccumulator = 0f
                }
            )
    ) {
        // ===================================================================
        // LAYER 1: Header (always visible)
        // ===================================================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer {
                    // Slight fade when showing similar
                    alpha = 0.5f + (controlsAlpha * 0.5f)
                }
        ) {
            IconButton(onClick = {
                if (showSimilar) {
                    showSimilar = false
                } else {
                    onCollapse()
                }
            }) {
                Icon(
                    painter = painterResource(
                        if (showSimilar) R.drawable.arrow_back else R.drawable.expand_more
                    ),
                    contentDescription = if (showSimilar) "Back" else "Collapse",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                Text(
                    text = mediaMetadata?.title ?: "No Title",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite / Heart Toggle
            IconButton(
                onClick = { playerConnection.toggleLike() },
                modifier = Modifier.graphicsLayer { alpha = controlsAlpha }
            ) {
                Icon(
                    painter = painterResource(
                        if (currentSong?.song?.liked == true)
                            R.drawable.favorite
                        else
                            R.drawable.favorite_border
                    ),
                    contentDescription = "Favorite",
                    tint = if (currentSong?.song?.liked == true)
                        Color(0xFFEF4444)
                    else
                        Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3-Dots Menu
            IconButton(
                onClick = onShowMenu,
                modifier = Modifier.graphicsLayer { alpha = controlsAlpha }
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ===================================================================
        // LAYER 2: Center 3D Disc (SHARED ELEMENT — slides left for Similar)
        // ===================================================================
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = discTranslateX
                    scaleX = discScale
                    scaleY = discScale
                    // When anchored, move slightly up
                    translationY = if (showSimilar) -screenWidthPx * 0.05f else 0f
                }
        ) {
            FuturisticTurntableDisc(
                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                isPlaying = isPlaying,
                progress = progress,
                durationString = durationText,
                onSeek = { seekFraction ->
                    if (duration > 0) {
                        playerConnection.player.seekTo((seekFraction * duration).toLong())
                    }
                }
            )
        }

        // ===================================================================
        // LAYER 3: Controls + Next Song (fade out when Similar is shown)
        // ===================================================================
        if (controlsAlpha > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp)
                    .graphicsLayer { alpha = controlsAlpha }
            ) {
                // Playback Controls
                FuturisticPlaybackControls(
                    isPlaying = isPlaying,
                    isShuffle = playerConnection.player.shuffleModeEnabled,
                    isRepeat = playerConnection.player.repeatMode != Player.REPEAT_MODE_OFF,
                    onTogglePlayPause = { playerConnection.togglePlayPause() },
                    onPrevious = { playerConnection.seekToPrevious() },
                    onNext = { playerConnection.seekToNext() },
                    onToggleShuffle = {
                        playerConnection.player.shuffleModeEnabled =
                            !playerConnection.player.shuffleModeEnabled
                    },
                    onToggleRepeat = {
                        val nextMode = when (playerConnection.player.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        playerConnection.player.repeatMode = nextMode
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "Next Songs" Card
                val nextTitle = nextItem?.mediaMetadata?.title?.toString()
                val nextArtist = nextItem?.mediaMetadata?.artist?.toString()
                val nextArt = nextItem?.mediaMetadata?.artworkUri?.toString()

                FuturisticNextSongCard(
                    nextSongTitle = nextTitle,
                    nextSongArtist = nextArtist,
                    nextSongThumbnail = nextArt,
                    durationString = null,
                    onClick = onOpenQueue
                )
            }
        }

        // ===================================================================
        // LAYER 4: Similar Orbit Wheel (fade in when triggered)
        // ===================================================================
        if (carouselAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = carouselAlpha }
            ) {
                FuturisticSimilarOrbitWheel(
                    similarSongs = similarSongs,
                    currentSong = mediaMetadata,
                    onSongSelected = { selected ->
                        showSimilar = false
                    },
                    onClose = {
                        showSimilar = false
                    }
                )
            }
        }
    }
}
