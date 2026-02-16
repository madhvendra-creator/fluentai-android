package com.supereva.fluentai.domain.model

/**
 * The AI's conversational reply to continue the practice dialogue.
 *
 * Separate from correction/feedback — this is a natural follow-up
 * question or comment to keep the conversation going.
 */
data class ConversationReply(
    val aiText: String
)
