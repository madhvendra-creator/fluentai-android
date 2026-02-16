package com.supereva.fluentai.ui.home

/**
 * Home screen UI state exposed by [HomeViewModel].
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val topicCategories: Map<String, List<TopicUiModel>> = emptyMap(),
    val error: String? = null
)
