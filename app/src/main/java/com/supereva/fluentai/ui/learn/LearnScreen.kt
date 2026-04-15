package com.supereva.fluentai.ui.learn

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supereva.fluentai.domain.learn.Lesson
import com.supereva.fluentai.domain.learn.LearnLevel
import com.supereva.fluentai.domain.learn.LearnUnit
import com.supereva.fluentai.domain.learn.LessonType

private val BackgroundColor = Color(0xFF0D0D0D)
private val SurfaceColor = Color(0xFF1A1A2E)
private val OrangeColor = Color(0xFFFF9800)
private val OrangeDim = Color(0xFF7A4800)
private val LockedColor = Color(0xFF2A2A2A)
private val LockedIconColor = Color(0xFF555555)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    viewModel: LearnViewModel = viewModel(factory = LearnViewModel.Factory),
    onNavigateToLesson: (lessonId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Top bar
            item {
                TopBar()
            }

            // Units
            uiState.units.forEach { unit ->
                item {
                    UnitCard(unit = unit)
                }

                unit.levels.forEach { level ->
                    if (level.isUnlocked) {
                        item {
                            LevelProgressCard(unit = unit, level = level)
                        }
                        itemsIndexed(level.lessons) { index, lesson ->
                            val isCurrentLesson = !lesson.isCompleted &&
                                level.lessons.take(index).all { it.isCompleted }
                            LessonNode(
                                lesson = lesson,
                                isCurrentLesson = isCurrentLesson,
                                indexInLevel = index,
                                onClick = {
                                    if (isCurrentLesson || lesson.isCompleted) {
                                        viewModel.selectLesson(lesson, unit.id, level.id)
                                    }
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    } else {
                        item {
                            LockedLevelCard(level = level)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        // Bottom sheet for selected lesson
        uiState.selectedLesson?.let { lesson ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissLesson() },
                sheetState = sheetState,
                containerColor = SurfaceColor,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF444444)) }
            ) {
                LessonBottomSheet(
                    lesson = lesson,
                    onContinue = {
                        viewModel.dismissLesson()
                        onNavigateToLesson(lesson.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // Streak
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = OrangeColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("7", color = OrangeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.width(8.dp))

        // Trophy
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Trophy",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("3", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.width(8.dp))

        // Coins
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("🪙", fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text("240", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun UnitCard(unit: LearnUnit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A2E))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(unit.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(unit.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(unit.subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LevelProgressCard(unit: LearnUnit, level: LearnLevel) {
    val totalAcrossLevels = unit.levels.sumOf { it.totalLessons }
    val completedAcrossLevels = unit.levels.sumOf { it.completedLessons }
    val remaining = totalAcrossLevels - completedAcrossLevels
    val progress = if (totalAcrossLevels > 0) completedAcrossLevels.toFloat() / totalAcrossLevels else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            "LEVEL ${level.levelNumber}",
            color = OrangeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(level.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "$remaining aur sentences seekho level ${level.levelNumber + 1} tak pahunchne ke liye",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = OrangeColor,
                trackColor = Color(0xFF2A2A2A)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "$completedAcrossLevels/$totalAcrossLevels",
                color = OrangeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LockedLevelCard(level: LearnLevel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LockedColor)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked",
            tint = LockedIconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text("LEVEL ${level.levelNumber}", color = LockedIconColor, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(level.title, color = Color(0xFF666666), fontSize = 14.sp)
        }
    }
}

// Zigzag horizontal offsets: left / center / right / center / left ...
private val zigzagOffsets = listOf(-72, 0, 72, 0, -72, 0, 72, 0)

@Composable
private fun LessonNode(
    lesson: Lesson,
    isCurrentLesson: Boolean,
    indexInLevel: Int,
    onClick: () -> Unit
) {
    val offsetX = zigzagOffsets[indexInLevel % zigzagOffsets.size]

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrentLesson) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val nodeColor = when {
        lesson.isCompleted -> OrangeColor
        isCurrentLesson -> OrangeColor
        else -> LockedColor
    }
    val borderColor = when {
        lesson.isCompleted -> OrangeColor
        isCurrentLesson -> Color(0xFFFFB74D)
        else -> Color(0xFF3A3A3A)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX.dp)
                .size(56.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(nodeColor)
                .border(3.dp, borderColor, CircleShape)
                .clickable(enabled = isCurrentLesson || lesson.isCompleted) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when {
                lesson.isCompleted -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                isCurrentLesson -> Text(
                    lessonTypeEmoji(lesson.type),
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                else -> Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = LockedIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun lessonTypeEmoji(type: LessonType): String = when (type) {
    LessonType.READING -> "📖"
    LessonType.LISTENING -> "🎧"
    LessonType.SPEAKING -> "🎤"
    LessonType.ARRANGE -> "🔤"
}

@Composable
private fun LessonBottomSheet(lesson: Lesson, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp, )
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(lessonTypeEmoji(lesson.type), fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            lesson.title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            lesson.description,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeColor)
        ) {
            Text("Continue Lesson", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
