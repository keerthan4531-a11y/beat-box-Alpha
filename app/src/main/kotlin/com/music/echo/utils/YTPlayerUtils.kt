

package iad1tya.echo.music.utils

import android.net.ConnectivityManager
import android.util.Log
import androidx.media3.common.PlaybackException
import com.music.innertube.NewPipeExtractor
import com.music.innertube.YouTube
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import iad1tya.echo.music.utils.BotDetectionMitigator
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.music.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.music.innertube.models.YouTubeClient.Companion.IOS
import com.music.innertube.models.YouTubeClient.Companion.IPADOS
import com.music.innertube.models.YouTubeClient.Companion.MOBILE
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5
import com.music.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.music.innertube.models.YouTubeClient.Companion.WEB
import com.music.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.music.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.music.innertube.models.response.PlayerResponse
import iad1tya.echo.music.constants.AudioQuality
import iad1tya.echo.music.utils.cipher.CipherDeobfuscator
import iad1tya.echo.music.utils.YTPlayerUtils.MAIN_CLIENT
import iad1tya.echo.music.utils.YTPlayerUtils.STREAM_FALLBACK_CLIENTS
import iad1tya.echo.music.utils.YTPlayerUtils.validateStatus
import iad1tya.echo.music.utils.potoken.PoTokenGenerator
import iad1tya.echo.music.utils.potoken.PoTokenResult
import iad1tya.echo.music.utils.sabr.EjsNTransformSolver
import iad1tya.echo.music.utils.PlaybackLogLevel
import iad1tya.echo.music.utils.PlaybackLogManager
import com.music.innertube.models.IpVersion
import okhttp3.Dns
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = Dns.SYSTEM.lookup(hostname)
                return when (YouTube.ipVersion) {
                    IpVersion.IPV4 -> addresses.filter { it is Inet4Address }.ifEmpty { addresses }
                    IpVersion.IPV6 -> addresses.filter { it is Inet6Address }.ifEmpty { addresses }
                    IpVersion.AUTO -> addresses
                }
            }
        })
        .proxySelector(object : ProxySelector() {
            override fun select(uri: URI?): List<Proxy> = listOfNotNull(YouTube.proxy ?: Proxy.NO_PROXY)
            override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
                Timber.tag(TAG).e(ioe, "Proxy connection failed for URI: $uri")
            }
        })
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    
    private val MAIN_CLIENT: YouTubeClient = TVHTML5_SIMPLY_EMBEDDED_PLAYER

    private val METADATA_CLIENT: YouTubeClient = WEB_REMIX

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        ANDROID_VR_1_61_48,
        WEB_REMIX,
        TVHTML5,
        ANDROID_CREATOR,
        IPADOS,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        WEB,
        WEB_CREATOR
    )
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
        val isSaavnStream: Boolean = false,
    )
    
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        context: android.content.Context? = null,
        knownArtist: String? = null,
        knownTitle: String? = null,
        knownDurationMs: Long? = null,
        isDownload: Boolean = false
    ): Result<PlaybackData> {
        val showFallbackToast = context?.let { 
            it.dataStore.data.first()[iad1tya.echo.music.constants.ShowAudioFallbackToastKey] 
        } ?: true

        var hasShownLosslessToast = false
        var hasShownSaavnToast = false
        var hasShownOpusToast = false

        if (videoId.startsWith("saavn_url:")) {
            val url = videoId.removePrefix("saavn_url:")
            val format = PlayerResponse.StreamingData.Format(
                itag = 0, url = url, mimeType = "audio/mp4; codecs=\"mp4a.40.2\"",
                bitrate = 320_000, audioQuality = "320kbps",
                contentLength = null, width = null, height = null, fps = null, qualityLabel = null,
                averageBitrate = null, approxDurationMs = null, audioSampleRate = null,
                audioChannels = null, loudnessDb = null, lastModified = null, signatureCipher = null,
                cipher = null, audioTrack = null, quality = "320kbps"
            )
            return Result.success(PlaybackData(
                audioConfig = null, videoDetails = null, playbackTracking = null,
                format = format, streamUrl = url, streamExpiresInSeconds = 3600,
                isSaavnStream = true
            ))
        }

        suspend fun fetchTrackMetadataFast(
            videoId: String,
            kTitle: String?,
            kArtist: String?
        ): Pair<String, String>? {
            if (!kTitle.isNullOrBlank() && !kArtist.isNullOrBlank()) {
                return kTitle to kArtist
            }

            return coroutineScope {
                val metaChannel = Channel<Pair<String, String>>(Channel.UNLIMITED)

                // InnerTube metadata racer — try multiple clients in parallel
                val innerTubeClients = listOf(
                    TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                    IOS,
                    ANDROID_VR_1_61_48,
                    WEB_REMIX
                )
                for (client in innerTubeClients) {
                    launch(Dispatchers.IO) {
                        try {
                            val resp = YouTube.player(videoId, null, client = client).getOrNull()
                            val title = resp?.videoDetails?.title
                            val author = resp?.videoDetails?.author?.replace(" - Topic", "")
                            if (!title.isNullOrBlank() && !author.isNullOrBlank()) {
                                metaChannel.trySend(title to author)
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Piped metadata racers
                val pipedInstances = listOf(
                    "https://pipedapi.kavin.rocks",
                    "https://pipedapi.adminforge.de"
                )
                for (instance in pipedInstances) {
                    launch(Dispatchers.IO) {
                        try {
                            val req = okhttp3.Request.Builder()
                                .url("$instance/streams/$videoId")
                                .header("User-Agent", "EchoMusic/1.0")
                                .header("Accept", "application/json")
                                .build()
                            val resp = httpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                val body = resp.body?.string()
                                if (body != null) {
                                    val json = org.json.JSONObject(body)
                                    val title = json.optString("title", "")
                                    val uploader = json.optString("uploader", "").replace(" - Topic", "")
                                    if (title.isNotBlank() && uploader.isNotBlank()) {
                                        metaChannel.trySend(title to uploader)
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                val result = withTimeoutOrNull(5000L) {
                    metaChannel.receiveCatching().getOrNull()
                }
                result ?: (kTitle?.let { it to (kArtist ?: "") })
            }
        }

        suspend fun trySaavn(metaTitle: String, metaArtist: String): Result<PlaybackData> {
            var saavnAttempt: Result<PlaybackData>? = null
            var lastException: Exception? = null

            val resolvedTitle = metaTitle.ifBlank { knownTitle.orEmpty() }
            val resolvedArtist = metaArtist.ifBlank { knownArtist.orEmpty() }

            if (resolvedTitle.isBlank()) {
                Timber.tag(TAG).d("Saavn skipped: title is blank for videoId=$videoId")
                return Result.failure(Exception("Title is blank for Saavn lookup"))
            }

            Timber.tag(TAG).d("JioSaavn streaming enabled (via Worker) — trying Saavn for videoId=$videoId, title=$resolvedTitle, artist=$resolvedArtist")
            try {
                saavnAttempt = withTimeoutOrNull(6000L) {
                    val title = resolvedTitle
                    val artist = resolvedArtist

                    // Title already validated above

                    val query = "$title $artist"
                        .replace("&", " ")
                        .replace(",", " ")
                        .replace(Regex("(?i)\\s*-\\s*topic\\b"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                    Timber.tag(TAG).d("Saavn search query: \"$query\" (original: \"$title $artist\")")

                    var songs = com.music.jiosaavn.SaavnService.searchSongs(query).getOrNull()
                    if (songs.isNullOrEmpty()) {
                        val cleanTitle = title.replace(Regex("(?i)\\s*\\([^)]*\\)"), "")
                            .replace(Regex("(?i)\\s*\\[[^]]*\\]"), "")
                            .trim()
                        if (cleanTitle.isNotBlank() && cleanTitle != title) {
                            val cleanQuery = "$cleanTitle $artist".trim()
                            Timber.tag(TAG).d("Saavn fallback search query: \"$cleanQuery\"")
                            songs = com.music.jiosaavn.SaavnService.searchSongs(cleanQuery).getOrNull()
                                ?: com.music.jiosaavn.SaavnService.searchSongs(cleanTitle).getOrNull()
                        }
                    }
                    if (songs.isNullOrEmpty()) {
                        songs = com.music.jiosaavn.SaavnService.searchSongs(title).getOrNull()
                    }

                    if (songs.isNullOrEmpty()) {
                        throw Exception("Saavn: no results for \"$query\"")
                    }

                    val ytDuration = knownDurationMs?.let { it / 1000L } ?: 0L

                    fun normalize(s: String): Set<String> =
                        s.lowercase()
                            .replace(Regex("[^a-z0-9\\s]"), " ")
                            .split(Regex("\\s+"))
                            .filter { it.length > 1 }
                            .toSet()

                    fun wordOverlapScore(a: String, b: String, maxPts: Int): Int {
                        val setA = normalize(a)
                        val setB = normalize(b)
                        if (setA.isEmpty() || setB.isEmpty()) return 0
                        val common = setA.intersect(setB).size
                        val ratio  = common.toDouble() / maxOf(setA.size, setB.size)
                        return (ratio * maxPts).toInt()
                    }

                    data class ScoredSong(val song: com.music.jiosaavn.SaavnSong, val score: Int)

                    val scored = songs.map { candidate ->
                        var score = 0
                        score += wordOverlapScore(title, candidate.name, maxPts = 50)
                        val saavnDuration = candidate.duration?.toLong() ?: 0L
                        if (ytDuration > 0 && saavnDuration > 0) {
                            val diff = Math.abs(ytDuration - saavnDuration)
                            score += when {
                                diff <= 5  -> 30
                                diff <= 15 -> 15
                                else       -> 0
                            }
                        }
                        val saavnArtists = candidate.primaryArtists
                        score += wordOverlapScore(artist, saavnArtists, maxPts = 20)
                        if (candidate.explicitContent) score += 5
                        score += com.music.jiosaavn.SaavnMatcher.variantPenalty(title, candidate.name)
                        ScoredSong(candidate, score)
                    }

                    val MIN_CONFIDENCE = 35
                    val bestSong = scored.maxByOrNull { it.score }
                        ?.takeIf { it.score >= MIN_CONFIDENCE }
                        ?.song

                    if (bestSong == null) {
                        throw Exception("Saavn: best score below threshold $MIN_CONFIDENCE")
                    }

                    Timber.tag(TAG).d("Saavn best match: id=${bestSong.id}, name=${bestSong.name}")

                    val directUrl = bestSong.downloadUrl.firstOrNull { it.quality.equals("320kbps", ignoreCase = true) }?.url
                        ?: bestSong.downloadUrl.firstOrNull { it.quality.equals("160kbps", ignoreCase = true) }?.url
                        ?: bestSong.downloadUrl.lastOrNull()?.url
                    val streamUrl = directUrl?.takeIf { it.isNotBlank() }
                        ?: com.music.jiosaavn.SaavnService.getBestStreamUrl(bestSong.id, "320kbps")

                    if (streamUrl.isNullOrBlank()) {
                        throw Exception("Saavn: no stream URL for songId=${bestSong.id}")
                    }

                    val format = PlayerResponse.StreamingData.Format(
                        itag = 0,
                        url = streamUrl,
                        mimeType = "audio/mp4; codecs=\"mp4a.40.2\"",
                        bitrate = 320_000,
                        width = null,
                        height = null,
                        contentLength = null,
                        quality = "320kbps",
                        fps = null,
                        qualityLabel = null,
                        averageBitrate = null,
                        audioQuality = "320kbps",
                        approxDurationMs = null,
                        audioSampleRate = null,
                        audioChannels = null,
                        loudnessDb = null,
                        lastModified = null,
                        signatureCipher = null,
                        cipher = null,
                        audioTrack = null
                    )

                    val saavnImage = bestSong.image.lastOrNull()?.url ?: bestSong.image.firstOrNull()?.url
                    val videoDetails = PlayerResponse.VideoDetails(
                        videoId = videoId, title = title, author = artist, lengthSeconds = ytDuration.toString(),
                        channelId = "", musicVideoType = null, viewCount = null,
                        thumbnail = com.music.innertube.models.Thumbnails(
                            listOf(com.music.innertube.models.Thumbnail(url = saavnImage ?: "", width = 500, height = 500))
                        )
                    )

                    Result.success(PlaybackData(
                        audioConfig = null,
                        videoDetails = videoDetails,
                        playbackTracking = null,
                        format = format,
                        streamUrl = streamUrl,
                        streamExpiresInSeconds = 3600,
                        isSaavnStream = true
                    ))
                }

                if (saavnAttempt == null) {
                    lastException = Exception("Timeout fetching Saavn stream from worker")
                }
            } catch (e: Exception) {
                lastException = e
            }

            return saavnAttempt ?: Result.failure(lastException ?: Exception("Saavn resolution failed"))
        }

        suspend fun fetchInnerTubeClient(
            client: YouTubeClient,
            sigTimestamp: Int?
        ): PlaybackData? {
            return try {
                val resp = YouTube.player(videoId, playlistId, client, sigTimestamp, null).getOrNull() ?: return null
                if (resp.playabilityStatus.status != "OK") return null
                val fmt = findFormat(resp, audioQuality, connectivityManager) ?: return null
                var url = findUrlOrNull(fmt, videoId, resp) ?: return null
                if (url.isBlank()) return null

                // 1. Transform n-parameter (critical: prevents googlevideo 403 throttle)
                try {
                    val transformed = EjsNTransformSolver.transformNParamInUrl(url)
                    if (transformed != url) url = transformed
                } catch (_: Exception) {}
                try {
                    val cipherTransformed = CipherDeobfuscator.transformNParamInUrl(url)
                    if (cipherTransformed != url) url = cipherTransformed
                } catch (_: Exception) {}

                // 2. Apply PoToken for web clients
                if (client.useWebPoTokens) {
                    val sessionId = if (YouTube.cookie != null) YouTube.dataSyncId else YouTube.visitorData
                    if (sessionId != null) {
                        try {
                            val pot = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                            if (pot?.streamingDataPoToken != null) {
                                val separator = if ("?" in url) "&" else "?"
                                url = "${url}${separator}pot=${pot.streamingDataPoToken}"
                            }
                        } catch (_: Exception) {}
                    }
                }

                // 3. Strict validation: ensure URL actually works before winning race
                if (!validateStatus(url)) {
                    var recovered = false
                    try {
                        val nTrans = CipherDeobfuscator.transformNParamInUrl(url)
                        if (nTrans != url && validateStatus(nTrans)) {
                            url = nTrans
                            recovered = true
                        }
                    } catch (_: Exception) {}

                    if (!recovered) {
                        Timber.tag(TAG).w("[Race] Client ${client.clientName} URL failed HTTP validation (403), skipping")
                        return null
                    }
                }

                Timber.tag(TAG).i("[Race] Client ${client.clientName} VALIDATED OK! URL: ${url.take(80)}...")
                PlaybackData(
                    audioConfig = resp.playerConfig?.audioConfig,
                    videoDetails = resp.videoDetails,
                    playbackTracking = resp.playbackTracking,
                    format = fmt,
                    streamUrl = url,
                    streamExpiresInSeconds = resp.streamingData?.expiresInSeconds ?: 3600
                )
            } catch (_: Exception) {
                null
            }
        }

        suspend fun fetchPipedStream(
            instance: String,
            pTitle: String?,
            pArtist: String?
        ): PlaybackData? {
            return try {
                val url = "$instance/streams/$videoId"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "EchoMusic/1.0")
                    .header("Accept", "application/json")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) return null

                val body = response.body?.string() ?: return null
                val jsonObj = org.json.JSONObject(body)

                val audioStreams = jsonObj.optJSONArray("audioStreams") ?: return null
                if (audioStreams.length() == 0) return null

                var bestUrl: String? = null
                var bestBitrate = 0
                var bestMimeType = "audio/mp4"
                var bestQuality = ""

                for (i in 0 until audioStreams.length()) {
                    val stream = audioStreams.getJSONObject(i)
                    val streamUrl = stream.optString("url", "")
                    val bitrate = stream.optInt("bitrate", 0)
                    val mimeType = stream.optString("mimeType", "audio/mp4")
                    val quality = stream.optString("quality", "")

                    if (streamUrl.isNotBlank() && bitrate > bestBitrate) {
                        bestUrl = streamUrl
                        bestBitrate = bitrate
                        bestMimeType = mimeType
                        bestQuality = quality
                    }
                }

                if (bestUrl.isNullOrBlank()) return null

                val title = jsonObj.optString("title", pTitle ?: videoId)
                val uploader = jsonObj.optString("uploader", pArtist ?: "")
                val duration = jsonObj.optLong("duration", 0L)
                val thumbnailUrl = jsonObj.optString("thumbnailUrl", "")

                val standardMime = if (bestMimeType.contains("codecs=")) bestMimeType else {
                    if (bestMimeType.contains("webm")) "audio/webm; codecs=\"opus\""
                    else "audio/mp4; codecs=\"mp4a.40.2\""
                }

                val format = PlayerResponse.StreamingData.Format(
                    itag = 0,
                    url = bestUrl,
                    mimeType = standardMime,
                    bitrate = bestBitrate,
                    width = null,
                    height = null,
                    contentLength = null,
                    quality = bestQuality,
                    fps = null,
                    qualityLabel = null,
                    averageBitrate = null,
                    audioQuality = bestQuality.ifBlank { "${bestBitrate / 1000}kbps" },
                    approxDurationMs = (duration * 1000).toString(),
                    audioSampleRate = null,
                    audioChannels = null,
                    loudnessDb = null,
                    lastModified = null,
                    signatureCipher = null,
                    cipher = null,
                    audioTrack = null
                )

                val videoDetails = PlayerResponse.VideoDetails(
                    videoId = videoId,
                    title = title,
                    author = uploader.replace(" - Topic", ""),
                    lengthSeconds = duration.toString(),
                    channelId = "",
                    musicVideoType = null,
                    viewCount = null,
                    thumbnail = com.music.innertube.models.Thumbnails(
                        if (thumbnailUrl.isNotBlank()) {
                            listOf(com.music.innertube.models.Thumbnail(url = thumbnailUrl, width = 720, height = 720))
                        } else emptyList()
                    )
                )

                PlaybackData(
                    audioConfig = null,
                    videoDetails = videoDetails,
                    playbackTracking = null,
                    format = format,
                    streamUrl = bestUrl,
                    streamExpiresInSeconds = 3600
                )
            } catch (_: Exception) {
                null
            }
        }

        suspend fun fetchNewPipeStream(pTitle: String?, pArtist: String?): PlaybackData? {
            return try {
                val streams = NewPipeExtractor.newPipePlayer(videoId)
                val audioStream = streams.firstOrNull { it.second.isNotBlank() }?.second ?: return null
                val format = PlayerResponse.StreamingData.Format(
                    itag = 0, url = audioStream, mimeType = "audio/mp4; codecs=\"mp4a.40.2\"", bitrate = 160000,
                    width = null, height = null, contentLength = null, quality = "160kbps",
                    fps = null, qualityLabel = null, averageBitrate = null,
                    audioQuality = "160kbps", approxDurationMs = null, audioSampleRate = null,
                    audioChannels = null, loudnessDb = null, lastModified = null,
                    signatureCipher = null, cipher = null, audioTrack = null
                )
                val videoDetails = PlayerResponse.VideoDetails(
                    videoId = videoId, title = pTitle ?: videoId, author = pArtist ?: "",
                    lengthSeconds = "0", channelId = "", musicVideoType = null, viewCount = null,
                    thumbnail = com.music.innertube.models.Thumbnails(emptyList())
                )
                PlaybackData(
                    audioConfig = null, videoDetails = videoDetails, playbackTracking = null,
                    format = format, streamUrl = audioStream, streamExpiresInSeconds = 3600
                )
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Simultaneous Parallel Race:
         * Queries YouTube InnerTube clients, Piped instances, and NewPipe simultaneously.
         * The FIRST valid response immediately starts playing.
         */
        suspend fun raceAllFallbackStreams(
            sigTimestamp: Int?,
            fTitle: String?,
            fArtist: String?
        ): Result<PlaybackData> = coroutineScope {
            val resultChannel = Channel<PlaybackData>(Channel.UNLIMITED)
            val jobs = mutableListOf<Job>()

            // InnerTube Racers
            val clientsToRace = listOf(
                ANDROID_VR_1_61_48,
                IOS,
                TVHTML5_SIMPLY_EMBEDDED_PLAYER,
                WEB_REMIX,
                ANDROID_CREATOR,
                ANDROID_VR_NO_AUTH,
                MOBILE,
                IPADOS,
                TVHTML5,
                WEB
            )
            for (c in clientsToRace) {
                jobs += launch(Dispatchers.IO) {
                    fetchInnerTubeClient(c, sigTimestamp)?.let { resultChannel.trySend(it) }
                }
            }

            // Piped Instance Racers
            val pipedUrls = listOf(
                "https://pipedapi.kavin.rocks",
                "https://pipedapi.adminforge.de",
                "https://api.piped.projectsegfau.lt",
                "https://pipedapi.leptons.xyz"
            )
            for (pUrl in pipedUrls) {
                jobs += launch(Dispatchers.IO) {
                    fetchPipedStream(pUrl, fTitle, fArtist)?.let { resultChannel.trySend(it) }
                }
            }

            // NewPipe Racer
            jobs += launch(Dispatchers.IO) {
                fetchNewPipeStream(fTitle, fArtist)?.let { resultChannel.trySend(it) }
            }

            // Wait for first valid result with overall timeout
            val winner = withTimeoutOrNull(15000L) {
                // Keep trying to receive results until one succeeds or all jobs finish
                var result: PlaybackData? = null
                while (result == null) {
                    val received = resultChannel.receiveCatching().getOrNull()
                    if (received != null) {
                        result = received
                    } else if (jobs.none { it.isActive }) {
                        // All jobs completed without sending a result
                        break
                    }
                }
                result
            }

            // Cancel remaining jobs
            jobs.forEach { it.cancel() }

            if (winner != null) {
                Timber.tag(TAG).i("Parallel streaming race WON! Stream URL: ${winner.streamUrl.take(80)}...")
                Result.success(winner)
            } else {
                Timber.tag(TAG).w("Parallel streaming race had no validated winners, falling back to resolvePlaybackData")
                resolvePlaybackData(videoId, playlistId, audioQuality, connectivityManager)
            }
        }

        suspend fun tryLossless(): Result<PlaybackData> {
            var qobuzAttempt: Result<PlaybackData>? = null
            var lastException: Exception? = null
            for (attempt in 1..3) {
                try {
                    qobuzAttempt = withTimeoutOrNull(15000L) {
                        val metadata = playerResponseForMetadata(videoId).getOrNull()
                        val title = knownTitle ?: metadata?.videoDetails?.title
                        val author = knownArtist ?: metadata?.videoDetails?.author?.replace(" - Topic", "")
                        if (title != null && author != null) {
                            val qobuzClient = iad1tya.echo.music.utils.qobuz.QobuzApiClient()
                            val queryArtist = author
                            val queryTitle = title
                            val durationSeconds = metadata?.videoDetails?.lengthSeconds?.toLongOrNull()
                            val durationMs = knownDurationMs ?: (if (durationSeconds != null) durationSeconds * 1000L else null)

                            var resolvedPlaybackData: PlaybackData? = null
                            for (term in qobuzSearchTerms(queryArtist, queryTitle)) {
                                val searchResult = runCatching { qobuzClient.search(term) }.getOrNull() ?: continue
                                val candidates = searchResult.tracks?.items ?: continue
                                val validCandidates = candidates.filter {
                                    val streamable = it.streamable ?: false
                                    val maxDepth = it.maximumBitDepth ?: 0
                                    streamable && maxDepth >= 16
                                }
                                val sorted = validCandidates.sortedByDescending { confidence(queryArtist, queryTitle, durationMs, it) }
                                for (candidate in sorted) {
                                    if (confidence(queryArtist, queryTitle, durationMs, candidate) >= 0.5f) {
                                        val downloadData = runCatching { qobuzClient.getFileUrl(candidate.id) }.getOrNull()
                                        val url = downloadData?.url
                                        if (url != null) {
                                            val format = PlayerResponse.StreamingData.Format(
                                                itag = 0,
                                                mimeType = "audio/flac; codecs=\"flac\"",
                                                bitrate = (candidate.maximumSamplingRate * 1000 * candidate.maximumBitDepth * 2).toInt(),
                                                audioSampleRate = (candidate.maximumSamplingRate * 1000).toInt(),
                                                contentLength = 0L,
                                                url = url,
                                                cipher = null,
                                                signatureCipher = null,
                                                audioQuality = "LOSSLESS",
                                                fps = null,
                                                width = null,
                                                height = null,
                                                quality = "lossless",
                                                qualityLabel = null,
                                                averageBitrate = null,
                                                approxDurationMs = null,
                                                audioChannels = null,
                                                loudnessDb = null,
                                                lastModified = null,
                                                audioTrack = null
                                            )
                                            resolvedPlaybackData = PlaybackData(
                                                audioConfig = null,
                                                videoDetails = metadata?.videoDetails,
                                                playbackTracking = null,
                                                format = format,
                                                streamUrl = url,
                                                streamExpiresInSeconds = 3600
                                            )
                                            break
                                        }
                                    }
                                }
                                if (resolvedPlaybackData != null) {
                                    break
                                }
                            }

                            if (resolvedPlaybackData != null) {
                                return@withTimeoutOrNull Result.success(resolvedPlaybackData)
                            } else {
                                throw Exception("No streamable match resolved on Qobuz")
                            }
                        } else {
                            throw Exception("Missing title or artist for lookup")
                        }
                    }
                    if (qobuzAttempt == null) {
                        lastException = Exception("Timeout fetching Qobuz stream")
                    }
                } catch (e: Exception) {
                    lastException = e
                }

                if (qobuzAttempt != null && qobuzAttempt.isSuccess) {
                    break
                }
            }
            return qobuzAttempt ?: Result.failure(lastException ?: Exception("Qobuz resolution failed"))
        }

        // Fast metadata resolution
        val meta = fetchTrackMetadataFast(videoId, knownTitle, knownArtist)
        val title = meta?.first ?: knownTitle.orEmpty()
        val artist = meta?.second ?: knownArtist.orEmpty()
        val sigTimestamp = getSignatureTimestampOrNull(videoId).timestamp

        return when (audioQuality) {
            AudioQuality.LOSSLESS -> {
                val losslessRes = tryLossless()
                if (losslessRes.isSuccess) return losslessRes

                Timber.tag(TAG).e("Qobuz resolution failed, trying JioSaavn Worker")
                val saavnRes = trySaavn(title, artist)
                if (saavnRes.isSuccess) return saavnRes

                Timber.tag(TAG).e("Saavn Worker failed, running parallel streaming race")
                raceAllFallbackStreams(sigTimestamp, title, artist)
            }
            else -> {
                // Priority 1: User's Cloudflare Worker (JioSaavn 320kbps)
                val saavnRes = trySaavn(title, artist)
                if (saavnRes.isSuccess) return saavnRes

                // Priority 2 & 3: Parallel Streaming Race (InnerTube + Piped instances + NewPipe)
                Timber.tag(TAG).d("Song not on Worker, racing all YouTube & Piped endpoints in parallel")
                raceAllFallbackStreams(sigTimestamp, title, artist)
            }
        }
    }

    private suspend fun resolvePlaybackData(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        PlaybackLogManager.log(PlaybackLogLevel.INFO, "Resolving playback data", "Video: $videoId")
        
        
        val isUploadedTrack = playlistId == "MLPT" || playlistId?.contains("MLPT") == true

        val isLoggedIn = YouTube.cookie != null
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: ${signatureTimestamp.timestamp}")

        
        var poToken: PoTokenResult? = null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            Timber.tag(logTag).d("Generating PoToken for MAIN_CLIENT with sessionId")
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                if (poToken != null) {
                    Timber.tag(logTag).d("PoToken generated successfully")
                }
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "PoToken generation failed: ${e.message}")
            }
        }

        
        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying ${MAIN_CLIENT.clientName} (Main)")
        var mainPlayerResponse = YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp.timestamp, poToken?.playerRequestPoToken).getOrThrow()

        
        
        
        var metadataResponse: PlayerResponse? = null
        if (isLoggedIn) {
            Timber.tag(logTag).d("Fetching metadata from METADATA_CLIENT (WEB_REMIX) for authenticated tracking")
            try {
                
                var metaPoToken: PoTokenResult? = null
                val metaSessionId = YouTube.dataSyncId
                if (METADATA_CLIENT.useWebPoTokens && metaSessionId != null) {
                    try {
                        metaPoToken = poTokenGenerator.getWebClientPoToken(videoId, metaSessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Metadata PoToken generation failed")
                    }
                }
                metadataResponse = YouTube.player(
                    videoId, playlistId, METADATA_CLIENT,
                    signatureTimestamp.timestamp, metaPoToken?.playerRequestPoToken
                ).getOrNull()
                Timber.tag(logTag).d("Metadata response obtained: ${metadataResponse?.playabilityStatus?.status}")
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "Failed to fetch metadata from METADATA_CLIENT")
            }
        }

        
        var usedAgeRestrictedClient: YouTubeClient? = null
        val wasOriginallyAgeRestricted: Boolean

        
        
        
        
        
        val mainStatus = mainPlayerResponse.playabilityStatus.status
        val isAgeRestrictedFromResponse = mainStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )
        wasOriginallyAgeRestricted = isAgeRestrictedFromResponse

        if (isAgeRestrictedFromResponse && isLoggedIn) {
            
            Timber.tag(logTag).d("Age-restricted detected, using WEB_CREATOR")
            Log.i(TAG, "Age-restricted: using WEB_CREATOR for videoId=$videoId")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("WEB_CREATOR works for age-restricted content")
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        
        if (mainPlayerResponse == null) {
            throw Exception("Failed to get player response")
        }

        
        
        val audioConfig = metadataResponse?.playerConfig?.audioConfig ?: mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = metadataResponse?.videoDetails ?: mainPlayerResponse.videoDetails
        val playbackTracking = metadataResponse?.playbackTracking ?: mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null
        var retryMainPlayerResponse: PlayerResponse? = if (usedAgeRestrictedClient != null) mainPlayerResponse else null

        
        val currentStatus = mainPlayerResponse.playabilityStatus.status
        var isAgeRestricted = currentStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )

        if (isAgeRestricted) {
            Timber.tag(logTag).d("Content is still age-restricted (status: $currentStatus), will try fallback clients")
            Log.i(TAG, "Age-restricted content detected: videoId=$videoId, status=$currentStatus")
        }

        
        val isPrivateTrack = mainPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

        
        
        
        val startIndex = when {
            isPrivateTrack -> 1  
            isAgeRestricted -> 0
            else -> -1
        }

        for (clientIndex in (startIndex until STREAM_FALLBACK_CLIENTS.size)) {
            
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            
            val client: YouTubeClient
            if (clientIndex == -1) {
                
                client = MAIN_CLIENT
                streamPlayerResponse = retryMainPlayerResponse ?: mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag).d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.DEBUG, "Trying fallback [${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}]", client.clientName)

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    
                    Timber.tag(logTag).d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                
                if (client.useWebPoTokens && poToken == null && sessionId != null) {
                    Timber.tag(logTag).d("Lazily generating PoToken for fallback web client: ${client.clientName}")
                    try {
                        poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "Lazy PoToken generation failed")
                    }
                }

                Timber.tag(logTag).d("Fetching player response for fallback client: ${client.clientName}")
                
                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                
                val clientSigTimestamp = if (wasOriginallyAgeRestricted) null else signatureTimestamp.timestamp
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken).getOrNull()
            }

            
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag).d("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                PlaybackLogManager.log(PlaybackLogLevel.INFO, "Player response OK", if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName)

                
                val hasDirectUrls = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.url.isNullOrEmpty() } == true
                val hasSignatureCipher = streamPlayerResponse.streamingData?.adaptiveFormats
                    ?.any { !it.signatureCipher.isNullOrEmpty() || !it.cipher.isNullOrEmpty() } == true

                Timber.tag(logTag).d("URL check: hasDirectUrls=$hasDirectUrls, hasSignatureCipher=$hasSignatureCipher")

                
                val responseToUse = streamPlayerResponse

                format =
                    findFormat(
                        responseToUse,
                        audioQuality,
                        connectivityManager,
                    )

                if (format == null) {
                    Timber.tag(logTag).d("No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId, responseToUse, skipNewPipe = wasOriginallyAgeRestricted)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                
                val currentClient = if (clientIndex == -1) {
                    usedAgeRestrictedClient ?: MAIN_CLIENT
                } else {
                    STREAM_FALLBACK_CLIENTS[clientIndex]
                }

                
                val isPrivatelyOwnedTrack = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                
                if (currentClient.useWebPoTokens) {
                    try {
                        Timber.tag(logTag).d("Applying n-transform to stream URL for ${currentClient.clientName}")
                        val transformed = EjsNTransformSolver.transformNParamInUrl(streamUrl!!)
                        if (transformed != streamUrl) {
                            streamUrl = transformed
                            Timber.tag(logTag).d("N-transform applied successfully")
                        }
                    } catch (e: Exception) {
                        Timber.tag(logTag).e(e, "N-transform failed: ${e.message}")
                    }
                }

                
                
                if (currentClient.useWebPoTokens && poToken?.streamingDataPoToken != null) {
                    Timber.tag(logTag).d("Appending pot= parameter to stream URL")
                    val separator = if ("?" in streamUrl!!) "&" else "?"
                    streamUrl = "${streamUrl}${separator}pot=${poToken.streamingDataPoToken}"
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                
                val urlHost = try { java.net.URL(streamUrl).host } catch (e: Exception) { "unknown" }
                Timber.tag(logTag).d("Stream URL host: $urlHost, pot length: ${poToken?.streamingDataPoToken?.length ?: 0}")

                
                val isPrivatelyOwned = streamPlayerResponse.videoDetails?.musicVideoType == "MUSIC_VIDEO_TYPE_PRIVATELY_OWNED_TRACK"

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1 || isPrivatelyOwned) {
                    
                    if (isPrivatelyOwned) {
                        Timber.tag(logTag).d("Skipping validation for privately owned track: ${currentClient.clientName}")
                    } else {
                        Timber.tag(logTag).d("Using last fallback client without validation: ${STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    }
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId, private=$isPrivatelyOwned")
                    break
                }

                if (validateStatus(streamUrl!!)) {
                    
                    Timber.tag(logTag).d("Stream validated successfully with client: ${currentClient.clientName}")
                    PlaybackLogManager.log(PlaybackLogLevel.INFO, "Stream validated", currentClient.clientName)
                    
                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId")
                    break
                } else {
                    Timber.tag(logTag).d("Stream validation failed for client: ${currentClient.clientName}")

                    
                    if (currentClient.useWebPoTokens) {
                        var nTransformWorked = false

                        
                        try {
                            val nTransformed = CipherDeobfuscator.transformNParamInUrl(streamUrl!!)
                            if (nTransformed != streamUrl) {
                                Timber.tag(logTag).d("CipherDeobfuscator n-transform applied, re-validating...")
                                if (validateStatus(nTransformed)) {
                                    Timber.tag(logTag).d("N-transformed URL VALIDATED OK!")
                                    streamUrl = nTransformed
                                    nTransformWorked = true
                                    Log.i(TAG, "Playback: client=${currentClient.clientName}, videoId=$videoId (cipher n-transform)")
                                }
                            }
                        } catch (e: Exception) {
                            Timber.tag(logTag).e(e, "CipherDeobfuscator n-transform error")
                        }

                        if (nTransformWorked) break
                    }
                }
            } else {
                val status = streamPlayerResponse?.playabilityStatus?.status ?: "Unknown"
                val reason = streamPlayerResponse?.playabilityStatus?.reason ?: "No reason"
                Timber.tag(logTag).d("Player response status not OK: $status, reason: $reason")
                PlaybackLogManager.log(PlaybackLogLevel.WARNING, "Client failed: ${client.clientName}", "$status: $reason")
                
                
                Timber.tag(logTag).d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }.onFailure { e ->
        Timber.tag(logTag).e(e, "Playback resolution failed")
        PlaybackLogManager.log(PlaybackLogLevel.ERROR, "Playback failed", "${e::class.simpleName}: ${e.message}")
    }
    
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) 
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.OPUS, AudioQuality.SAAVN, AudioQuality.LOSSLESS -> 1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) 
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }
    
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)

            
            YouTube.cookie?.let { cookie ->
                requestBuilder.addHeader("Cookie", cookie)
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }
    data class SignatureTimestampResult(
        val timestamp: Int?,
        val isAgeRestricted: Boolean
    )

    private fun getSignatureTimestampOrNull(videoId: String): SignatureTimestampResult {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        val result = NewPipeExtractor.getSignatureTimestamp(videoId)
        return result.fold(
            onSuccess = { timestamp ->
                Timber.tag(logTag).d("Signature timestamp obtained: $timestamp")
                SignatureTimestampResult(timestamp, isAgeRestricted = false)
            },
            onFailure = { error ->
                val isAgeRestricted = error.message?.contains("age-restricted", ignoreCase = true) == true ||
                    error.cause?.message?.contains("age-restricted", ignoreCase = true) == true
                if (isAgeRestricted) {
                    Timber.tag(logTag).d("Age-restricted content detected from NewPipe")
                    Log.i(TAG, "Age-restricted detected early via NewPipe: videoId=$videoId")
                } else {
                    Timber.tag(logTag).e(error, "Failed to get signature timestamp")
                    reportException(error)
                }
                SignatureTimestampResult(null, isAgeRestricted)
            }
        )
    }

    suspend fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId, skipNewPipe: $skipNewPipe")

        
        if (!format.url.isNullOrEmpty()) {
            Timber.tag(logTag).d("Using URL from format directly")
            return format.url
        }

        
        val signatureCipher = format.signatureCipher ?: format.cipher
        if (!signatureCipher.isNullOrEmpty()) {
            Timber.tag(logTag).d("Format has signatureCipher, using custom deobfuscation")
            val customDeobfuscatedUrl = CipherDeobfuscator.deobfuscateStreamUrl(signatureCipher, videoId)
            if (customDeobfuscatedUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained via custom cipher deobfuscation")
                return customDeobfuscatedUrl
            }
            Timber.tag(logTag).d("Custom cipher deobfuscation failed")
        }

        
        if (skipNewPipe) {
            Timber.tag(logTag).d("Skipping NewPipe methods for age-restricted content")
            return null
        }

        
        val deobfuscatedUrl = NewPipeExtractor.getStreamUrl(format, videoId)
        if (deobfuscatedUrl != null) {
            Timber.tag(logTag).d("Stream URL obtained via NewPipe deobfuscation")
            return deobfuscatedUrl
        }

        
        Timber.tag(logTag).d("Trying StreamInfo fallback for URL")
        val streamUrls = YouTube.getNewPipeStreamUrls(videoId)
        if (streamUrls.isNotEmpty()) {
            val streamUrl = streamUrls.find { it.first == format.itag }?.second
            if (streamUrl != null) {
                Timber.tag(logTag).d("Stream URL obtained from StreamInfo")
                return streamUrl
            }

            
            val audioStream = streamUrls.find { urlPair ->
                playerResponse.streamingData?.adaptiveFormats?.any {
                    it.itag == urlPair.first && it.isAudio
                } == true
            }?.second

            if (audioStream != null) {
                Timber.tag(logTag).d("Audio stream URL obtained from StreamInfo (different itag)")
                return audioStream
            }
        }

        Timber.tag(logTag).e("Failed to get stream URL")
        return null
    }

    fun forceRefreshForVideo(videoId: String) {
        Timber.tag(logTag).d("Force refreshing for videoId: $videoId")
    }

    data class VideoPlaybackResult(
        val videoUrl: String,
        val quality: String = "HD",
        val isMuxed: Boolean = true,
    )

    suspend fun getVideoPlaybackUrl(
        videoId: String,
        knownTitle: String? = null,
        knownArtist: String? = null,
    ): Result<VideoPlaybackResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        runCatching {
            var targetVideoId = videoId
            if (targetVideoId.startsWith("saavn_url:")) {
                val query = "${knownTitle.orEmpty()} ${knownArtist.orEmpty()} official video".trim()
                if (query.isNotBlank()) {
                    val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                        ?: YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    val matched = searchResult?.items?.filterIsInstance<com.music.innertube.models.SongItem>()
                        ?.firstOrNull()?.id
                        ?: searchResult?.items?.firstOrNull()?.id
                    if (!matched.isNullOrBlank() && !matched.startsWith("saavn_url:")) {
                        targetVideoId = matched
                    }
                }
            }

            if (targetVideoId.startsWith("saavn_url:")) {
                throw Exception("Cannot resolve YouTube video for track without title")
            }

            Timber.tag(TAG).d("Resolving video playback for targetVideoId: $targetVideoId")

            val playerResponse = YouTube.player(
                videoId = targetVideoId,
                client = TVHTML5_SIMPLY_EMBEDDED_PLAYER
            ).getOrNull() ?: YouTube.player(
                videoId = targetVideoId,
                client = IOS
            ).getOrNull() ?: YouTube.player(
                videoId = targetVideoId,
                client = WEB
            ).getOrThrow()

            // 1. Check progressive / muxed formats (contains both video + audio)
            val progressiveFormats = playerResponse.streamingData?.formats
                ?.filter { it.mimeType.startsWith("video/") && (it.height ?: 0) >= 360 }
                ?.sortedByDescending { it.height ?: 0 }

            for (format in progressiveFormats.orEmpty()) {
                val url = findUrlOrNull(format, targetVideoId, playerResponse)
                if (!url.isNullOrBlank()) {
                    val q = format.qualityLabel ?: "${format.height ?: 720}p"
                    return@runCatching VideoPlaybackResult(
                        videoUrl = url,
                        quality = q,
                        isMuxed = true
                    )
                }
            }

            // 2. Check adaptive formats (1080p, 720p, 480p)
            val adaptiveVideoFormats = playerResponse.streamingData?.adaptiveFormats
                ?.filter { it.mimeType.startsWith("video/") }
                ?.sortedByDescending { (it.height ?: 0) * 10000 + (it.bitrate ?: 0) }

            for (format in adaptiveVideoFormats.orEmpty()) {
                val url = findUrlOrNull(format, targetVideoId, playerResponse)
                if (!url.isNullOrBlank()) {
                    val q = format.qualityLabel ?: "${format.height ?: 720}p"
                    return@runCatching VideoPlaybackResult(
                        videoUrl = url,
                        quality = q,
                        isMuxed = false
                    )
                }
            }

            // 3. Fallback to NewPipe video streams
            val newPipeStreams = NewPipeExtractor.newPipePlayer(targetVideoId)
            val videoStream = newPipeStreams.firstOrNull { it.second.isNotBlank() }?.second
            if (!videoStream.isNullOrBlank()) {
                return@runCatching VideoPlaybackResult(
                    videoUrl = videoStream,
                    quality = "HD",
                    isMuxed = true
                )
            }

            throw Exception("No playable video format found for $targetVideoId")
        }
    }
}




