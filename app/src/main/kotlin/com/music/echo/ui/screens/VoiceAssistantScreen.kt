package com.music.echo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SettingsVoice
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.music.echo.ai.GrokAiService
import com.music.echo.ai.SpeechRecognizerManager
import com.music.echo.ai.SpeechState
import com.music.echo.ai.TamilTtsService
import com.music.echo.ai.TamilVoiceEngine
import com.music.echo.ui.component.visualizer.AssistantVoiceState
import com.music.echo.ui.component.visualizer.VoiceAssistantOrb
import iad1tya.echo.music.ui.theme.liquidGlass
import kotlinx.coroutines.launch
import org.json.JSONObject

data class AssistantRecommendation(
    val title: String,
    val subtitle: String,
    val query: String
)

/**
 * Modern Siri / Gemini Inspired Voice Assistant Screen
 * Features deep purple/navy gradient, dynamic animated voice orb, streaming typography,
 * and rich song recommendation cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    onPlayCommand: (String) -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var voiceState by remember { mutableStateOf(AssistantVoiceState.IDLE) }
    var userTranscription by remember { mutableStateOf("") }
    var assistantResponse by remember { mutableStateOf("") }
    var recommendations by remember { mutableStateOf(listOf<AssistantRecommendation>()) }

    var selectedEngine by remember { mutableStateOf(TamilVoiceEngine.MICROSOFT_PALLAVI) }
    var isAutoVoiceEnabled by remember { mutableStateOf(true) }
    var showEngineMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val aiService = remember { GrokAiService() }
    val ttsService = remember { TamilTtsService(context) }
    val speechManager = remember { SpeechRecognizerManager(context) }

    val isSpeaking by ttsService.isSpeaking.collectAsState()
    val speechState by speechManager.speechState.collectAsState()

    // Sync VoiceState with SpeechState and TTS
    LaunchedEffect(speechState, isSpeaking) {
        voiceState = when {
            speechState is SpeechState.Listening -> AssistantVoiceState.LISTENING
            isSpeaking -> AssistantVoiceState.RESPONDING
            voiceState == AssistantVoiceState.THINKING -> AssistantVoiceState.THINKING
            else -> AssistantVoiceState.IDLE
        }
    }

    val executePrompt: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            voiceState = AssistantVoiceState.THINKING
            userTranscription = query
            assistantResponse = ""
            recommendations = emptyList()

            coroutineScope.launch {
                var streamedText = ""
                aiService.getGrokResponseStream(query).collect { chunk ->
                    voiceState = AssistantVoiceState.RESPONDING
                    streamedText += chunk
                    assistantResponse = streamedText + " █"
                }

                val finalOutput = streamedText.trim()
                assistantResponse = finalOutput

                // Parse action
                var isAction = false
                try {
                    if (finalOutput.startsWith("{") && finalOutput.endsWith("}")) {
                        val json = JSONObject(finalOutput)
                        if (json.has("action") && json.getString("action") == "play") {
                            val songName = json.getString("query")
                            val confirmVoice = "Playing $songName for you!"
                            assistantResponse = confirmVoice
                            recommendations = listOf(
                                AssistantRecommendation(
                                    title = songName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    subtitle = "Tap to play song instantly",
                                    query = songName
                                )
                            )
                            if (isAutoVoiceEnabled) {
                                ttsService.speak(confirmVoice, selectedEngine) {
                                    onPlayCommand(songName)
                                }
                            } else {
                                onPlayCommand(songName)
                            }
                            isAction = true
                        }
                    }
                } catch (_: Exception) {}

                if (!isAction && isAutoVoiceEnabled && finalOutput.isNotBlank()) {
                    ttsService.speak(finalOutput, selectedEngine)
                }
            }
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechManager.startListening("ta-IN") { recognized ->
                userTranscription = recognized
                executePrompt(recognized)
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice assistant", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleListening: () -> Unit = {
        if (speechState is SpeechState.Listening) {
            speechManager.stopListening()
        } else {
            ttsService.stop()
            val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permission == PackageManager.PERMISSION_GRANTED) {
                speechManager.startListening("ta-IN") { recognized ->
                    userTranscription = recognized
                    executePrompt(recognized)
                }
            } else {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsService.stop()
            speechManager.stopListening()
        }
    }

    // Modern Deep Purple to Obsidian Navy Gradient
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E0A3C), // Deep Purple
            Color(0xFF0F0E2A), // Dark Violet
            Color(0xFF070A18)  // Obsidian Navy
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Voice Assistant",
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Inixa Voice",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            if (selectedEngine == TamilVoiceEngine.MICROSOFT_PALLAVI) "Pallavi Neural (Real Ponnu)" else "Google Neural Stream",
                            color = Color(0xFF00F5FF),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Engine Switcher
                    Box {
                        IconButton(onClick = { showEngineMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsVoice,
                                contentDescription = "Voice Settings",
                                tint = Color(0xFF00F5FF)
                            )
                        }

                        DropdownMenu(
                            expanded = showEngineMenu,
                            onDismissRequest = { showEngineMenu = false }
                        ) {
                            TamilVoiceEngine.values().forEach { engine ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            engine.displayName,
                                            fontWeight = if (selectedEngine == engine) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedEngine = engine
                                        showEngineMenu = false
                                        Toast.makeText(context, "Voice: ${engine.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Auto-voice toggle
                    IconButton(onClick = {
                        isAutoVoiceEnabled = !isAutoVoiceEnabled
                        if (!isAutoVoiceEnabled) ttsService.stop()
                    }) {
                        Icon(
                            imageVector = if (isAutoVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Auto Voice Toggle",
                            tint = if (isAutoVoiceEnabled) Color(0xFF00F5FF) else Color.Gray
                        )
                    }

                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Scrollable Content Area: Typography & Recommendation Cards
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // User Spoken Text (Transcription)
                if (userTranscription.isNotBlank()) {
                    item {
                        Text(
                            text = "\"$userTranscription\"",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }
                }

                // Assistant Streaming Text Display
                if (assistantResponse.isNotBlank()) {
                    item {
                        Text(
                            text = assistantResponse,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                } else if (userTranscription.isBlank()) {
                    item {
                        Text(
                            text = when (voiceState) {
                                AssistantVoiceState.LISTENING -> "Listening to your voice..."
                                AssistantVoiceState.THINKING -> "Thinking..."
                                AssistantVoiceState.RESPONDING -> "Speaking..."
                                AssistantVoiceState.IDLE -> "Tap the Orb to Speak\nin Tamil or English"
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }

                // Rich Content Cards (Recommendations / Songs)
                if (recommendations.isNotEmpty()) {
                    items(recommendations) { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .liquidGlass(cornerRadius = 24.dp, glassAlpha = 0.85f)
                                .clickable { onPlayCommand(rec.query) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F5FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec.subtitle,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Center Glowing Siri/Gemini Dynamic Voice Orb
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                VoiceAssistantOrb(
                    state = voiceState,
                    size = 140.dp,
                    onClick = toggleListening
                )
            }
        }
    }
}
