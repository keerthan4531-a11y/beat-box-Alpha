package com.music.echo.data

import iad1tya.echo.music.utils.applyTls13Fix
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class SaavnHomeResponse(
    val status: String? = null,
    val data: SaavnHomeData? = null
)

@Serializable
data class SaavnHomeData(
    val newAlbums: List<SaavnHomeItem>? = null,
    val playlists: List<SaavnHomeItem>? = null,
    val charts: List<SaavnHomeItem>? = null,
    val trending: List<SaavnHomeItem>? = null
)

@Serializable
data class SaavnHomeItem(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val type: String? = null,
    val image: List<SaavnImage>? = null,
    val downloadUrl: List<SaavnDownloadUrl>? = null
) {
    fun toYTItem(): com.music.innertube.models.YTItem {
        val displayName = title ?: name ?: "Unknown"
        val displayThumb = image?.lastOrNull()?.let { it.url ?: it.link } ?: image?.firstOrNull()?.let { it.url ?: it.link } ?: ""
        val isSong = type == "song"
        val isPlaylist = type == "playlist"
        
        return if (isSong) {
            val streamUrl = downloadUrl?.lastOrNull()?.let { it.url ?: it.link } ?: downloadUrl?.firstOrNull()?.let { it.url ?: it.link } ?: ""
            com.music.innertube.models.SongItem(
                id = "saavn_url:$streamUrl",
                title = displayName,
                artists = listOf(com.music.innertube.models.Artist(subtitle ?: "", null)),
                thumbnail = displayThumb,
                explicit = false
            )
        } else if (isPlaylist) {
            com.music.innertube.models.PlaylistItem(
                id = "saavn_playlist:$id",
                title = displayName,
                author = com.music.innertube.models.Artist(subtitle ?: "", null),
                songCountText = null,
                thumbnail = displayThumb,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null
            )
        } else {
            com.music.innertube.models.AlbumItem(
                browseId = "saavn_album:$id",
                playlistId = "saavn_album:$id",
                title = displayName,
                artists = listOf(com.music.innertube.models.Artist(subtitle ?: "", null)),
                thumbnail = displayThumb,
                explicit = false
            )
        }
    }
}

@Serializable
data class SaavnImage(
    val quality: String? = null,
    val url: String? = null,
    val link: String? = null
)

@Serializable
data class SaavnDownloadUrl(
    val quality: String? = null,
    val url: String? = null,
    val link: String? = null
)

class SaavnHomeRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .applyTls13Fix()
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val baseUrl = "https://weathered-heart-9c50.fct197096.workers.dev"

    suspend fun fetchTamilHome(): Result<SaavnHomeData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/homepage?language=tamil")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch Saavn Home"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val parsed = json.decodeFromString<SaavnHomeResponse>(body)
                if (parsed.data != null) {
                    Result.success(parsed.data)
                } else {
                    Result.failure(Exception("No data in response"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSaavnAlbum(id: String): com.music.innertube.pages.AlbumPage? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/albums?id=$id")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val parsed = json.decodeFromString<SaavnListResponse>(body)
                val data = parsed.data ?: return@withContext null
                
                val displayThumb = data.resolvedImage()
                val albumItem = com.music.innertube.models.AlbumItem(
                    browseId = "saavn_album:$id",
                    playlistId = "saavn_album:$id",
                    title = data.resolvedName(),
                    artists = listOf(com.music.innertube.models.Artist(data.primaryArtists ?: "Unknown Artist", null)),
                    year = data.year?.toIntOrNull(),
                    thumbnail = displayThumb,
                    explicit = false
                )
                
                val songs = data.songs?.map { it.toSongItem() } ?: emptyList()
                
                return@withContext com.music.innertube.pages.AlbumPage(
                    album = albumItem,
                    songs = songs,
                    otherVersions = emptyList()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchSaavnPlaylist(id: String): com.music.innertube.pages.PlaylistPage? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/playlists?id=$id")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val parsed = json.decodeFromString<SaavnListResponse>(body)
                val data = parsed.data ?: return@withContext null
                
                val displayThumb = data.resolvedImage()
                val playlistItem = com.music.innertube.models.PlaylistItem(
                    id = "saavn_playlist:$id",
                    title = data.resolvedName(),
                    author = com.music.innertube.models.Artist("JioSaavn", null),
                    songCountText = "${data.songs?.size ?: 0} songs",
                    thumbnail = displayThumb,
                    playEndpoint = null,
                    shuffleEndpoint = null,
                    radioEndpoint = null
                )
                
                val songs = data.songs?.map { it.toSongItem() } ?: emptyList()
                
                return@withContext com.music.innertube.pages.PlaylistPage(
                    playlist = playlistItem,
                    songs = songs,
                    songsContinuation = null,
                    continuation = null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Serializable
data class SaavnListResponse(
    val status: String? = null,
    val data: SaavnListData? = null
)

@Serializable
data class SaavnListData(
    val id: String? = null,
    val listid: String? = null,
    val name: String? = null,
    val listname: String? = null,
    val title: String? = null,
    val year: String? = null,
    val primaryArtists: String? = null,
    val image: kotlinx.serialization.json.JsonElement? = null,
    val songs: List<SaavnDetailSong>? = null
) {
    fun resolvedId(): String = id ?: listid ?: ""
    fun resolvedName(): String = title ?: name ?: listname ?: "Unknown"
    fun resolvedImage(): String {
        if (image == null) return ""
        return when {
            image is kotlinx.serialization.json.JsonPrimitive -> image.content
            image is kotlinx.serialization.json.JsonArray -> {
                val arr = image as kotlinx.serialization.json.JsonArray
                val last = arr.lastOrNull() as? kotlinx.serialization.json.JsonObject
                last?.get("link")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ?: last?.get("url")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ?: ""
            }
            else -> ""
        }
    }
}

@Serializable
data class SaavnDetailSong(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val type: String? = null,
    val subtitle: String? = null,
    val primaryArtists: String? = null,
    val album: String? = null,
    val image: kotlinx.serialization.json.JsonElement? = null,
    val downloadUrl: kotlinx.serialization.json.JsonElement? = null
) {
    fun resolvedImage(): String {
        if (image == null) return ""
        return when {
            image is kotlinx.serialization.json.JsonPrimitive -> image.content
            image is kotlinx.serialization.json.JsonArray -> {
                val arr = image as kotlinx.serialization.json.JsonArray
                val last = arr.lastOrNull() as? kotlinx.serialization.json.JsonObject
                last?.get("link")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ?: last?.get("url")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ?: ""
            }
            else -> ""
        }
    }

    fun resolvedDownloadUrl(): String {
        if (downloadUrl == null) return ""
        return when {
            downloadUrl is kotlinx.serialization.json.JsonPrimitive -> downloadUrl.content
            downloadUrl is kotlinx.serialization.json.JsonArray -> {
                val arr = downloadUrl as kotlinx.serialization.json.JsonArray
                val first = arr.firstOrNull() as? kotlinx.serialization.json.JsonObject
                first?.get("url")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ?: first?.get("link")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                    ?: ""
            }
            else -> ""
        }
    }

    fun toSongItem(): com.music.innertube.models.SongItem {
        val displayName = title ?: name ?: "Unknown"
        val displayThumb = resolvedImage()
        val streamUrl = resolvedDownloadUrl()
        val artistName = primaryArtists ?: subtitle ?: "Unknown Artist"
        return com.music.innertube.models.SongItem(
            id = "saavn_url:$streamUrl",
            title = displayName,
            artists = listOf(com.music.innertube.models.Artist(artistName, null)),
            thumbnail = displayThumb,
            explicit = false
        )
    }
}
