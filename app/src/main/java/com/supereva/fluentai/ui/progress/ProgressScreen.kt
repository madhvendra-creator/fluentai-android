package com.supereva.fluentai.ui.progress

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supereva.fluentai.data.database.entity.SessionEntity
import com.supereva.fluentai.di.SessionServiceLocator
import com.supereva.fluentai.ui.progress.ProgressViewModel.DailyScore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Brand colors ────────────────────────────────────────────────────────
private val BgColor = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF161625)
private val GreenBrand = Color(0xFF4CAF50)
private val GreenDark = Color(0xFF0D3B0F)
private val GreenMid = Color(0xFF1B5E20)
private val AmberColor = Color(0xFFFFB300)
private val OrangeColor = Color(0xFFFF9800)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)
private val RedColor = Color(0xFFEF5350)

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.provideFactory(SessionServiceLocator.localHistoryRepository)
    )
) {
    val sessions by viewModel.sessions.collectAsState()
    val chartData by viewModel.weeklyChartData.collectAsState()

    val totalSessions = sessions.size
    val avgScore = if (sessions.isNotEmpty()) sessions.map { it.averageScore }.average() else 0.0
    val sentencesLearnt = totalSessions * 3
    val xp = totalSessions * 50
    val level = (xp / 600) + 1
    val xpInLevel = xp % 600
    val currentStreak = chartData.reversed().takeWhile { it.score > 0.0 }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ProfileCard(level = level, xp = xpInLevel, xpGoal = 600) }
        item { SentencesLearntCard(sentencesLearnt = sentencesLearnt) }
        item { StreakCard(weeklyData = chartData, currentStreak = currentStreak) }
        item { StatsRow(totalSessions = totalSessions, avgScore = avgScore, bestStreak = currentStreak) }

        item {
            Text(
                "Recent Sessions",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    "No sessions yet. Head to the Home tab to start practicing!",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(sessions) { session ->
                PremiumSessionItem(session = session)
            }
        }
    }
}

// ── Profile Card ─────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(level: Int, xp: Int, xpGoal: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF0D0D0D)))
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar with green ring + level badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .drawBehind {
                            drawCircle(color = GreenBrand, radius = size.minDimension / 2f + 3.dp.toPx())
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A3E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧑", fontSize = 28.sp)
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(GreenBrand),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$level", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("FluentAI Learner", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Level $level", color = TextSecondary, fontSize = 12.sp)
                    Text(" — ", color = TextSecondary, fontSize = 12.sp)
                    Text("$xp/$xpGoal XP", color = GreenBrand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                // XP bar with glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF2A2A3E))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (xp.toFloat() / xpGoal).coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(listOf(GreenBrand, Color(0xFF81C784)))
                            )
                    )
                }
            }
        }
    }
}

// ── Sentences Learnt Card ─────────────────────────────────────────────────

