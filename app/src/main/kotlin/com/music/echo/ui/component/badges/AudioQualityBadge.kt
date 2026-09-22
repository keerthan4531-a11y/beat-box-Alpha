package iad1tya.echo.music.ui.component.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.ui.theme.ColorPalette2026
import iad1tya.echo.music.ui.theme.isAppInDarkTheme

/**
 * Audiophile Quality & Verified Metadata Badges
 * Apple Music Master & Tidal Hi-Fi Studio Style
 */
@Composable
fun AudioQualityBadge(
    text: String,
    accentColor: Color = ColorPalette2026.HiResCyan,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val bgColor = if (isDark) {
        accentColor.copy(alpha = 0.12f)
    } else {
        accentColor.copy(alpha = 0.08f)
    }
    val borderColor = if (isDark) {
        accentColor.copy(alpha = 0.35f)
    } else {
        accentColor.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(width = 0.75.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accentColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
fun HiResBadge(modifier: Modifier = Modifier) {
    AudioQualityBadge(
        text = "24-BIT / 192 kHz",
        accentColor = ColorPalette2026.HiResCyan,
        modifier = modifier
    )
}

@Composable
fun LosslessBadge(modifier: Modifier = Modifier) {
    AudioQualityBadge(
        text = "LOSSLESS",
        accentColor = ColorPalette2026.LosslessEmerald,
        modifier = modifier
    )
}

@Composable
fun DolbyAtmosBadge(modifier: Modifier = Modifier) {
    AudioQualityBadge(
        text = "DOLBY ATMOS",
        accentColor = ColorPalette2026.DolbyAtmosPurple,
        modifier = modifier
    )
}

@Composable
fun VerifiedArtistBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(ColorPalette2026.VerifiedBlue, Color(0xFF0284C7))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "Verified Artist",
            tint = Color.White,
            modifier = Modifier.size(9.dp)
        )
    }
}

@Composable
fun TrackQualityRow(
    modifier: Modifier = Modifier,
    isLossless: Boolean = true,
    isHiRes: Boolean = false,
    hasDolby: Boolean = false,
    bitrate: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        if (isHiRes) {
            HiResBadge()
        } else if (isLossless) {
            LosslessBadge()
        }
        if (hasDolby) {
            DolbyAtmosBadge()
        }
        if (!bitrate.isNullOrBlank()) {
            AudioQualityBadge(
                text = bitrate,
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
