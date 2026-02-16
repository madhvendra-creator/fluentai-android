package com.supereva.fluentai.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.supereva.fluentai.data.repository.FakeTopicRepository
import com.supereva.fluentai.di.SessionServiceLocator
import com.supereva.fluentai.domain.model.Topic
import com.supereva.fluentai.domain.session.SpeakingSessionCoordinator
import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.usecase.GetTopicsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * Loads topics via [GetTopicsUseCase] and maps domain models
 * to [TopicUiModel] for display. Handles [HomeAction]s by
 * starting a real [SpeakingSession] through the coordinator
 * before emitting a one-shot [HomeNavigationEvent].
 *
 * The [coordinator] is an **app-scoped singleton** so the
 * session created here survives navigation to the session screen.
 */
class HomeViewModel(
    private val coordinator: SpeakingSessionCoordinator
) : ViewModel() {

    private val useCase = GetTopicsUseCase(
        topicRepository = FakeTopicRepository()
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = Channel<HomeNavigationEvent>(Channel.BUFFERED)

    /** One-shot navigation events. Collect inside a `LaunchedEffect`. */
    val navigationEvent: Flow<HomeNavigationEvent> = _navigationEvent.receiveAsFlow()

    init {
        loadTopics()
    }

    // ── Public API ──────────────────────────────────────────────────────

    /**
     * Handle a user action from the Home screen.
     *
     * Creates a session via the coordinator, then emits a
     * [HomeNavigationEvent.NavigateToSession] so the UI layer
     * can navigate without holding business logic.
     */
    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.StartAiPractice -> startSession(
                topicId = "free_talk",
                difficulty = Difficulty.BEGINNER
            )
            is HomeAction.OpenTopic -> {
                val difficulty = resolveDifficulty(action.topicId)
                startSession(
                    topicId = action.topicId,
                    difficulty = difficulty
                )
            }
        }
    }

    // ── Internals ───────────────────────────────────────────────────────

    private fun startSession(topicId: String, difficulty: Difficulty) {
        coordinator.ensureFreshSession(topicId, difficulty, viewModelScope)
        viewModelScope.launch {
            _navigationEvent.send(HomeNavigationEvent.NavigateToSession(topicId))
        }
    }

    /**
     * Look up a topic's difficulty from the current [HomeUiState].
     * Falls back to [Difficulty.BEGINNER] if the topic isn't found.
     */
    private fun resolveDifficulty(topicId: String): Difficulty {
        val difficultyStr = _uiState.value.topicCategories
            .values
            .flatten()
            .firstOrNull { it.id == topicId }
            ?.difficulty

        return try {
            difficultyStr?.let { Difficulty.valueOf(it.uppercase()) }
                ?: Difficulty.BEGINNER
        } catch (_: IllegalArgumentException) {
            Difficulty.BEGINNER
        }
    }

    private fun loadTopics() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            try {
                val categories = useCase()
                val uiCategories = categories.mapValues { (_, topics) ->
                    topics.map { it.toUiModel() }
                }
                _uiState.value = HomeUiState(
                    isLoading = false,
                    topicCategories = uiCategories
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load topics"
                )
            }
        }
    }

    private fun Topic.toUiModel() = TopicUiModel(
        id = id,
        title = title,
        duration = "${durationMinutes} min",
        difficulty = difficulty.name.lowercase()
            .replaceFirstChar { it.uppercase() },
        imageUrl = imageUrl
    )

    // ── Factory ─────────────────────────────────────────────────────────

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        coordinator = SessionServiceLocator.coordinator
                    ) as T
                }
            }
    }
}
