package iad1tya.echo.music.ui.component.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import iad1tya.echo.music.ui.component.badges.LosslessBadge
import iad1tya.echo.music.ui.theme.ColorPalette2026
import iad1tya.echo.music.ui.theme.isAppInDarkTheme

/**
 * 2026 Bento Grid Modular Card Layout
 * 1 Big Hero Card + 2 Quick Mix Square Tiles for High-End Apple/Spotify Aesthetics
 */
@Composable
fun BentoHeroCard(
    title: String,
    subtitle: String,
    badgeText: String,
    thumbnailUrl: String?,
    onPlayClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val borderColor = if (isDark) ColorPalette2026.SlateBorder else ColorPalette2026.PorcelainBorder
    val surfaceColor = if (isDark) ColorPalette2026.SlateSurface else ColorPalette2026.IvoryCard

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Gradient readability overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                LosslessBadge()
            }

            // Bottom Title & Play Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BentoSquareTile(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    iconEmoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val borderColor = if (isDark) ColorPalette2026.SlateBorder else ColorPalette2026.PorcelainBorder
    val surfaceColor = if (isDark) ColorPalette2026.SlateSurface else ColorPalette2026.IvoryCard

    Column(
        modifier = modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(text = iconEmoji, fontSize = 24.sp)
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BentoGridSection(
    heroTitle: String,
    heroSubtitle: String,
    heroThumbnail: String?,
    onHeroPlay: () -> Unit,
    onHeroClick: () -> Unit,
    tile1Title: String,
    tile1Subtitle: String,
    tile1Emoji: String,
    onTile1Click: () -> Unit,
    tile2Title: String,
    tile2Subtitle: String,
    tile2Emoji: String,
    onTile2Click: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BentoHeroCard(
            title = heroTitle,
            subtitle = heroSubtitle,
            badgeText = "Featured Mix",
            thumbnailUrl = heroThumbnail,
            onPlayClick = onHeroPlay,
            onClick = onHeroClick
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoSquareTile(
                title = tile1Title,
                subtitle = tile1Subtitle,
                thumbnailUrl = null,
                iconEmoji = tile1Emoji,
                onClick = onTile1Click,
                modifier = Modifier.weight(1f)
            )
            BentoSquareTile(
                title = tile2Title,
                subtitle = tile2Subtitle,
                thumbnailUrl = null,
                iconEmoji = tile2Emoji,
                onClick = onTile2Click,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
