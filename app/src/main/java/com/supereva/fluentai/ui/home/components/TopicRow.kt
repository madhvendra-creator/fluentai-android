package com.supereva.fluentai.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supereva.fluentai.ui.home.TopicUiModel

/**
 * Section header + horizontal scrolling row of [TopicCard]s.
 *
 * Stateless — takes a category title, list of topics, and click handler.
 */
@Composable
fun TopicRow(
    category: String,
    topics: List<TopicUiModel>,
    onTopicClick: (topicId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = topics, key = { it.id }) { topic ->
                TopicCard(
                    topic = topic,
                    onClick = { onTopicClick(topic.id) }
                )
            }
        }
    }
}
