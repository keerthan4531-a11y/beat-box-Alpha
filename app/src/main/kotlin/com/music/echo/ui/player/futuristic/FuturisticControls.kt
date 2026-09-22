package iad1tya.echo.music.ui.player.futuristic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import iad1tya.echo.music.R

/**
 * Minimalist Futuristic Controls matching the reference video:
 * [Shuffle] [Previous] [White Pill Play/Pause] [Next] [Repeat]
 */
@Composable
fun FuturisticPlaybackControls(
    isPlaying: Boolean,
    isShuffle: Boolean,
    isRepeat: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Shuffle Button
        IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.shuffle),
                contentDescription = "Shuffle",
                tint = if (isShuffle) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Previous Button
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Center White Rounded Pill Play/Pause Button
        val playPauseInteractionSource = remember { MutableInteractionSource() }
        val isPressed by playPauseInteractionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.92f else 1.0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "pillScale"
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(pressScale)
                .width(62.dp)
                .height(62.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.White.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = playPauseInteractionSource,
                    indication = null,
                    onClick = onTogglePlayPause
                )
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.pause else R.drawable.play
                ),
                contentDescription = "Play/Pause",
                tint = Color(0xFF0F1218),
                modifier = Modifier.size(28.dp)
            )
        }

        // Next Button
        IconButton(
            onClick = onNext,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Repeat Button
        IconButton(
            onClick = onToggleRepeat,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.repeat),
                contentDescription = "Repeat",
                tint = if (isRepeat) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Bottom Glass Capsule displaying "Next Songs" preview.
 */
@Composable
fun FuturisticNextSongCard(
    nextSongTitle: String?,
    nextSongArtist: String?,
    nextSongThumbnail: String?,
    durationString: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Next Songs",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(nextSongThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nextSongTitle ?: "No upcoming songs",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!nextSongArtist.isNullOrBlank()) {
                    Text(
                        text = nextSongArtist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Duration
            if (!durationString.isNullOrBlank()) {
                Text(
                    text = durationString,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
