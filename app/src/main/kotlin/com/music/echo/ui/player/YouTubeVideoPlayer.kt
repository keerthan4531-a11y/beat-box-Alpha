package iad1tya.echo.music.ui.player

import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import iad1tya.echo.music.playback.PlayerConnection
import iad1tya.echo.music.utils.YTPlayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun YouTubeVideoPlayer(
    videoId: String,
    title: String?,
    artist: String?,
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 24f,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    val isPlaying by playerConnection.isPlaying.collectAsState()

    var videoUrl by remember(videoId) { mutableStateOf<String?>(null) }
    var videoQuality by remember(videoId) { mutableStateOf("HD") }
    var isLoading by remember(videoId) { mutableStateOf(true) }
    var isError by remember(videoId) { mutableStateOf(false) }
    var isVideoReady by remember(videoId) { mutableStateOf(false) }
    var videoAspectRatio by remember(videoId) { mutableFloatStateOf(16f / 9f) }
    var showControls by remember { mutableStateOf(true) }

    // Fetch HD Video stream URL
    LaunchedEffect(videoId) {
        isLoading = true
        isError = false
        isVideoReady = false

        val result = withContext(Dispatchers.IO) {
            YTPlayerUtils.getVideoPlaybackUrl(
                videoId = videoId,
                knownTitle = title,
                knownArtist = artist
            )
        }

        result.onSuccess { data ->
            videoUrl = data.videoUrl
            videoQuality = data.quality
            isLoading = false
        }.onFailure {
            isError = true
            isLoading = false
        }
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
    }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)
                val builder = request.newBuilder()
                    .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                    .header("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                    .header("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
                chain.proceed(builder.build())
            }
            .build()
    }

    val mediaSourceFactory = remember(okHttpClient) {
        DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                context,
                OkHttpDataSource.Factory(okHttpClient),
            ),
        )
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setForceHighestSupportedBitrate(true)
                    .build()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false,
                )
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                volume = 0f // Audio plays via main music service to keep seamless background playback
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Sync play/pause with main player
    LaunchedEffect(isPlaying) {
        if (exoPlayer.playWhenReady != isPlaying) {
            exoPlayer.playWhenReady = isPlaying
        }
    }

    // Sync current position when video starts or seeks
    LaunchedEffect(videoUrl) {
        val currentPos = playerConnection.player.currentPosition
        if (currentPos > 0) {
            exoPlayer.seekTo(currentPos)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoReady = true
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUrl) {
        val url = videoUrl ?: return@LaunchedEffect
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(if (url.contains(".m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        val currentPos = playerConnection.player.currentPosition
        if (currentPos > 0) {
            exoPlayer.seekTo(currentPos)
        }
        exoPlayer.playWhenReady = isPlaying
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "videoAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        val isLeftSide = offset.x < size.width / 2f
                        val currentPos = playerConnection.player.currentPosition
                        val duration = playerConnection.player.duration
                        if (isLeftSide) {
                            val newPos = (currentPos - 10000).coerceAtLeast(0)
                            playerConnection.player.seekTo(newPos)
                            exoPlayer.seekTo(newPos)
                        } else {
                            val newPos = (currentPos + 10000).coerceAtMost(duration)
                            playerConnection.player.seekTo(newPos)
                            exoPlayer.seekTo(newPos)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Video View
        AndroidView(
            factory = { viewContext ->
                AspectRatioFrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    val textureView = TextureView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { view ->
                view.setAspectRatio(videoAspectRatio)
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
        )

        // Loading spinner
        if (isLoading || (!isVideoReady && !isError)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Error state
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.clickable {
                        // Retry
                        videoUrl = null
                        isLoading = true
                        isError = false
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Text("Video unavailable, tap to retry", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            ) {
                // Top Bar: Quality & Switch to Song
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = videoQuality,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Switch back to Audio Button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Audio Mode",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Song",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Center Play/Pause Indicator
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clickable {
                            if (playerConnection.player.playWhenReady) {
                                playerConnection.player.pause()
                            } else {
                                playerConnection.player.play()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
