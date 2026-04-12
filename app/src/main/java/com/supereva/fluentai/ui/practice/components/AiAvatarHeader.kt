package com.supereva.fluentai.ui.practice.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.supereva.fluentai.R

@Composable
fun AiAvatarHeader(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")

    // Create 3 expanding rings
    val rings = List(3) { index ->
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isSpeaking) 2.5f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    delayMillis = index * 300,
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_scale_$index"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    delayMillis = index * 300,
                    easing = LinearOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_alpha_$index"
        )
        if (isSpeaking) scale to alpha else 1f to 0f
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .height(160.dp)
    ) {
        // Draw pulsing rings
        rings.forEach { (scale, alpha) ->
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale as Float)
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha as Float),
                        shape = CircleShape
                    )
            )
        }
        // Central Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ai_avatar),
                contentDescription = "AI Tutor",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
