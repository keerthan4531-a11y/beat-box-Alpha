package iad1tya.echo.music.ui.component.pills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iad1tya.echo.music.ui.theme.ColorPalette2026
import iad1tya.echo.music.ui.theme.isAppInDarkTheme

data class MoodPillItem(
    val id: String,
    val title: String,
    val emoji: String,
    val query: String
)

val DefaultMoodPills = listOf(
    MoodPillItem("tamil", "Trending Tamil", "🔥", "Trending Tamil Songs"),
    MoodPillItem("latenight", "Late Night Vibes", "🌙", "Late Night Chill Songs"),
    MoodPillItem("highenergy", "High Energy", "⚡", "Party Dance Songs"),
    MoodPillItem("focus", "Focus & Study", "🧘", "Acoustic Lofi Calm"),
    MoodPillItem("acoustic", "Acoustic Hits", "🎸", "Acoustic Guitar Songs"),
    MoodPillItem("romance", "Romantic Melody", "💖", "Love Melody Songs")
)

/**
 * Frosted Capsule Mood Filter Pills
 * Apple iOS 26 Glassmorphic Category Filter Row
 */
@Composable
fun MoodFilterPills(
    selectedMoodId: String?,
    onMoodSelected: (MoodPillItem) -> Unit,
    modifier: Modifier = Modifier,
    moods: List<MoodPillItem> = DefaultMoodPills
) {
    val isDark = isAppInDarkTheme()
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moods.forEach { mood ->
            val isSelected = mood.id == selectedMoodId

            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                if (isDark) ColorPalette2026.SlateSurface else ColorPalette2026.IvoryCard
            }

            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            val borderColor = if (isSelected) {
                Color.Transparent
            } else {
                if (isDark) ColorPalette2026.SlateBorder else ColorPalette2026.PorcelainBorder
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
                    .clickable { onMoodSelected(mood) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = mood.emoji, fontSize = 13.sp)
                    Text(
                        text = mood.title,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
