package com.music.echo.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class TamilVoiceEngine(val displayName: String, val tag: String) {
    MICROSOFT_PALLAVI("Microsoft Pallavi Neural (Studio Ponnu)", "pallavi"),
    GOOGLE_NEURAL("Google Neural Stream (Fast & Natural)", "google")
}

class TamilTtsService(private val context: Context) {
    private val TAG = "TamilTtsService"
    private var mediaPlayer: MediaPlayer? = null
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun speak(
        text: String,
        engine: TamilVoiceEngine = TamilVoiceEngine.MICROSOFT_PALLAVI,
        onDone: () -> Unit = {}
    ) {
        stop()
        if (text.isBlank()) return

        // Clean out JSON or markdown formatting from text before speaking
        val cleanText = sanitizeTextForSpeech(text)
        if (cleanText.isBlank()) return

        currentJob = scope.launch {
            _isSpeaking.value = true
            try {
                val audioFile = when (engine) {
                    TamilVoiceEngine.MICROSOFT_PALLAVI -> {
                        fetchMicrosoftPallaviAudio(cleanText) ?: fetchGoogleTtsAudio(cleanText)
                    }
                    TamilVoiceEngine.GOOGLE_NEURAL -> {
                        fetchGoogleTtsAudio(cleanText) ?: fetchMicrosoftPallaviAudio(cleanText)
                    }
                }

                if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                    withContext(Dispatchers.Main) {
                        playAudioFile(audioFile, onDone)
                    }
                } else {
                    _isSpeaking.value = false
                    withContext(Dispatchers.Main) { onDone() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating TTS: ${e.message}", e)
                _isSpeaking.value = false
                withContext(Dispatchers.Main) { onDone() }
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
        _isSpeaking.value = false
    }

    private fun playAudioFile(file: File, onDone: () -> Unit) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
                setOnCompletionListener {
                    _isSpeaking.value = false
                    it.release()
                    mediaPlayer = null
                    onDone()
                }
                setOnErrorListener { mp, _, _ ->
                    _isSpeaking.value = false
                    mp.release()
                    mediaPlayer = null
                    onDone()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file: ${e.message}", e)
            _isSpeaking.value = false
            onDone()
        }
    }

    /**
     * Microsoft Pallavi Neural Voice via worker proxy or direct neural engine
     */
    private fun fetchMicrosoftPallaviAudio(text: String): File? {
        return try {
            // First try user's ultimate-ai-worker audio endpoint
            val workerUrl = "https://ultimate-ai-worker.haruyhari930.workers.dev/v1/audio/speech"
            val requestBody = JSONObject().apply {
                put("input", text)
                put("voice", "ta-IN-PallaviNeural")
                put("model", "tts-1")
            }.toString()

            val request = Request.Builder()
                .url(workerUrl)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "BeatBoxApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val bytes = response.body!!.bytes()
                if (bytes.size > 500) {
                    val tempFile = File.createTempFile("tts_pallavi_", ".mp3", context.cacheDir)
                    FileOutputStream(tempFile).use { it.write(bytes) }
                    return tempFile
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Worker Pallavi TTS failed, falling back: ${e.message}")
            null
        }
    }

    /**
     * Google Neural Stream Voice (100% Free, High Quality, No Key)
     */
    private fun fetchGoogleTtsAudio(text: String): File? {
        return try {
            val encodedText = URLEncoder.encode(text.take(200), "UTF-8")
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=ta&client=tw-ob&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Referer", "https://translate.google.com/")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val bytes = response.body!!.bytes()
                if (bytes.size > 200) {
                    val tempFile = File.createTempFile("tts_google_", ".mp3", context.cacheDir)
                    FileOutputStream(tempFile).use { it.write(bytes) }
                    return tempFile
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Google TTS failed: ${e.message}")
            null
        }
    }

    private fun sanitizeTextForSpeech(text: String): String {
        return text
            .replace(Regex("(?s)\\{.*?\\}"), "") // Remove JSON
            .replace(Regex("[*#_`~>|]"), "") // Remove Markdown
            .replace(Regex("https?://\\S+"), "") // Remove URLs
            .replace("█", "")
            .trim()
    }
}
