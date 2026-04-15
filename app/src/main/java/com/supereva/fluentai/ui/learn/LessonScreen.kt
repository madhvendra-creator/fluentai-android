package com.supereva.fluentai.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LessonBg = Color(0xFF0D0D0D)
private val LessonSurface = Color(0xFF1A1A1A)
private val LessonOrange = Color(0xFFFF9800)
private val LessonTextPrimary = Color(0xFFFFFFFF)
private val LessonTextSecondary = Color(0xFFAAAAAA)
private val TileDefault = Color(0xFF2A2A2A)
private val TileSelected = Color(0xFF3D2800)
private val TileBorderDefault = Color(0xFF444444)
private val TileBorderSelected = LessonOrange

data class Exercise(
    val hindiSentence: String,
    val correctWords: List<String>
)

private val exercises = listOf(
    Exercise(
        hindiSentence = "मैं यहां आकर आभारी हूं!",
        correctWords = listOf("I", "am", "grateful", "to", "be", "here.")
    ),
    Exercise(
        hindiSentence = "मेरा नाम राज है।",
        correctWords = listOf("My", "name", "is", "Raj.")
    ),
    Exercise(
        hindiSentence = "मुझे यह नौकरी बहुत पसंद है।",
        correctWords = listOf("I", "really", "like", "this", "job.")
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonScreen(
    lessonId: String,
    onClose: () -> Unit,
    onComplete: (lessonId: String) -> Unit
) {
    var currentExerciseIndex by remember { mutableStateOf(0) }
    val currentExercise = exercises[currentExerciseIndex]

    // Scramble words once per exercise
    val scrambledWords = remember(currentExerciseIndex) {
        currentExercise.correctWords.shuffled().toMutableList()
    }

    val arrangedWords = remember(currentExerciseIndex) { mutableStateListOf<String>() }
    val remainingWords = remember(currentExerciseIndex) {
        mutableStateListOf<String>().also { it.addAll(scrambledWords) }
    }

    var showResult by remember(currentExerciseIndex) { mutableStateOf(false) }
    var isCorrect by remember(currentExerciseIndex) { mutableStateOf(false) }

    val progress = (currentExerciseIndex).toFloat() / exercises.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LessonBg)
            .padding(16.dp)
    ) {
        // Top row: close + progress bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = LessonTextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = LessonOrange,
                trackColor = Color(0xFF2A2A2A)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Listen & arrange",
            color = LessonTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Character card with speech bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A1A00)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧑", fontSize = 28.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.width(12.dp))

            // Speech bubble
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(LessonSurface)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        currentExercise.hindiSentence,
                        color = LessonTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { /* TTS playback would go here */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Play audio",
                            tint = LessonOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Arranged answer area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141414))
                .border(
                    1.dp,
                    if (showResult) (if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)) else Color(0xFF333333),
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (arrangedWords.isEmpty()) {
                Text("Tap words below to arrange…", color = Color(0xFF555555), fontSize = 13.sp)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    arrangedWords.forEach { word ->
                        WordTile(
                            word = word,
                            isSelected = true,
                            onClick = {
                                if (!showResult) {
                                    arrangedWords.remove(word)
                                    remainingWords.add(word)
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Scrambled word tiles
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            remainingWords.toList().forEach { word ->
                WordTile(
                    word = word,
                    isSelected = false,
                    onClick = {
                        if (!showResult) {
                            remainingWords.remove(word)
                            arrangedWords.add(word)
                        }
                    }
                )
            }
        }

        // Result feedback
        if (showResult) {
            Spacer(Modifier.height(16.dp))
            Text(
                if (isCorrect) "Sahi jawab! Bahut achha!" else "Sahi answer: ${currentExercise.correctWords.joinToString(" ")}",
                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))

        // CHECK / NEXT / COMPLETE button
        val buttonEnabled = arrangedWords.isNotEmpty()
        val buttonLabel = when {
            showResult && currentExerciseIndex < exercises.size - 1 -> "Next"
            showResult && currentExerciseIndex == exercises.size - 1 -> "Complete Lesson"
            else -> "Check"
        }

        Button(
            onClick = {
                when {
                    showResult && currentExerciseIndex < exercises.size - 1 -> {
                        currentExerciseIndex++
                        showResult = false
                    }
                    showResult && currentExerciseIndex == exercises.size - 1 -> {
                        onComplete(lessonId)
                    }
                    else -> {
                        isCorrect = arrangedWords == currentExercise.correctWords
                        showResult = true
                    }
                }
            },
            enabled = buttonEnabled || showResult,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (buttonEnabled || showResult) LessonOrange else Color(0xFF2A2A2A),
                disabledContainerColor = Color(0xFF2A2A2A)
            )
        ) {
            Text(
                buttonLabel,
                color = if (buttonEnabled || showResult) Color.White else Color(0xFF555555),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun WordTile(word: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TileSelected else TileDefault)
            .border(1.dp, if (isSelected) TileBorderSelected else TileBorderDefault, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            word,
            color = if (isSelected) LessonOrange else LessonTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
