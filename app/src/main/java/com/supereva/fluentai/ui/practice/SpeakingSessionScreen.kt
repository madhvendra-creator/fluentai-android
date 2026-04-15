package com.supereva.fluentai.ui.practice
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supereva.fluentai.ui.practice.components.AiAvatarHeader
import kotlinx.coroutines.delay

@Composable
fun SpeakingSessionScreen(
uiState: SessionUiState,
isRecording: Boolean,
onMicClick: () -> Unit,
onCancelClick: () -> Unit,
onStartSession: () -> Unit,
onEndSession: () -> Unit,
onWordClick: (String) -> Unit = {},
onNextQuestion: () -> Unit = {},
onToggleAutocorrect: () -> Unit = {},
onBackClick: () -> Unit = {},
modifier: Modifier = Modifier
) {
var showPleaseWait by remember { mutableStateOf(false) }
val haptic = LocalHapticFeedback.current
var showEmptyError by remember { mutableStateOf(false) }
val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }

DisposableEffect(Unit) {
    onDispose { toneGenerator.release() }
}

LaunchedEffect(showEmptyError) {
    if (showEmptyError) {
        delay(2500)
        showEmptyError = false
    }
}

LaunchedEffect(uiState.isAiSpeaking) {
    if (!uiState.isAiSpeaking) {
        showPleaseWait = false // Reset when AI finishes
    }
}

