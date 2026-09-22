package iad1tya.echo.music.ui.screens.beats

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.music.innertube.models.WatchEndpoint
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.models.BeatItem
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.ui.theme.liquidGlass
import iad1tya.echo.music.ui.theme.neomorphicCard
import iad1tya.echo.music.viewmodels.BeatsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun BeatsScreen(
    navController: NavController? = null,
    viewModel: BeatsViewModel = hiltViewModel()
) {
    val beats by viewModel.beats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val likedSongIds by viewModel.likedSongIds.collectAsState()

    if (isLoading && beats.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Beats...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { beats.size })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val beatItem = beats.getOrNull(pageIndex)
            if (beatItem != null) {
                val isPageActive = pagerState.currentPage == pageIndex
                BeatPageItem(
                    item = beatItem,
                    isActive = isPageActive,
                    isLiked = likedSongIds.contains(beatItem.id),
                    onToggleLike = { viewModel.toggleLike(beatItem.id) }
                )
            }
        }

        // Top Header Overlay: "Beats ⚡"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .liquidGlass(cornerRadius = 20.dp, glassAlpha = 0.4f, elevation = 4.dp)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Beats",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun BeatPageItem(
    item: BeatItem,
    isActive: Boolean,
    isLiked: Boolean,
    onToggleLike: () -> Unit
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f
        }
    }

    var isVideoReady by remember { mutableStateOf(false) }
    var progressSeconds by remember { mutableFloatStateOf(0f) }
    val clipDurationSec = 30f

    // Manage Playback lifecycle with Peak timestamp
    LaunchedEffect(item.videoUrl, isActive) {
        if (isActive && !item.videoUrl.isNullOrBlank()) {
            val mediaItem = MediaItem.fromUri(Uri.parse(item.videoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.seekTo(item.peakStartMs)
            exoPlayer.play()
            isVideoReady = true

            // 30s Loop Timer
            while (isActive && isActive) {
                delay(100)
                val currentPos = exoPlayer.currentPosition
                val elapsedSincePeak = (currentPos - item.peakStartMs).coerceAtLeast(0)
                val sec = elapsedSincePeak / 1000f

                if (sec >= clipDurationSec) {
                    exoPlayer.seekTo(item.peakStartMs)
                    progressSeconds = 0f
                } else {
                    progressSeconds = sec
                }
            }
        } else {
            exoPlayer.pause()
            progressSeconds = 0f
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Background (Video or High-Res Artwork)
        if (item.videoUrl != null && isVideoReady) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Dark Vignette Scrim for maximum contrast & luxury readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 3. Right Side Floating Actions (Like, Lyrics, Share, Play Full Song)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Like Action
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .size(48.dp)
                    .liquidGlass(cornerRadius = 24.dp, glassAlpha = 0.35f, elevation = 4.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFEF4444) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Share Action
            IconButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Listen to ${item.title} by ${item.artist} on Beat Box: https://music.youtube.com/watch?v=${item.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Beat"))
                },
                modifier = Modifier
                    .size(48.dp)
                    .liquidGlass(cornerRadius = 24.dp, glassAlpha = 0.35f, elevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // "Play Full Song" Button
            IconButton(
                onClick = {
                    playerConnection?.playQueue(
                        YouTubeQueue(WatchEndpoint(videoId = item.id))
                    )
                },
                modifier = Modifier
                    .size(52.dp)
                    .neomorphicCard(cornerRadius = 26.dp, elevation = 6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play Full Song",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // 4. Bottom-Left Song Details & Peak Badge
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 20.dp, bottom = 40.dp)
        ) {
            // Peak Hook Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Peak Hook • 30s",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.artist,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 30s Smooth Progress Indicator
            LinearProgressIndicator(
                progress = { (progressSeconds / clipDurationSec).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}
