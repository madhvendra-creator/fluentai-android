package com.supereva.fluentai.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RealPeopleCard(
    onCallClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E88E5), Color(0xFF1565C0)) // Material Blue
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Talk with Real People",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ENGLISH CONVERSATIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }

                    // History Button
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { onHistoryClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🕒 Call History →",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                // Preference Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Preference",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    // Any (Selected)
                    PreferenceChip(text = "Any", isSelected = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Male (Premium)
                    PreferenceChip(text = "Male", isPremium = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Female (Premium)
                    PreferenceChip(text = "Female", isPremium = true)
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Call Button Container
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onCallClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)), // Amber
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Call & Practice Now",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    // "FREE" Badge floating on top left of button
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 20.dp, y = (-10).dp)
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = "FREE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceChip(text: String, isSelected: Boolean = false, isPremium: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) Color(0xFFFFC107) else Color.White,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (isPremium) {
                Text("👑 ", fontSize = 12.sp)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}