Box(
modifier = modifier
.fillMaxSize()
.background(
Brush.verticalGradient(
colors = listOf(Color(0xFF2E003E), Color(0xFF000000))
)
)
) {
    // TOP SECTION — nav bar + avatar + timer + autocorrect toggle
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.clickable { /* Switch to chat */ }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go To Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        AiAvatarHeader(
            isSpeaking = uiState.isAiSpeaking,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ongoing call", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        CallTimer()
        Spacer(modifier = Modifier.height(8.dp))
        // Auto-correct Toggle Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (uiState.isAutocorrectEnabled) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
            modifier = Modifier.clickable { onToggleAutocorrect() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = if (uiState.isAutocorrectEnabled) Color(0xFF4CAF50) else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isAutocorrectEnabled) "Auto-Correct: ON" else "Auto-Correct: OFF",
                    color = if (uiState.isAutocorrectEnabled) Color(0xFF4CAF50) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    // BOTTOM SECTION — bubbles + buttons
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- LIVE TRANSCRIPT BUBBLE ---
        val lastMessage = uiState.messages.lastOrNull()
        val conversationalMessage = uiState.messages
            .filter {
                it.role == com.supereva.fluentai.domain.session.model.TurnRole.AI &&
                it.score == null
            }
            .lastOrNull()
        
        // True ONLY if the absolute last message in the list is the new conversational reply
        val isNewAiMessageStreaming = uiState.isAiSpeaking && 
                lastMessage?.role == com.supereva.fluentai.domain.session.model.TurnRole.AI && 
                lastMessage.score == null
        
        // Master control: True until the very first word of the NEW reply arrives
        val isWaitingForAiReply = uiState.isProcessing || uiState.isAiThinking || 
                (uiState.isAiSpeaking && !isNewAiMessageStreaming)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TOP BOX: ALWAYS show the AI's challenge/transcript if it exists
            if (conversationalMessage != null && conversationalMessage.transcript.isNotBlank()) {
                val challengeText = conversationalMessage.transcript.trim()
                val displayText = if (
                    challengeText.contains("\n\n") &&
                    (challengeText.startsWith("That is incorrect") ||
                     challengeText.startsWith("Incorrect!") ||
                     challengeText.startsWith("That is correct") ||
                     challengeText.startsWith("Great job!"))
                ) {
                    challengeText.substringAfter("\n\n").trim()
                } else {
                    challengeText
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                        .padding(vertical = 16.dp, horizontal = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = displayText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. CORRECT ANSWER BUBBLE: Show after evaluation (only when autocorrect is ON)
            if (uiState.lastCorrectAnswer != null && uiState.isAutocorrectEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B5E20), RoundedCornerShape(24.dp))
                        .padding(vertical = 14.dp, horizontal = 24.dp)
                ) {
                    Text(
                        text = "✅ Correct answer: ${uiState.lastCorrectAnswer}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 3. BOTTOM BOX: Show the live status or user speech
            val (statusText, statusColor, statusBgColor) = when {
                // Live User Speech (Words detected)
                isRecording && uiState.partialTranscript.isNotEmpty() -> 
                    Triple(uiState.partialTranscript, Color.White, Color(0xFF3E2723))
                
                // Mic is on, listening
                isRecording && uiState.partialTranscript.isEmpty() -> 
                    Triple("Listening...", Color(0xFF4CAF50), Color.Transparent)
                
                // Error State
                uiState.error != null -> 
                    Triple("⚠️ ${uiState.error}", Color(0xFFFF5252), Color(0xFF2C1B1B))
                
                // Waiting for AI
                isWaitingForAiReply -> 
                    Triple("Thinking...", Color.White.copy(alpha = 0.7f), Color.Transparent)
                
                // If we already showed the AI challenge above and have nothing else to show, stay blank
                conversationalMessage != null -> 
                    Triple("", Color.Transparent, Color.Transparent)
                
                // Fallback
                else -> 
                    Triple("Tap to speak", Color.White.copy(alpha = 0.5f), Color.Transparent)
            }

            if (statusText.isNotEmpty()) {
                if (statusBgColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusBgColor, RoundedCornerShape(24.dp))
                            .padding(vertical = 16.dp, horizontal = 24.dp)
                    ) {
                        Text(
                            text = statusText.trim(),
                            color = statusColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    Text(statusText, color = statusColor, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        // --- BUTTON ROW ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly, // Space items evenly
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Trash Button (VISIBLE ONLY WHEN RECORDING) // This allows the user to cancel/delete while speaking.
            if (isRecording) {
                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = Color(0xFFFF5252))
                }
            } else {
                // HIDDEN when Idle (Orange Mic) or AI Speaking
                Spacer(modifier = Modifier.size(50.dp))
            }
            // 2. Main Mic Button
            if (isWaitingForAiReply) {
                Box(
                    modifier = Modifier.size(80.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }else if (uiState.isWaitingForNextQuestion) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
                            )
                        )
                        .clickable { onNextQuestion() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Next Question →",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else {
                CallActionButton(
                    isRecording = isRecording,
                    isAiSpeaking = uiState.isAiSpeaking,
                    onClick = {
                        if (uiState.isAiSpeaking) {
                            showPleaseWait = true // Show warning, do NOT trigger mic
                        } else if (isRecording) {
                            if (uiState.partialTranscript.isBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showEmptyError = true
                                // 🟢 REMOVED onCancelClick() - Mic stays Green!
                            } else {
                                onMicClick()
                            }
                        } else {
                            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                            onMicClick() // Turn Orange -> Green
                        }
                    }
                )
            }
            // 3. Close Button (Always visible)
            IconButton(
                onClick = onEndSession,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "End", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val hintText = when {
            uiState.isWaitingForNextQuestion -> "Tap to continue to next question"
            showEmptyError -> "We could not detect any speech"
            showPleaseWait && uiState.isAiSpeaking -> "Please wait"
            uiState.isAiSpeaking -> "" // Keep blank while AI speaks
            isRecording -> "Tap again to submit"
            else -> ""
        }

        Text(
            text = hintText,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}
}

@Composable
fun CallTimer() {
var seconds by remember { mutableLongStateOf(0L) }
LaunchedEffect(Unit) {
while (true) {
delay(1000)
seconds++
}
}
val formatted = String.format("%02d:%02d", seconds / 60, seconds % 60)
Text(text = formatted, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
}

@Composable
fun CallActionButton(
    isRecording: Boolean,
    isAiSpeaking: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isRecording -> Color(0xFF00C853)
        isAiSpeaking -> Color.White.copy(alpha = 0.15f)
        else -> Color(0xFF8B8B8B).copy(alpha = 0.35f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic")

    val outerPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer"
    )

    val innerPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring — only when recording
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(outerPulse)
                    .clip(CircleShape)
                    .background(Color(0xFF00C853).copy(alpha = 0.25f))
            )
        }

        // Main button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .scale(if (isRecording) innerPulse else 1f)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable { onClick() }
        ) {
            if (isRecording) {
                // Animated waveform bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0, 150, 75).forEachIndexed { index, offsetMs ->
                        val barHeight by infiniteTransition.animateFloat(
                            initialValue = 8f,
                            targetValue = 28f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400 + index * 100, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                                initialStartOffset = StartOffset(offsetMs)
                            ),
                            label = "bar$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
