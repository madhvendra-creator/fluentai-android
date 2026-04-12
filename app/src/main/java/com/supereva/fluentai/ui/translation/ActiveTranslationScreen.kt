package com.supereva.fluentai.ui.translation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTranslationScreen(
    sourceLanguage: String = "English",
    targetLanguage: String = "Hindi",
    onBack: () -> Unit,
    onOpenLanguagePicker: (isSource: Boolean) -> Unit,
    onSwapLanguages: () -> Unit,
    onStartPracticeCall: (String, String) -> Unit
) {
    val brandGreen = Color(0xFF4CAF50)
    val darkBackground = Color(0xFF0D0D0D)
    val surfaceColor = Color.White.copy(alpha = 0.05f)

    Scaffold(
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Translate", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Language Swap Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                LanguageChip(
                    text = sourceLanguage,
                    color = surfaceColor,
                    onClick = { onOpenLanguagePicker(true) }
                )
                
                IconButton(onClick = onSwapLanguages, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Languages",
                        tint = brandGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                LanguageChip(
                    text = targetLanguage,
                    color = surfaceColor,
                    onClick = { onOpenLanguagePicker(false) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // NEW: Start AI Practice Call Button
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(brandGreen, Color(0xFF1B5E20))
                        )
                    )
                    .clickable { onStartPracticeCall(sourceLanguage, targetLanguage) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Practice Call",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Start AI Practice Call",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LanguageChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
