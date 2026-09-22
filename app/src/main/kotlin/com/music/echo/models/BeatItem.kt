package iad1tya.echo.music.models

import androidx.compose.runtime.Immutable

@Immutable
data class BeatItem(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int = 180,
    val peakStartMs: Long = 45000L,
    val isLiked: Boolean = false,
    val lyricsSnippet: String? = null
)