fun cleanSearchTerm(term: String): String {
    return term
        .replace(Regex("(?i)\\b(official video|music video|official audio|lyric video|lyrics|audio|video|hq|high quality)\\b"), "")
        .replace(Regex("\\([^)]*\\)"), "")
        .replace(Regex("\\[[^]]*\\]"), "")
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun qobuzSearchTerms(artist: String, title: String): List<String> {
    val cleanArtist = cleanSearchTerm(artist)
    val cleanTitle = cleanSearchTerm(title)
    
    val terms = mutableListOf<String>()
    
    terms.add("$artist $title".trim())
    
    if (cleanArtist.isNotEmpty() && cleanTitle.isNotEmpty()) {
        terms.add("$cleanArtist $cleanTitle".trim())
    }
    
    val primary = artist.substringBefore(",").trim()
    if (primary.isNotEmpty() && !primary.equals(artist.trim(), ignoreCase = true)) {
        terms.add("$primary $title".trim())
        val cleanPrimary = cleanSearchTerm(primary)
        if (cleanPrimary.isNotEmpty() && cleanTitle.isNotEmpty()) {
            terms.add("$cleanPrimary $cleanTitle".trim())
        }
    }
    
    return terms.distinct()
}

private fun normalize(s: String): String =
    s.lowercase()
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("\\[[^]]*\\]"), " ")
        .replace(Regex("(?i)\\b(feat\\.?|ft\\.?|featuring)\\b.*"), " ")
        .replace(Regex("[''`]"), "")
        .replace(Regex("[^\\p{L}\\p{N}\\p{S}\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun jaccard(a: String, b: String): Float {
    val setA = a.split(" ").filter { it.isNotEmpty() }.toSet()
    val setB = b.split(" ").filter { it.isNotEmpty() }.toSet()
    if (setA.isEmpty() || setB.isEmpty()) return 0f
    val intersection = setA.intersect(setB).size.toFloat()
    val union = setA.union(setB).size.toFloat()
    return intersection / union
}

private fun artistSimilarity(a: String, b: String): Float {
    val setA = a.split(" ").filter { it.isNotEmpty() }.toSet()
    val setB = b.split(" ").filter { it.isNotEmpty() }.toSet()
    if (setA.isEmpty() || setB.isEmpty()) return 0f

    val intersection = setA.intersect(setB)
    val union = setA.union(setB)
    val jaccardScore = intersection.size.toFloat() / union.size.toFloat()

    val smallerSize = minOf(setA.size, setB.size)
    val smallerFullyCovered = intersection.size == smallerSize
    val hasDistinctiveOverlap = intersection.any { token ->
        token.length > 3 || token.any { ch -> !ch.isLetterOrDigit() }
    }

    val coverageScore = if (smallerFullyCovered && hasDistinctiveOverlap) 1.0f else 0f
    return maxOf(jaccardScore, coverageScore)
}

fun confidence(queryArtist: String, queryTitle: String, queryDuration: Long?, candidate: iad1tya.echo.music.utils.qobuz.QobuzTrack): Float {
    if (!candidate.streamable) return 0f

    val titleSim = jaccard(normalize(queryTitle), normalize(candidate.title))
    val artistSim = artistSimilarity(
        normalize(queryArtist),
        normalize(candidate.performer?.name.orEmpty()),
    )

    val durationFactor: Float = run {
        val queryMs = queryDuration ?: return@run 1.0f
        if (queryMs <= 0 || candidate.duration <= 0) return@run 1.0f
        val candidateMs = candidate.duration * 1000L
        val drift = kotlin.math.abs(queryMs - candidateMs).toDouble() / queryMs.toDouble()
        when {
            drift < 0.05 -> 1.0f      
            drift < 0.10 -> 0.85f     
            drift < 0.20 -> 0.6f      
            else -> 0.3f              
        }
    }

    return (titleSim * artistSim * durationFactor)
}
