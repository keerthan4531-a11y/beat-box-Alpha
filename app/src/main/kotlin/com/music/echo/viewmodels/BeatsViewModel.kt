package iad1tya.echo.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import dagger.hilt.android.lifecycle.HiltViewModel
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.models.BeatItem
import iad1tya.echo.music.utils.BeatsMediaResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BeatsViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {

    private val _beats = MutableStateFlow<List<BeatItem>>(emptyList())
    val beats: StateFlow<List<BeatItem>> = _beats.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _likedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSongIds: StateFlow<Set<String>> = _likedSongIds.asStateFlow()

    init {
        loadBeatsFeed()
        observeLikedSongs()
    }

    private fun observeLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            database.likedSongsByCreateDateAsc().collect { songs ->
                _likedSongIds.value = songs.map { it.id }.toSet()
            }
        }
    }

    fun loadBeatsFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val collectedSongs = mutableListOf<SongItem>()

                // 1. Fetch Trending Tamil hits queries
                val tamilQueries = listOf(
                    "Tamil Trending Songs",
                    "Anirudh Ravichander Top Hits",
                    "A.R. Rahman Tamil Hits",
                    "Yuvan Shankar Raja Hits",
                    "Latest Tamil Songs 2026",
                    "Harris Jayaraj Hits",
                    "Sid Sriram Tamil Hits"
                )

                // 2. Also fetch global hits
                val globalQueries = listOf(
                    "Billboard Hot 100",
                    "Global Pop Hits",
                    "Trending Music Worldwide"
                )

                val queriesToRun = tamilQueries.shuffled().take(4) + globalQueries.shuffled().take(2)

                for (query in queriesToRun) {
                    val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    val songs = searchResult?.items?.filterIsInstance<SongItem>() ?: emptyList()
                    collectedSongs.addAll(songs.take(6))
                }

                // Remove duplicates while maintaining order
                val uniqueSongs = collectedSongs.distinctBy { it.id }

                // Map to BeatItem with initial metadata
                val initialBeatItems = uniqueSongs.map { song ->
                    BeatItem(
                        id = song.id,
                        title = song.title,
                        artist = song.artists.joinToString(", ") { it.name },
                        thumbnailUrl = song.thumbnail,
                        durationSeconds = song.duration ?: 180,
                        peakStartMs = (song.duration ?: 180).let { if (it > 40) (it * 0.28f * 1000).toLong() else 0L }
                    )
                }

                _beats.value = initialBeatItems
                _isLoading.value = false

                // Pre-resolve media for first 5 items in background
                initialBeatItems.take(5).forEachIndexed { index, item ->
                    resolveItemMedia(index, item)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading Beats feed")
                _isLoading.value = false
            }
        }
    }

    fun onPageChanged(newIndex: Int) {
        val currentList = _beats.value
        // Resolve upcoming 3 items
        for (i in newIndex..(newIndex + 3)) {
            if (i in currentList.indices) {
                val item = currentList[i]
                if (item.videoUrl == null) {
                    resolveItemMedia(i, item)
                }
            }
        }
    }

    private fun resolveItemMedia(index: Int, item: BeatItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolved = BeatsMediaResolver.resolveMedia(
                    id = item.id,
                    title = item.title,
                    artist = item.artist,
                    thumbnailUrl = item.thumbnailUrl,
                    durationSeconds = item.durationSeconds
                )
                val current = _beats.value.toMutableList()
                if (index in current.indices && current[index].id == item.id) {
                    current[index] = resolved
                    _beats.value = current
                }
            } catch (e: Exception) {
                Timber.w(e, "Error resolving media for ${item.title}")
            }
        }
    }

    fun toggleLike(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isCurrentlyLiked = _likedSongIds.value.contains(songId)
            val song = database.song(songId).first()
            if (song != null) {
                database.query {
                    update(song.song.toggleLike())
                }
            }
        }
    }
}
