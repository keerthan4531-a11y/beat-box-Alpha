package iad1tya.echo.music.echomusiccanvas

import iad1tya.echo.music.canvas.CanvasArtwork
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class echomusicCanvasManifest(
    val items: List<echomusicCanvasItem> = emptyList()
)

@Serializable
data class echomusicCanvasItem(
    val song: String,
    val artist: String,
    val url: String
)

object echomusicCanvasProvider {
    private val MANIFEST_URLS = listOf(
        "https://raw.githubusercontent.com/iad1tya/Echo-Music-Canvas/main/canvas.json",
        "https://cdn.jsdelivr.net/gh/iad1tya/Echo-Music-Canvas@main/canvas.json"
    )

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
                requestTimeoutMillis = 8_000
                socketTimeoutMillis = 8_000
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
        val value: echomusicCanvasManifest?,
        val expiresAtMs: Long,
    )

    private var manifestCache: CacheEntry? = null
    // Cache TTL 1 hour for remote manifest
    private val ttlMs = 3600_000L

    private suspend fun fetchManifest(): echomusicCanvasManifest {
        val currentCache = manifestCache
        if (currentCache != null && currentCache.expiresAtMs > System.currentTimeMillis() && currentCache.value != null) {
            return currentCache.value
        }

        for (url in MANIFEST_URLS) {
            try {
                val manifest: echomusicCanvasManifest = client.get(url).body()
                if (manifest.items.isNotEmpty()) {
                    manifestCache = CacheEntry(manifest, System.currentTimeMillis() + ttlMs)
                    return manifest
                }
            } catch (_: Exception) {
                // Try next manifest URL
            }
        }

        return echomusicCanvasManifest(emptyList())
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String
    ): CanvasArtwork? {
        if (song.isBlank() || artist.isBlank()) return null

        val manifest = fetchManifest()
        val match = manifest.items.firstOrNull { item ->
            (item.song.contains(song, ignoreCase = true) || song.contains(item.song, ignoreCase = true)) &&
            (item.artist.contains(artist, ignoreCase = true) || artist.contains(item.artist, ignoreCase = true)) &&
            item.url.isNotBlank() && item.url.startsWith("http") && !item.url.contains("...")
        }

        return match?.let {
            CanvasArtwork(
                name = it.song,
                artist = it.artist,
                videoUrl = it.url,
                animated = it.url
            )
        }
    }
}
