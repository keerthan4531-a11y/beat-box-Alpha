package com.music.echo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SettingsVoice
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.music.echo.ai.GrokAiService
import com.music.echo.ai.SpeechRecognizerManager
import com.music.echo.ai.SpeechState
import com.music.echo.ai.TamilTtsService
import com.music.echo.ai.TamilVoiceEngine
import iad1tya.echo.music.ui.theme.liquidGlass
import iad1tya.echo.music.ui.theme.liquidGlassPressable
import iad1tya.echo.music.ui.theme.neomorphicGlass
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatUiMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onPlayCommand: (String) -> Unit
) {
    val context = LocalContext.current
    var textState by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<ChatUiMessage>()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedEngine by remember { mutableStateOf(TamilVoiceEngine.MICROSOFT_PALLAVI) }
    var isAutoVoiceEnabled by remember { mutableStateOf(true) }
    var showEngineMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val aiService = remember { GrokAiService() }
    val ttsService = remember { TamilTtsService(context) }
    val speechManager = remember { SpeechRecognizerManager(context) }

    val isSpeaking by ttsService.isSpeaking.collectAsState()
    val speechState by speechManager.speechState.collectAsState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            speechManager.startListening(languageCode = "ta-IN") { spokenText ->
                textState = spokenText
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice assistant", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsService.stop()
            speechManager.stopListening()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice Listening Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
            .imePadding()
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "AI Icon",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Inixa Voice AI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            if (selectedEngine == TamilVoiceEngine.MICROSOFT_PALLAVI) "Pallavi Neural (Real Ponnu)" else "Google Neural Stream",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            actions = {
                // Voice Engine Selector Button
                Box {
                    IconButton(onClick = { showEngineMenu = true }) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsVoice,
                            contentDescription = "Select Voice Engine",
                            tint = Color(0xFFFFB74D)
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
                                        fontWeight = if (selectedEngine == engine) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedEngine == engine) MaterialTheme.colorScheme.primary else Color.Unspecified
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

                // Auto-Speak Toggle
                IconButton(onClick = {
                    isAutoVoiceEnabled = !isAutoVoiceEnabled
                    if (!isAutoVoiceEnabled) ttsService.stop()
                }) {
                    Icon(
                        imageVector = if (isAutoVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Auto Voice",
                        tint = if (isAutoVoiceEnabled) Color(0xFF81C784) else Color.Gray
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // Live Voice Status Banner
        AnimatedVisibility(
            visible = speechState is SpeechState.Listening || isSpeaking,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .liquidGlass(cornerRadius = 16.dp, glassAlpha = 0.85f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = "Voice Activity",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier
                        .size(20.dp)
                        .scale(if (isSpeaking || speechState is SpeechState.Listening) pulseScale else 1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSpeaking) "Inixa is speaking in Tamil..." else "Listening to your voice...",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(messages) { index, msg ->
                val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                val isLastAssistant = !msg.isUser && index == messages.size - 1

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .liquidGlass(
                                    cornerRadius = 20.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    glassAlpha = if (msg.isUser) 0.88f else 0.75f,
                                    tintColor = if (msg.isUser) Color(0xFFD97706) else Color(0xFF1E2030)
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.text,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )

                                if (!msg.isUser && msg.text.isNotBlank() && !msg.text.endsWith("█")) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                ttsService.speak(msg.text, selectedEngine)
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Listen to response",
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Listen Voice",
                                            color = Color(0xFFFFB74D),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFB74D),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        val handleSend: () -> Unit = {
            val query = textState.trim()
            if (query.isNotBlank() && !isLoading) {
                speechManager.stopListening()
                messages = messages + ChatUiMessage(query, true)
                textState = ""
                isLoading = true

                coroutineScope.launch {
                    var responseText = ""
                    val responseIndex = messages.size
                    messages = messages + ChatUiMessage("", false)

                    aiService.getGrokResponseStream(query).collect { chunk ->
                        isLoading = false
                        responseText += chunk
                        messages = messages.toMutableList().apply {
                            this[responseIndex] = ChatUiMessage(responseText + " █", false)
                        }
                    }

                    val finalResponse = responseText.trim()
                    messages = messages.toMutableList().apply {
                        this[responseIndex] = ChatUiMessage(finalResponse, false)
                    }

                    // Check for music play commands
                    var musicPlayed = false
                    try {
                        if (finalResponse.startsWith("{") && finalResponse.endsWith("}")) {
                            val json = JSONObject(finalResponse)
                            if (json.has("action") && json.getString("action") == "play") {
                                val songQuery = json.getString("query")
                                val playMsg = "Playing $songQuery for you right now!"
                                messages = messages.toMutableList().apply {
                                    this[responseIndex] = ChatUiMessage(playMsg, false)
                                }
                                if (isAutoVoiceEnabled) {
                                    ttsService.speak(playMsg, selectedEngine) {
                                        onPlayCommand(songQuery)
                                    }
                                } else {
                                    onPlayCommand(songQuery)
                                }
                                musicPlayed = true
                            }
                        }
                    } catch (_: Exception) { }

                    // Auto Speak AI response if enabled and not music playback
                    if (!musicPlayed && isAutoVoiceEnabled && finalResponse.isNotBlank()) {
                        ttsService.speak(finalResponse, selectedEngine)
                    }
                }
            }
        }

        // Bottom Voice & Text Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .liquidGlass(cornerRadius = 28.dp, glassAlpha = 0.85f)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone STT Button
            val isListening = speechState is SpeechState.Listening
            IconButton(
                onClick = {
                    if (isListening) {
                        speechManager.stopListening()
                    } else {
                        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (permission == PackageManager.PERMISSION_GRANTED) {
                            speechManager.startListening(languageCode = "ta-IN") { spokenText ->
                                textState = spokenText
                                handleSend()
                            }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .scale(if (isListening) pulseScale else 1f)
                    .background(
                        color = if (isListening) Color(0xFFE53935) else Color(0x33FFB74D),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListening) Color.White else Color(0xFFFFB74D)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            TextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent),
                placeholder = {
                    Text(
                        if (isListening) "Listening to voice..." else "Type or Speak in Tamil/English...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { handleSend() })
            )

            // Send Button
            IconButton(
                onClick = handleSend,
                modifier = Modifier
                    .background(
                        color = if (textState.isNotBlank()) Color(0xFFFFB74D) else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (textState.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
