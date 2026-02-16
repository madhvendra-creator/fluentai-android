package com.supereva.fluentai.ui.practice.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supereva.fluentai.domain.session.model.SessionTurn

/**
 * Left-aligned chat bubble for the **AI's** response.
 *
 * Supports two visual modes:
 * - **Streaming** (`isStreaming = true`): partial text grows live,
 *   pulsing typing dots shown at the bottom.
 * - **Final** (`isStreaming = false`): full response with optional
 *   feedback and score badge.
 *
 * Stateless — receives a [SessionTurn] snapshot from the coordinator.
 */
@Composable
fun AiMessageBubble(
    turn: SessionTurn,
    onWordClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Partial or final transcript with clickable words
                val styledText = androidx.compose.ui.text.buildAnnotatedString {
                    val text = turn.transcript
                    // Split by whitespace but keep delimiter logic simple for now
                    // A proper tokenizer is better but split(" ") suffices for basic needs
                    val words = text.split(" ")
                    words.forEachIndexed { index, word ->
                        pushStringAnnotation(tag = "WORD", annotation = word)
                        append(word)
                        pop()
                        if (index < words.size - 1) append(" ")
                    }
                }

                androidx.compose.foundation.text.ClickableText(
                    text = styledText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    onClick = { offset ->
                        // Don't trigger if streaming (text is moving target)
                        if (!turn.isStreaming) {
                            styledText.getStringAnnotations(tag = "WORD", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    // Remove punctuation for cleaner TTS
                                    val cleanWord = annotation.item.trim { !it.isLetterOrDigit() }
                                    if (cleanWord.isNotEmpty()) {
                                        onWordClick(cleanWord)
                                    }
                                }
                        }
                    }
                )

                // Streaming indicator
                if (turn.isStreaming) {
                    Spacer(modifier = Modifier.height(6.dp))
                    StreamingDotsIndicator()
                }

                // Feedback (only when complete)
                if (!turn.isStreaming && turn.feedback.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 ${turn.feedback}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                            .copy(alpha = 0.7f)
                    )
                }

                // Score (only when complete)
                if (!turn.isStreaming) {
                    turn.score?.let { score ->
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreBadge(score = score)
                    }
                }
            }
        }
    }
}

// ── Streaming dots ──────────────────────────────────────────────────────

/**
 * Three pulsing dots indicating the AI is still streaming.
 */
@Composable
internal fun StreamingDotsIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "streamDots")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200,
                        easing = EaseInOut
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary)
            )
        }
    }
}

// ── Score badge ─────────────────────────────────────────────────────────

/**
 * Colour-coded score chip: green ≥ 80, amber ≥ 60, red < 60.
 */
@Composable
internal fun ScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> MaterialTheme.colorScheme.tertiary
        score >= 60 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "⭐ $score / 100",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
