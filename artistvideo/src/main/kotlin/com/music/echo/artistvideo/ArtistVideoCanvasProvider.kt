package iad1tya.echo.music.artistvideo

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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object ArtistVideoCanvasProvider {
    private const val BASE_URL = "https://artwork-archivetune.koiiverse.cloud/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 6_000
                requestTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
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
        val value: ArtistVideoResponse?,
        val expiresAtMs: Long,
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val ttlMs = 60_000L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null,
        duration: Int? = null,
    ): ArtistVideoResponse? {
        if (song.isBlank() || artist.isBlank()) return null
        val key = cacheKey("sa", song, artist, album.orEmpty(), duration?.toString().orEmpty())
        cache[key]?.let { entry ->
            if (entry.expiresAtMs > System.currentTimeMillis()) return entry.value
            cache.remove(key)
        }

        var value: ArtistVideoResponse? = null

        // Strategy 1: Primary archive tune
        val response =
            runCatching {
                client.get(BASE_URL) {
                    parameter("s", song)
                    parameter("a", artist)
                    if (album != null) parameter("al", album)
                    if (duration != null && duration > 0) parameter("d", duration)
                }
            }.getOrNull()

        if (response?.status == HttpStatusCode.OK) {
            value = runCatching { response.body<ArtistVideoResponse>() }.getOrNull()
        }

        // Strategy 2: Fallback Spotify Canvas proxy
        if (value?.preferredAnimationUrl == null) {
            value = runCatching {
                val query = URLEncoder.encode("$song $artist", StandardCharsets.UTF_8.toString())
                val canvasRes = client.get("https://api.paxsenix.org/spotify/canvas?q=$query") {
                    header("User-Agent", "Mozilla/5.0")
                }
                if (canvasRes.status == HttpStatusCode.OK) {
                    val root = canvasRes.body<JsonObject>()
                    val canvasUrl = root["canvas_url"]?.jsonPrimitive?.contentOrNull
                        ?: root["url"]?.jsonPrimitive?.contentOrNull
                    if (!canvasUrl.isNullOrBlank()) {
                        ArtistVideoResponse(
                            name = song,
                            artist = artist,
                            videoUrl = canvasUrl,
                            animated = canvasUrl
                        )
                    } else null
                } else null
            }.getOrNull()
        }

        cache[key] =
            CacheEntry(
                value = value,
                expiresAtMs = System.currentTimeMillis() + ttlMs,
            )

        return value
    }

    private fun cacheKey(prefix: String, vararg parts: String): String {
        val normalized =
            parts
                .map { it.trim().lowercase(Locale.ROOT) }
                .joinToString("|")
        return "$prefix|$normalized"
    }
}

@Serializable
data class ArtistVideoResponse(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}
