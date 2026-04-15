package com.supereva.fluentai.ui.membership

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand palette ────────────────────────────────────────────────────────
private val BgColor = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF111111)
private val GreenBrand = Color(0xFF4CAF50)
private val GoldColor = Color(0xFFFFD700)
private val AmberColor = Color(0xFFFFB300)
private val OrangeColor = Color(0xFFFF9800)
private val BlueColor = Color(0xFF2196F3)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFAAAAAA)
private val GlassBorder = Color.White.copy(alpha = 0.12f)
private val GlassBg = Color.White.copy(alpha = 0.04f)

@Composable
fun MembershipScreen(onProfileClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeroSection(onProfileClick = onProfileClick) }
            item { ProficiencyChart() }
            item { SubscriptionCard() }
            item { PremiumBenefits() }
            item { TransactionHistory() }
        }

        // Floating "Chat with us" button — fixed bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            ChatFab()
        }
    }
}

// ── Hero Section ─────────────────────────────────────────────────────────

@Composable
private fun HeroSection(onProfileClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFFFFD700).copy(alpha = 0.3f))
                )
            )
    ) {
        // Profile / avatar icon — top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .border(2.dp, Color.White, CircleShape)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("👑", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))

            // Shimmer PREMIUM text drawn on Canvas
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "PREMIUM",
                    color = GoldColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
                Canvas(modifier = Modifier.size(width = 260.dp, height = 52.dp)) {
                    drawIntoCanvas { canvas ->
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerX, 0f),
                            end = Offset(shimmerX + 200f, size.height)
                        )
                        canvas.drawRect(
                            left = 0f, top = 0f,
                            right = size.width, bottom = size.height,
                            paint = Paint().apply { blendMode = BlendMode.SrcOver }
                        )
                    }
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.4f), Color.Transparent),
                            start = Offset(shimmerX, 0f),
                            end = Offset(shimmerX + 160f, size.height)
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            // Active badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Active until 01 May 2026", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Proficiency Progress Chart ────────────────────────────────────────────

