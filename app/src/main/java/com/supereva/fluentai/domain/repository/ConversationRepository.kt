package com.supereva.fluentai.domain.repository

import com.supereva.fluentai.domain.model.ConversationReply
import com.supereva.fluentai.domain.session.model.SessionTurn

/**
 * Generates a conversational AI follow-up after each user turn.
 *
 * The reply is a **separate turn** from the correction/feedback —
 * it continues the practice dialogue naturally based on
 * the topic, what the user said, and the conversation history.
 *
 * No Android dependencies — pure domain contract.
 */
interface ConversationRepository {

    /**
     * Generate a natural follow-up reply as a stream of text chunks.
     *
     * @param sessionId      The ID of the current session.
     * @param topicId        Current practice topic (e.g., "job_interview").
     * @param userText       The user's original spoken text.
     * @param correctedText  Grammar-corrected version of [userText].
     */
    suspend fun streamReply(
        sessionId: String,
        topicId: String,
        userText: String,
        correctedText: String,
        isAutocorrectEnabled: Boolean
    ): kotlinx.coroutines.flow.Flow<String>
}
