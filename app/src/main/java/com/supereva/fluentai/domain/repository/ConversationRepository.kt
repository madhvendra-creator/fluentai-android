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
     * Generate a natural follow-up reply.
     *
     * @param topicId        Current practice topic (e.g., "job_interview").
     * @param userText       The user's original spoken text.
     * @param correctedText  Grammar-corrected version of [userText].
     * @param sessionHistory Full conversation so far for context.
     */
    suspend fun generateReply(
        topicId: String,
        userText: String,
        correctedText: String,
        sessionHistory: List<SessionTurn>
    ): ConversationReply
}
