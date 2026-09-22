package com.music.echo.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class AiEndpoint(val url: String, val model: String, val authHeader: String = "")

data class ChatMessage(
    val role: String,
    val content: String
)

class GrokAiService {
    private val TAG = "GrokAiService"

    // Standard Android OkHttpClient without TLS cipher restrictions
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val endpoints = listOf(
        AiEndpoint(
            url = "https://ultimate-ai-worker.haruyhari930.workers.dev/v1/chat/completions",
            model = "gpt-5.4-mini"
        ),
        AiEndpoint(
            url = "https://ultimate-ai-worker.haruyhari930.workers.dev/v1/chat/completions",
            model = "minitool/gpt-5.4-mini"
        ),
        AiEndpoint(
            url = "https://ultimate-ai-worker.haruyhari930.workers.dev/chat/completions",
            model = "gpt-5.4-mini"
        ),
        AiEndpoint(
            url = "https://ultimate-ai-worker.haruyhari930.workers.dev/v1/chat/completions",
            model = "minitool/gpt-4.1-mini"
        ),
        AiEndpoint(
            url = "https://ultimate-ai-worker.haruyhari930.workers.dev/v1/chat/completions",
            model = "minitool/grok-4.5"
        )
    )

    fun getGrokResponseStream(userText: String): Flow<String> = flow {
        val systemPrompt = ChatMessage(
            role = "system",
            content = """
                You are Inixa AI, an intelligent music assistant embedded within a premium Kotlin-based Android music player.
                You must act friendly, witty, concise, and helpful.
                You have control over the app's player.
                If the user asks you to play a song, play music, play an artist, or search for a track, you MUST respond ONLY with a raw JSON command that the app will parse.
                For example, if the user says 'play monica song' or 'play some anirudh songs', respond exactly with:
                {"action": "play", "query": "monica"}
                If the user is just chatting normally, answer them warmly in their language. Do not output JSON unless it's a play command.
            """.trimIndent()
        )

        val userMessage = ChatMessage(role = "user", content = userText)
        val messages = listOf(systemPrompt, userMessage)

        var success = false
        for (endpoint in endpoints) {
            try {
                // First try streaming
                streamFromEndpoint(endpoint, messages, this)
                success = true
                break
            } catch (streamError: Exception) {
                Log.w(TAG, "Streaming failed for ${endpoint.url} with ${endpoint.model}: ${streamError.message}. Trying non-streaming fallback...")
                try {
                    val fullResponse = fetchNonStreaming(endpoint, messages)
                    if (fullResponse.isNotBlank()) {
                        emit(fullResponse)
                        success = true
                        break
                    }
                } catch (fallbackError: Exception) {
                    Log.e(TAG, "Non-streaming fallback failed for ${endpoint.url} with ${endpoint.model}: ${fallbackError.message}")
                    continue
                }
            }
        }

        if (!success) {
            emit("\nUnable to connect to AI server. Please check your internet connection.")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun streamFromEndpoint(
        endpoint: AiEndpoint,
        messages: List<ChatMessage>,
        flowCollector: kotlinx.coroutines.flow.FlowCollector<String>
    ) {
        val jsonPayload = JSONObject().apply {
            put("model", endpoint.model)
            put("stream", true)
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
        }.toString()

        val requestBody = jsonPayload.toByteArray(StandardCharsets.UTF_8).toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(endpoint.url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw Exception("API Error: ${response.code} - $errorBody")
            }

            var emittedAny = false
            response.body?.source()?.let { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        try {
                            val chunk = JSONObject(data)
                            val content = chunk
                                .optJSONArray("choices")
                                ?.optJSONObject(0)
                                ?.optJSONObject("delta")
                                ?.optString("content", "") ?: ""

                            if (content.isNotEmpty()) {
                                flowCollector.emit(content)
                                emittedAny = true
                            }
                        } catch (_: Exception) {
                            continue
                        }
                    }
                }
            }

            if (!emittedAny) {
                throw Exception("No streaming chunks received")
            }
        }
    }

    private fun fetchNonStreaming(
        endpoint: AiEndpoint,
        messages: List<ChatMessage>
    ): String {
        val jsonPayload = JSONObject().apply {
            put("model", endpoint.model)
            put("stream", false)
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
        }.toString()

        val requestBody = jsonPayload.toByteArray(StandardCharsets.UTF_8).toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(endpoint.url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Non-streaming API Error: ${response.code}")
            }
            val bodyString = response.body?.string().orEmpty()
            val json = JSONObject(bodyString)
            return json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                .orEmpty()
        }
    }
}
