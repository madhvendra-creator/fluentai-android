package com.supereva.fluentai.ui.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supereva.fluentai.ui.home.components.AiPracticeCard

@Composable
fun TranslationScreen(
    onStartTranslation: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp)
        ) {
            // Section Header
            val supportedFlags = listOf(
                "🇪🇸", "🇫🇷", "🇩🇪", "🇮🇳", "🇯🇵", "🇨🇳", "🇮🇹", "🇰🇷", "🇷🇺", "🇸🇦", "🇧🇷"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Translate",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Scrolling flags
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(supportedFlags) { flag ->
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = flag, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Card — reusing AiPracticeCard with translation-themed text
            AiPracticeCard(
                onStartClick = onStartTranslation,
                titleTop = "PRACTICE",
                titleMain = "TRANSLATING",
                subtitle = "Available 24x7 | Multiple Languages",
                buttonText = "START TRANSLATING",
                gradientColors = listOf(
                    Color(0xFF1B5E20).copy(alpha = 0.6f),
                    Color(0xFF0D3B0F)
                ),
                buttonGradient = listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Placeholder for future translation content
            Text(
                text = "More features coming soon",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
