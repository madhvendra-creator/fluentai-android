package com.supereva.fluentai.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supereva.fluentai.R
import com.supereva.fluentai.ui.home.TopicUiModel

@Composable
fun TopicCard(
    topic: TopicUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .width(150.dp)  // Matches the target UI proportion
            .height(210.dp) // Strict height ensures perfectly aligned rows
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp) // Fixed image height
            ) {
                Image(
                    painter = painterResource(id = topic.imageResId ?: R.drawable.introductionyourself),
                    contentDescription = topic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // TRENDING NOW badge has been removed from here
            }

            // Layer 2: Text & Icons Footer
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxHeight(), // Fill remaining space to push alignment
                verticalArrangement = Arrangement.SpaceBetween 
            ) {
                // Title
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    minLines = 2, // Forces 1-line titles to reserve space for 2 lines
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                // Duration & Difficulty Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⏱ ${topic.duration}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    // Difficulty Dot
                    val diffColor = when (topic.difficulty.lowercase()) {
                        "beginner", "easy" -> Color(0xFF4CAF50) // Green
                        "intermediate", "medium" -> Color(0xFFFFC107) // Yellow
                        "advanced", "hard" -> Color(0xFFF44336) // Red
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(diffColor)
                    )
                    Text(
                        text = if (topic.difficulty.lowercase() == "beginner") "Easy" else topic.difficulty.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
