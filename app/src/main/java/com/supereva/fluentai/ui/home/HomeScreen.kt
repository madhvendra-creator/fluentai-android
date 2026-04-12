package com.supereva.fluentai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supereva.fluentai.ui.home.components.AiPracticeCard
import com.supereva.fluentai.ui.home.components.RealPeopleCard
import com.supereva.fluentai.ui.home.components.TopicRow

/**
 * Stateful wrapper that owns the [HomeViewModel].
 *
 * Collects one-shot [HomeNavigationEvent]s from the ViewModel
 * and delegates rendering to [HomeScreenContent].
 * No business logic lives here — all actions are forwarded
 * to the ViewModel via [HomeAction].
 */
@Composable
fun HomeScreen(
    onNavigateToSession: (topicId: String) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()

    // Collect one-shot navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is HomeNavigationEvent.NavigateToSession ->
                    onNavigateToSession(event.topicId)
            }
        }
    }

    HomeScreenContent(
        uiState = uiState,
        onStartPractice = { viewModel.onAction(HomeAction.StartAiPractice) },
        onTopicClick = { topicId -> viewModel.onAction(HomeAction.OpenTopic(topicId)) }
    )
}

/**
 * Stateless Home screen composable.
 *
 * Layout:
 * 1. AI Practice banner card
 * 2. Topic rows per category (horizontal scrolling)
 */
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onStartPractice: () -> Unit,
    onTopicClick: (topicId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️ ${uiState.error}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {


                // AI Practice banner
                item {
                    AiPracticeCard(
                        onStartClick = onStartPractice
                    )
                }

                item {
                    RealPeopleCard()
                }

                // Topic category rows
                items(
                    items = uiState.topicCategories.entries.toList(),
                    key = { it.key }
                ) { (category, topics) ->
                    TopicRow(
                        category = category,
                        topics = topics,
                        onTopicClick = onTopicClick
                    )
                }
            }
        }
    }
}
