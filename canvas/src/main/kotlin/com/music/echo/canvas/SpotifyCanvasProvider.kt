package iad1tya.echo.music.canvas

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Spotify Canvas Provider
 *
 * Fetches short looping silent video files (Spotify Canvas) for tracks.
 * Resolves song and artist metadata to Spotify Canvas MP4 streams (canvaz.scdn.co).
 */
object SpotifyCanvasProvider {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 15_000
                socketTimeoutMillis = 15_000
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            install(HttpCache)
            expectSuccess = false
        }
    }

    private data class CacheEntry(
        val value: CanvasArtwork?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
    ): CanvasArtwork? {
        if (song.isBlank() || artist.isBlank()) return null
        val key = cacheKey("spotify_canvas", song, artist, album.orEmpty())
        cache[key]?.takeIf { it.expiresAtMs > System.currentTimeMillis() }?.let { return it.value }

        val cleanSong = cleanTitle(song)
        val cleanArtist = cleanArtist(artist)

        val result = fetchCanvas(cleanSong, cleanArtist, album)
            ?: if (cleanSong != song || cleanArtist != artist) fetchCanvas(song, artist, album) else null

        if (result != null) {
            cache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    private suspend fun fetchCanvas(
        song: String,
        artist: String,
        album: String?,
    ): CanvasArtwork? {
        return runCatching {
            // Strategy 1: Our Cloudflare Worker (Spotify Pathfinder v2 GraphQL)
            // Strategy 2: Fallback to community resolvers (Paxsenix etc.)
            val query = URLEncoder.encode("$song $artist", StandardCharsets.UTF_8.toString())
            val endpoints = listOf(
                "https://spotify-canvas.haruyhari930.workers.dev?q=$query",
                "https://api.paxsenix.org/spotify/canvas?q=$query",
                "https://canvas-api.vercel.app/canvas?query=$query",
                "https://sp-canvas-api.onrender.com/canvas?song=$query"
            )

            for (endpoint in endpoints) {
                val artwork = tryEndpoint(endpoint, song, artist, album)
                if (artwork != null && !artwork.preferredAnimationUrl.isNullOrBlank()) {
                    return@runCatching artwork
                }
            }

            null
        }.getOrNull()
    }

    private suspend fun tryEndpoint(
        url: String,
        song: String,
        artist: String,
        album: String?,
    ): CanvasArtwork? {
        return runCatching {
            val response = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Accept", "application/json")
            }

            if (response.status != HttpStatusCode.OK) return@runCatching null
            val root = response.body<JsonObject>()

            val canvasUrl = root["canvas_url"]?.jsonPrimitive?.contentOrNull
                ?: root["canvasUrl"]?.jsonPrimitive?.contentOrNull
                ?: root["url"]?.jsonPrimitive?.contentOrNull
                ?: root["video_url"]?.jsonPrimitive?.contentOrNull
                ?: root["videoUrl"]?.jsonPrimitive?.contentOrNull
                ?: root["canvases"]?.jsonArray?.firstOrNull()?.jsonObject?.get("canvas_url")?.jsonPrimitive?.contentOrNull

            if (!canvasUrl.isNullOrBlank()) {
                val trackName = root["track_name"]?.jsonPrimitive?.contentOrNull
                    ?: root["trackName"]?.jsonPrimitive?.contentOrNull
                    ?: root["name"]?.jsonPrimitive?.contentOrNull
                    ?: song
                val artistName = root["artist_name"]?.jsonPrimitive?.contentOrNull
                    ?: root["artistName"]?.jsonPrimitive?.contentOrNull
                    ?: root["artist"]?.jsonPrimitive?.contentOrNull
                    ?: artist

                return@runCatching CanvasArtwork(
                    name = trackName,
                    artist = artistName,
                    albumName = album,
                    videoUrl = canvasUrl,
                    animated = canvasUrl
                )
            }
            null
        }.getOrNull()
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("""\s*\(.*?(from|official|video|audio|lyrics|visualizer|remix|version|theme).*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[.*?(from|official|video|audio|lyrics|visualizer|remix|version|theme).*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(from|official|video|audio|lyrics|visualizer).*$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        val delimiters = Regex("""(?:\s*,\s*|\s*&\s*|\s+×\s+|\s+x\s+|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b|\bwith\b)""", RegexOption.IGNORE_CASE)
        return artist.split(delimiters).firstOrNull()?.trim() ?: artist.trim()
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        return "$prefix|" + parts.joinToString("|") { it.trim().lowercase(Locale.ROOT) }
    }
}