@Composable
private fun SentencesLearntCard(sentencesLearnt: Int) {
    val goal = 100
    val progress = (sentencesLearnt.toFloat() / goal).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(GreenDark, GreenMid)))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "$sentencesLearnt",
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 48.sp,
                        lineHeight = 48.sp
                    )
                    Text("Sentences Learnt", color = Color(0xFFB9F6CA), fontSize = 14.sp)
                }
                Text("🏁", fontSize = 36.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Curved path canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val w = size.width
                val h = size.height
                val startPt = Offset(w * 0.05f, h * 0.85f)
                val controlPt = Offset(w * 0.5f, h * 0.1f)
                val endPt = Offset(w * 0.95f, h * 0.2f)

                // Full dashed path (grey — remaining)
                val fullPath = Path().apply {
                    moveTo(startPt.x, startPt.y)
                    quadraticTo(controlPt.x, controlPt.y, endPt.x, endPt.y)
                }
                drawPath(
                    path = fullPath,
                    color = Color(0x44FFFFFF),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                        cap = StrokeCap.Round
                    )
                )

                // Completed solid path
                if (progress > 0f) {
                    val pm = PathMeasure()
                    pm.setPath(fullPath, false)
                    val completedPath = Path()
                    pm.getSegment(0f, pm.length * progress, completedPath, startWithMoveTo = true)
                    drawPath(
                        completedPath,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF69F0AE), GreenBrand),
                            start = startPt,
                            end = endPt
                        ),
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Milestone dots at t = 0.25, 0.5, 0.75, 1.0
                val milestones = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                milestones.forEach { t ->
                    val mt = 1f - t
                    val mx = mt * mt * startPt.x + 2 * mt * t * controlPt.x + t * t * endPt.x
                    val my = mt * mt * startPt.y + 2 * mt * t * controlPt.y + t * t * endPt.y
                    val isPassed = progress >= t
                    drawCircle(if (isPassed) GreenBrand else Color(0x44FFFFFF), radius = 6.dp.toPx(), center = Offset(mx, my))
                }

                // Current position glowing dot
                val t = progress
                val mt = 1f - t
                val cx = mt * mt * startPt.x + 2 * mt * t * controlPt.x + t * t * endPt.x
                val cy = mt * mt * startPt.y + 2 * mt * t * controlPt.y + t * t * endPt.y
                drawCircle(GreenBrand.copy(alpha = 0.35f), radius = 20.dp.toPx(), center = Offset(cx, cy))
                drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(cx, cy))
                drawCircle(GreenBrand, radius = 5.dp.toPx(), center = Offset(cx, cy))

                // Flag at end
                drawCircle(Color(0xFFFFD700), radius = 6.dp.toPx(), center = endPt)
            }

            Spacer(Modifier.height(12.dp))

            // Amber achievement badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF7A5800).copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "You are among the top 50% English speakers",
                    color = AmberColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Streak Card ───────────────────────────────────────────────────────────

@Composable
private fun StreakCard(weeklyData: List<DailyScore>, currentStreak: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🔥",
                    fontSize = (32 * pulseScale).sp,
                    modifier = Modifier.drawBehind {
                        drawCircle(
                            color = Color(0xFFFF6D00).copy(alpha = 0.25f * pulseAlpha),
                            radius = 28.dp.toPx()
                        )
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "$currentStreak practice days",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text("Keep your streak alive!", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Weekly calendar row
            val todayIndex = (weeklyData.size - 1).coerceAtLeast(0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEachIndexed { index, day ->
                    val isToday = index == todayIndex
                    val hasSession = day.score > 0.0
                    val isMissed = !hasSession && !isToday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            day.label.take(1),
                            color = if (isToday) OrangeColor else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )

                        when {
                            hasSession -> {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(listOf(Color(0xFF66BB6A), GreenDark))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            isMissed -> {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3A1A1A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✗", color = RedColor, fontSize = 13.sp)
                                }
                            }
                            else -> {
                                // Today, no session yet — pulsing orange
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .drawBehind {
                                            drawCircle(
                                                color = OrangeColor.copy(alpha = 0.3f * pulseAlpha),
                                                radius = 20.dp.toPx()
                                            )
                                        }
                                        .clip(CircleShape)
                                        .background(Color(0xFF3D2200)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("!", color = OrangeColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (weeklyData.none { it.score > 0.0 }) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Practice today to start your streak!",
                    color = OrangeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Stats Row ─────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(totalSessions: Int, avgScore: Double, bestStreak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(emoji = "🎯", value = "$totalSessions", label = "Sessions", modifier = Modifier.weight(1f))
        StatCard(emoji = "📊", value = "${avgScore.toInt()}%", label = "Avg Score", modifier = Modifier.weight(1f))
        StatCard(emoji = "🏆", value = "$bestStreak", label = "Best Streak", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text(label, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

// ── Recent Session Item (premium styled) ─────────────────────────────────

@Composable
private fun PremiumSessionItem(session: SessionEntity) {
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val dateLabel = dateFormat.format(Date(session.timestamp))
    val scoreColor = when {
        session.averageScore >= 80 -> GreenBrand
        session.averageScore >= 60 -> OrangeColor
        else -> RedColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A2A1A)),
            contentAlignment = Alignment.Center
        ) {
            Text("💬", fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.topicId.replace("_", " ").replaceFirstChar { it.uppercase() },
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Text(dateLabel, color = TextSecondary, fontSize = 12.sp)
            Text("${session.turnCount} turns", color = TextSecondary, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${session.averageScore.toInt()}%",
                color = scoreColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(scoreColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (session.averageScore >= 80) "Great" else if (session.averageScore >= 60) "Good" else "Keep going",
                    color = scoreColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
