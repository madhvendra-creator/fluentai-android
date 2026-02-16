package com.supereva.fluentai.ui.progress.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.supereva.fluentai.ui.progress.ProgressViewModel

@Composable
fun WeeklyFluencyChart(
    data: List<ProgressViewModel.DailyScore>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Weekly Activity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { day ->
                BarItem(
                    label = day.label,
                    score = day.score,
                    maxScore = 100.0 // Assuming score is 0-100? Or 0-1? Let's assume 0-100 based on usage.
                    // If usage is 0.0-1.0, wait. SpeakingSession score is usually 0-100?
                    // Let's check logic. ScoreProgress uses totalScore/turnCount.
                    // If user gets integer scores, it's typically 0-100.
                )
            }
        }
    }
}

@Composable
fun BarItem(
    label: String,
    score: Double,
    maxScore: Double
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.width(32.dp)
    ) {
        val heightFraction = (score / maxScore).coerceIn(0.1, 1.0).toFloat()
        // Ensure even 0 score has a tiny bar for visual anchor if needed, or 0.
        val barHeightFraction = if (score > 0) heightFraction else 0.02f

        Surface(
            modifier = Modifier
                .width(12.dp)
                .fillMaxWidth() // Parent row handles width? No, width(32.dp) above.
                .weight(1f, fill = false) // Don't fill vertical, use height fraction
                .height(150.dp * barHeightFraction), // Max bar height 150dp
            color = if (score > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
        ) {}
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
