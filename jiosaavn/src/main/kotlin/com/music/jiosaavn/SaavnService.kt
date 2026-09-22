/**
 * echomusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * JioSaavn audio streaming service via Cloudflare Worker.
 *
 * API endpoints used:
 *   - GET /api/search/songs?query={q}&limit=5
 *   - GET /api/songs/{id}
 */

package com.music.jiosaavn

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import org.json.JSONObject

// ─── Data models ────────────────────────────────────────────────────────────

data class SaavnDownloadUrl(
    val quality: String = "",
    val url: String = ""
)

data class SaavnImage(
    val quality: String = "",
    val url: String = ""
)

data class SaavnSong(
    val id: String = "",
    val name: String = "",
    val duration: Int? = null,
    val explicitContent: Boolean = false,
    val primaryArtists: String = "",
    val image: List<SaavnImage> = emptyList(),
    val downloadUrl: List<SaavnDownloadUrl> = emptyList()
)

// ─── Service ─────────────────────────────────────────────────────────────────

object SaavnService {

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 6_000
                connectTimeoutMillis = 4_000
                socketTimeoutMillis  = 6_000
            }
            defaultRequest {
                headers.append(HttpHeaders.Accept, "application/json")
                headers.append(HttpHeaders.UserAgent, "EchoMusic/1.0")
            }
            expectSuccess = false
        }
    }

    private suspend fun getWithFallback(
        endpoint: String,
        block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}
    ): String {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < 3) {
            try {
                val url = "${DeviceRouter.getCurrentServer()}/api/$endpoint"
                val response = client.get(url, block)
                if (response.status.value in 500..599) {
                    DeviceRouter.fallbackToNextServer()
                    attempt++
                    continue
                }
                return response.bodyAsText()
            } catch (e: Exception) {
                lastException = e
                DeviceRouter.fallbackToNextServer()
                attempt++
            }
        }
        throw lastException ?: IllegalStateException("All Saavn servers failed")
    }

    private fun parseSong(obj: JSONObject): SaavnSong {
        val id = obj.optString("id", "")
        val name = obj.optString("name", obj.optString("title", ""))
        val duration = if (obj.has("duration") && !obj.isNull("duration")) obj.optInt("duration") else null
        val explicit = obj.optBoolean("explicitContent", false)

        val primaryArtists = when {
            obj.has("primaryArtists") && !obj.isNull("primaryArtists") -> obj.optString("primaryArtists", "")
            obj.has("artists") && !obj.isNull("artists") -> {
                val art = obj.opt("artists")
                if (art is JSONObject) {
                    val primary = art.optJSONArray("primary")
                    if (primary != null) {
                        (0 until primary.length()).mapNotNull { idx ->
                            primary.optJSONObject(idx)?.optString("name")
                        }.joinToString(", ")
                    } else ""
                } else art?.toString() ?: ""
            }
            else -> ""
        }

        val images = mutableListOf<SaavnImage>()
        val imgArr = obj.optJSONArray("image")
        if (imgArr != null) {
            for (i in 0 until imgArr.length()) {
                val imgObj = imgArr.optJSONObject(i)
                if (imgObj != null) {
                    val q = imgObj.optString("quality", "")
                    val u = imgObj.optString("url", imgObj.optString("link", ""))
                    if (u.isNotBlank()) {
                        images.add(SaavnImage(q, u))
                    }
                }
            }
        }

        val downloads = mutableListOf<SaavnDownloadUrl>()
        val dArr = obj.optJSONArray("downloadUrl")
        if (dArr != null) {
            for (i in 0 until dArr.length()) {
                val dObj = dArr.optJSONObject(i)
                if (dObj != null) {
                    val q = dObj.optString("quality", "")
                    val u = dObj.optString("url", dObj.optString("link", ""))
                    if (u.isNotBlank()) {
                        downloads.add(SaavnDownloadUrl(q, u))
                    }
                }
            }
        }

        return SaavnSong(
            id = id,
            name = name,
            duration = duration,
            explicitContent = explicit,
            primaryArtists = primaryArtists,
            image = images,
            downloadUrl = downloads
        )
    }

    /**
     * Search for songs on JioSaavn by a free-form query (title + artist recommended).
     *
     * @return Result wrapping a list of matched [SaavnSong]s, or failure if the
     *         request fails or returns no results.
     */
    suspend fun searchSongs(query: String): Result<List<SaavnSong>> = runCatching {
        val text = getWithFallback("search/songs") {
            parameter("query", query)
            parameter("limit", 5)
        }

        val root = JSONObject(text)
        val dataObj = root.optJSONObject("data")
        val resultsArr = dataObj?.optJSONArray("results")
            ?: root.optJSONArray("data")
            ?: root.optJSONArray("results")

        if (resultsArr == null || resultsArr.length() == 0) {
            throw NoSuchElementException("No songs found on JioSaavn for: \"$query\"")
        }

        val list = mutableListOf<SaavnSong>()
        for (i in 0 until resultsArr.length()) {
            val item = resultsArr.optJSONObject(i) ?: continue
            list.add(parseSong(item))
        }

        if (list.isEmpty()) {
            throw NoSuchElementException("No parseable songs on JioSaavn for: \"$query\"")
        }

        list
    }

    /**
     * Fetch the [SaavnSong] detail for a known Saavn song ID and extract the
     * best stream URL matching [quality].
     */
    suspend fun getBestStreamUrl(saavnSongId: String, quality: String): String? = runCatching {
        val text = getWithFallback("songs/$saavnSongId")
        val root = JSONObject(text)
        val dataArr = root.optJSONArray("data")
        val songObj = dataArr?.optJSONObject(0) ?: root.optJSONObject("data")

        if (songObj == null) return@runCatching null
        val song = parseSong(songObj)
        val urls = song.downloadUrl.filter { it.url.isNotBlank() }
        if (urls.isEmpty()) return@runCatching null

        urls.firstOrNull { it.quality.equals(quality, ignoreCase = true) }?.url
            ?: urls.firstOrNull { it.quality.equals("320kbps", ignoreCase = true) }?.url
            ?: urls.firstOrNull { it.quality.equals("160kbps", ignoreCase = true) }?.url
            ?: urls.lastOrNull()?.url
    }.getOrNull()
}
