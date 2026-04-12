package com.supereva.fluentai.ui.home

/**
 * UI-layer display model for a conversation topic.
 * Mapped from the domain [Topic] by [HomeViewModel].
 */
data class TopicUiModel(
    val id: String,
    val title: String,
    val duration: String,
    val difficulty: String,
    val imageUrl: String,
    val firstQuestion: String,
    @androidx.annotation.DrawableRes val imageResId: Int? = null
)