@Composable
private fun ProficiencyChart() {
    val milestones = listOf("Beginner", "Rising\nVoice", "Good\nTalker", "Star\nSpeaker")
    val currentProgress = 0.38f  // user is at 38% of the journey

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Text("Your Proficiency Journey", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("Keep practicing to unlock the next level", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height
            val pts = listOf(
                Offset(w * 0.05f, h * 0.85f),
                Offset(w * 0.35f, h * 0.60f),
                Offset(w * 0.65f, h * 0.35f),
                Offset(w * 0.95f, h * 0.10f)
            )

            // Build smooth path through points using cubic beziers
            val curvePath = Path()
            curvePath.moveTo(pts[0].x, pts[0].y)
            for (i in 0 until pts.size - 1) {
                val cx = (pts[i].x + pts[i + 1].x) / 2f
                val cy = (pts[i].y + pts[i + 1].y) / 2f
                curvePath.quadraticTo(pts[i].x, pts[i].y, cx, cy)
            }
            curvePath.lineTo(pts.last().x, pts.last().y)

            // Glow shadow pass
            drawPath(
                curvePath,
                color = OrangeColor.copy(alpha = 0.25f),
                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            )

            // Full grey track
            drawPath(
                curvePath,
                color = Color(0xFF2A2A2A),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Completed orange path via PathMeasure
            val pm = androidx.compose.ui.graphics.PathMeasure()
            pm.setPath(curvePath, false)
            val completedPath = Path()
            pm.getSegment(0f, pm.length * currentProgress, completedPath, startWithMoveTo = true)
            drawPath(
                completedPath,
                brush = Brush.linearGradient(
                    listOf(OrangeColor, Color(0xFFFFB74D)),
                    start = pts[0],
                    end = pts.last()
                ),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Milestone dots
            pts.forEachIndexed { i, pt ->
                val fraction = i.toFloat() / (pts.size - 1)
                val passed = fraction <= currentProgress
                drawCircle(if (passed) OrangeColor else Color(0xFF333333), radius = 8.dp.toPx(), center = pt)
                if (passed) drawCircle(Color.White, radius = 4.dp.toPx(), center = pt)
            }

            // "You are here" green pin
            val t = currentProgress
            // Approximate position along the path
            val segIdx = (t * (pts.size - 1)).toInt().coerceIn(0, pts.size - 2)
            val segT = (t * (pts.size - 1)) - segIdx
            val pinX = pts[segIdx].x + segT * (pts[segIdx + 1].x - pts[segIdx].x)
            val pinY = pts[segIdx].y + segT * (pts[segIdx + 1].y - pts[segIdx].y)
            drawCircle(GreenBrand.copy(alpha = 0.4f), radius = 16.dp.toPx(), center = Offset(pinX, pinY))
            drawCircle(GreenBrand, radius = 8.dp.toPx(), center = Offset(pinX, pinY))
            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(pinX, pinY))
        }

        // Milestone labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            milestones.forEachIndexed { i, label ->
                val fraction = i.toFloat() / (milestones.size - 1)
                val isPassed = fraction <= currentProgress
                Text(
                    label,
                    color = if (isPassed) OrangeColor else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = if (isPassed) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
}

// ── Subscription Card (Glass morphism) ───────────────────────────────────

@Composable
private fun SubscriptionCard() {
    val daysLeft = 16
    val totalDays = 30
    val ringProgress = daysLeft.toFloat() / totalDays

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(GlassBg)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Monthly Plan", color = TextSecondary, fontSize = 12.sp)
                    Text("₹99 / month", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenBrand, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GreenBrand.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", color = GreenBrand, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }

                // Countdown ring
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFF2A2A2A),
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(GreenBrand, Color(0xFF81C784))),
                            startAngle = -90f, sweepAngle = 360f * ringProgress,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$daysLeft", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 18.sp)
                        Text("days", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = GlassBorder)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Auto-renews on 01 May 2026",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
                TextButton(onClick = {}) {
                    Text("Cancel Plan", color = Color(0xFFEF5350), fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Premium Benefits ──────────────────────────────────────────────────────

private data class BenefitItem(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

@Composable
private fun PremiumBenefits() {
    val benefits = listOf(
        BenefitItem("🤖", "Talk to AI Friend", "Practice real conversations anytime, anywhere", GreenBrand),
        BenefitItem("📚", "Master Grammar", "Smart corrections & detailed feedback on every attempt", BlueColor),
        BenefitItem("🔥", "Daily Practice", "Streak tracking, challenges & personalized targets", OrangeColor)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text("Premium Benefits", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        benefits.forEach { (emoji, title, subtitle, accentColor) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg)
                    .drawBehind {
                        drawRoundRect(
                            color = accentColor,
                            size = Size(4.dp.toPx(), size.height),
                            cornerRadius = CornerRadius(x = 2.dp.toPx(), y = 2.dp.toPx())
                        )
                    }
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Transaction History ───────────────────────────────────────────────────

@Composable
private fun TransactionHistory() {
    val transactions = listOf(
        Triple("01 Apr 2026", "Monthly Plan — ₹99", true),
        Triple("01 Mar 2026", "Monthly Plan — ₹99", true),
        Triple("01 Feb 2026", "Monthly Plan — ₹99", true)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Text("Transaction History", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        transactions.forEachIndexed { index, (date, desc, isDebit) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(desc, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(date, color = TextSecondary, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isDebit) "- ₹99" else "+ ₹0",
                        color = if (isDebit) Color(0xFFEF5350) else GreenBrand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Receipt",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (index < transactions.lastIndex) {
                HorizontalDivider(color = Color(0xFF222222))
            }
        }
    }
}

// ── Floating Chat FAB ─────────────────────────────────────────────────────

@Composable
private fun ChatFab() {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Color(0xFF25D366).copy(alpha = 0.25f))
        )
        FloatingActionButton(
            onClick = {},
            containerColor = Color(0xFF25D366),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(Icons.Default.Chat, contentDescription = "Chat with us", tint = Color.White)
        }
    }
}

