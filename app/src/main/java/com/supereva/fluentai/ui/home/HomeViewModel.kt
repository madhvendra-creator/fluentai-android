package com.supereva.fluentai.ui.home

import kotlinx.coroutines.Dispatchers
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Warm up Railway container
                SessionServiceLocator.okHttpClient.newCall(
                    okhttp3.Request.Builder()
                        .url("https://fluentai-backend-production-6a57.up.railway.app/health")
                        .build()
                ).execute().close()
            } catch (e: Exception) { /* silent warm-up, ignore errors */ }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Pre-fetch JWT so it is cached before practice screen opens
                SessionServiceLocator.authManager.authenticateDevice()
            } catch (e: Exception) { /* silent, will retry when needed */ }
        }

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
                firstQuestion = "Hi! Let's practice speaking. Tell me about whatever is on your mind today.",
                difficulty = Difficulty.BEGINNER
            )
            is HomeAction.OpenTopic -> {
                val difficulty = resolveDifficulty(action.topicId)
                val question = _uiState.value.topicCategories.values.flatten()
                    .firstOrNull { it.id == action.topicId }?.firstQuestion ?: "Hi! Let's start practicing."
                startSession(
                    topicId = action.topicId,
                    firstQuestion = question,
                    difficulty = difficulty
                )
            }
        }
    }

    // ── Internals ───────────────────────────────────────────────────────

    private fun startSession(topicId: String, firstQuestion: String, difficulty: Difficulty) {
        coordinator.ensureFreshSession(topicId, firstQuestion, difficulty, viewModelScope)
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
        imageUrl = imageUrl,
        firstQuestion = firstQuestion,
        imageResId = imageResId ?: when {
            id == "ji_02" || title.contains("Strength", ignoreCase = true) -> com.supereva.fluentai.R.drawable.strength
            id == "ji_03" || title.contains("Behavioral", ignoreCase = true) || title.contains("Behavioural", ignoreCase = true) -> com.supereva.fluentai.R.drawable.behaviour
            id == "ji_04" || title.contains("Salary", ignoreCase = true) -> com.supereva.fluentai.R.drawable.salary
            else -> com.supereva.fluentai.R.drawable.introductionyourself
        }
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
