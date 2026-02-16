package com.supereva.fluentai.ui.practice.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supereva.fluentai.domain.session.model.SessionTurn

/**
 * Inline correction card shown **below** a user bubble when the
 * AI's corrected text differs from the user's transcript.
 *
 * Displays:
 * - Original text with strikethrough in error colour
 * - Corrected text in primary colour
 *
 * Stateless — receives a [SessionTurn] and checks
 * `correctedText != transcript`.
 */
@Composable
fun CorrectionBubble(
    turn: SessionTurn,
    modifier: Modifier = Modifier
) {
    // Guard: only render when there IS a meaningful correction
    if (turn.correctedText == turn.transcript || turn.correctedText.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            tonalElevation = 0.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "✏️ Correction",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.error,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 13.sp
                            )
                        ) {
                            append(turn.transcript)
                        }
                        append("\n")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        ) {
                            append("✓ ${turn.correctedText}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Spacing to align with user avatar column
        Spacer(modifier = Modifier.width(40.dp))
    }
}
