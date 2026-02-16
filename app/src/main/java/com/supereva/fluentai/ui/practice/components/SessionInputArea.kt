package com.supereva.fluentai.ui.practice.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dynamic bottom input area whose appearance changes based on session state.
 *
 * All composables here are **stateless** — they receive flags and fire
 * event lambdas. Animations are declarative Compose animations.
 */
@Composable
fun SessionInputArea(
    isSessionActive: Boolean,
    isListening: Boolean,
    isProcessing: Boolean,
    isAiSpeaking: Boolean,
    isCompleted: Boolean,
    isRecording: Boolean,
    currentVolume: Float = 0f,
    error: String?,
    onMicClick: () -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp)
        ) {
            when {
                isCompleted -> CompletedArea()

                error != null -> ErrorArea(
                    message = error,
                    onRetry = onStartSession
                )

                !isSessionActive -> IdleStartArea(onStartSession = onStartSession)

                isProcessing -> ProcessingArea()

                isAiSpeaking -> AiSpeakingArea()

                isListening || isSessionActive -> ListeningArea(
                    isRecording = isRecording,
                    currentVolume = currentVolume,
                    onMicClick = onMicClick,
                    onEndSession = onEndSession
                )
            }
        }
    }
}

// ── Idle: no session active ─────────────────────────────────────────────

@Composable
private fun IdleStartArea(onStartSession: () -> Unit) {
    Button(
        onClick = onStartSession,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(56.dp)
    ) {
        Text(
            text = "🎤  Start Speaking",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Listening: pulsing mic ──────────────────────────────────────────────

@Composable
private fun ListeningArea(
    isRecording: Boolean,
    currentVolume: Float,
    onMicClick: () -> Unit,
    onEndSession: () -> Unit
) {
    // Volume-based scale animation
    // SpeechRecognizer RMS typically -2..10. We normalize for visual scale.
    // Ensure positive and clamp
    val targetScale = if (isRecording) {
        // Base scale 1.0 + volume factor
        // Assuming volume is rmsDb roughly 0-10 visible range
        1f + (currentVolume.coerceAtLeast(0f) / 15f).coerceIn(0f, 1.5f)
    } else {
        1f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "volumeScale"
    )

    // Secondary ring
    val ring2Scale by animateFloatAsState(
        targetValue = if (isRecording) targetScale * 1.3f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
        label = "ring2Scale"
    )

    val ringAlpha = if (isRecording) 0.3f else 0f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        if (!isRecording) {
            Text(
                text = "Tap to speak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            // Waveform bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(40.dp)
            ) {
                // Normalize volume (0..10 typically) to 0..1 scale
                val normalizedVolume = (currentVolume.coerceAtLeast(0f) / 10f).coerceIn(0.1f, 1f)
                val animatedVolume by animateFloatAsState(targetValue = normalizedVolume, label = "vol")

                val barCount = 5
                repeat(barCount) { index ->
                    val maxBarHeight = 32f
                    val minBarHeight = 4f
                    
                    // Middle bars grow more
                    val scaleFactor = when(index) {
                        0, 4 -> 0.3f
                        1, 3 -> 0.6f
                        else -> 1.0f
                    }
                    
                    val height = minBarHeight + (maxBarHeight * animatedVolume * scaleFactor)
                    
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(height.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Mic button with pulse rings
        Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
                // Pulse ring 1 (inner)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(animatedScale)
                        .alpha(ringAlpha)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
                // Pulse ring 2 (outer/delayed)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(ring2Scale)
                        .alpha(ringAlpha * 0.7f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }

            // Mic button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isRecording)
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.error,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            )
                        else
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            )
                    )
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) "⏹" else "🎤",
                    fontSize = 32.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onEndSession) {
            Text(
                text = "End Session",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Processing: analyzing speech ────────────────────────────────────────

@Composable
private fun ProcessingArea() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Analyzing your speech…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── AI Speaking: waveform bars ──────────────────────────────────────────

@Composable
private fun AiSpeakingArea() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val barCount = 5
    val barHeights = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = 32f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    delayMillis = index * 100,
                    easing = EaseInOut
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "AI is speaking…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEach { animatedHeight ->
                val height by animatedHeight
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(height.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

// ── Completed ───────────────────────────────────────────────────────────

@Composable
private fun CompletedArea() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "🎉",
            fontSize = 40.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ── Error ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorArea(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "⚠️ $message",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
