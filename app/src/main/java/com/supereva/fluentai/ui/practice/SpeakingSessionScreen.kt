package com.supereva.fluentai.ui.practice

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.supereva.fluentai.domain.session.model.TurnRole
import com.supereva.fluentai.ui.practice.components.AiMessageBubble
import com.supereva.fluentai.ui.practice.components.CorrectionBubble
import com.supereva.fluentai.ui.practice.components.SessionInputArea
import com.supereva.fluentai.ui.practice.components.SessionTopBar
import com.supereva.fluentai.ui.practice.components.UserMessageBubble

/**
 * Fully **stateless** immersive speaking-practice screen.
 *
 * Receives [SessionUiState] and event lambdas — owns zero state itself.
 * The parent [PracticeScreen] provides the ViewModel, recorder, and
 * permissions logic.
 *
 * **Rendering rules:**
 * - Each [SessionTurn] is dispatched to [UserMessageBubble] or
 *   [AiMessageBubble] based on role.
 * - A [CorrectionBubble] is shown below user turns when correctedText
 *   differs from transcript.
 * - New turns animate into the list via [AnimatedVisibility].
 * - Stable keys use `index_role_timestamp` so streaming AI updates
 *   recompose only the last item.
 */
@Composable
fun SpeakingSessionScreen(
    uiState: SessionUiState,
    isRecording: Boolean,
    onMicClick: () -> Unit,
    onStartSession: () -> Unit,
    onEndSession: () -> Unit,
    onWordClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll when messages arrive or streaming content grows
    val messages = uiState.messages
    val lastMessage = messages.lastOrNull()
    LaunchedEffect(messages.size, lastMessage?.transcript) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            SessionTopBar(
                topicId = uiState.topicId,
                turnCount = uiState.turnCount,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            SessionInputArea(
                isSessionActive = uiState.isSessionActive,
                isListening = uiState.isListening,
                isProcessing = uiState.isProcessing,
                isAiSpeaking = uiState.isAiSpeaking,
                isCompleted = uiState.isCompleted,
                isRecording = isRecording,
                currentVolume = uiState.currentVolume,
                error = uiState.error,
                onMicClick = onMicClick,
                onStartSession = onStartSession,
                onEndSession = onEndSession
            )
        },
        modifier = modifier
    ) { innerPadding ->

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = messages,
                key = { index, turn -> "${index}_${turn.role}_${turn.timestamp}" }
            ) { _, turn ->
                // Animate each turn into the list
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) +
                            fadeIn()
                ) {
                    Column {
                        when (turn.role) {
                            TurnRole.USER -> {
                                UserMessageBubble(turn = turn)
                                // Show correction card below the user bubble
                                // when the AI has corrected the text
                                if (!turn.isStreaming &&
                                    turn.correctedText != turn.transcript &&
                                    turn.correctedText.isNotEmpty()
                                ) {
                                    CorrectionBubble(turn = turn)
                                }
                            }

                            TurnRole.AI -> {
                                AiMessageBubble(
                                    turn = turn,
                                    onWordClick = onWordClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
