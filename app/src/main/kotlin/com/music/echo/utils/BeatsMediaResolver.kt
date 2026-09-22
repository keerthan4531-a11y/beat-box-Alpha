package iad1tya.echo.music.utils

import com.music.innertube.NewPipeExtractor
import iad1tya.echo.music.artistvideo.ArtistVideoCanvasProvider
import iad1tya.echo.music.models.BeatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object BeatsMediaResolver {
    private const val TAG = "BeatsMediaResolver"

    suspend fun resolveMedia(
        id: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        durationSeconds: Int
    ): BeatItem = withContext(Dispatchers.IO) {
        var videoUrl: String? = null

        // 1. Try Spotify Canvas vertical video (Ultra crisp 9:16 portrait video)
        try {
            val canvas = ArtistVideoCanvasProvider.getBySongArtist(title, artist)
            val canvasUrl = canvas?.preferredAnimationUrl
            if (!canvasUrl.isNullOrBlank()) {
                videoUrl = canvasUrl
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error fetching canvas for $title")
        }

        // 2. If no canvas, fallback to YouTube video streams via NewPipeExtractor
        if (videoUrl == null) {
            try {
                val streams: List<Pair<Int, String>> = NewPipeExtractor.newPipePlayer(id)
                val directVideo = streams.firstOrNull { pair: Pair<Int, String> -> 
                    pair.second.contains("mime=video") || pair.second.contains("googlevideo.com") 
                }?.second
                if (!directVideo.isNullOrBlank()) {
                    videoUrl = directVideo
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error fetching NewPipe video stream for $id")
            }
        }

        // 3. Calculate intelligent Peak Hook timestamp (standard chorus starts at 25%-35% into song)
        val calculatedPeakMs = when {
            durationSeconds > 60 -> (durationSeconds * 0.28f * 1000).toLong()
            durationSeconds > 30 -> 10_000L
            else -> 0L
        }

        BeatItem(
            id = id,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl,
            durationSeconds = durationSeconds,
            peakStartMs = calculatedPeakMs
        )
    }
}
