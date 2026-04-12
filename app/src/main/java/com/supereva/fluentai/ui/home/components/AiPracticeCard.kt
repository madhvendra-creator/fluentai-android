package com.supereva.fluentai.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.supereva.fluentai.R
import com.supereva.fluentai.ui.video.HeroVideoPlayer

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AiPracticeCard(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleTop: String = "TALK ABOUT",
    titleMain: String = "ANYTHING",
    subtitle: String = "Available 24x7 | Anytime Anywhere",
    buttonText: String = "TALK NOW",
    gradientColors: List<Color> = listOf(Color(0xFF2C3E9C).copy(alpha = 0.6f), Color(0xFF1E2B70)),
    buttonGradient: List<Color> = listOf(Color(0xFFFF9800), Color(0xFFE65100))
) {
    val context = LocalContext.current
    // Get the singleton player — never recreated on tab switch
    val exoPlayer = remember { HeroVideoPlayer.get(context) }

    // Track whether the video has rendered its first frame
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    // Attach a Player.Listener to detect the exact moment the first frame renders
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onRenderedFirstFrame() {
                isFirstFrameRendered = true
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Do NOT release the player here — it's a singleton shared across tabs
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .padding(horizontal = 16.dp)
            .clickable { onStartClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Layer 1: Static placeholder — visible behind the video until it starts rendering
            val placeholderAlpha by animateFloatAsState(
                targetValue = if (isFirstFrameRendered) 0f else 1f,
                animationSpec = tween(durationMillis = 300),
                label = "placeholderFade"
            )
            if (placeholderAlpha > 0f) {
                Image(
                    painter = painterResource(id = R.drawable.ai_avatar_first_frame),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = placeholderAlpha }
                )
            }

            // Layer 2: Video player (TextureView from XML)
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    // Use a FrameLayout as parent to resolve layout params from the inflated XML
                    val parent = android.widget.FrameLayout(ctx)
                    val view = android.view.LayoutInflater.from(ctx)
                        .inflate(R.layout.custom_player_view, parent, false)
                    parent.addView(view)

                    val playerView = view.findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)
                    playerView.player = exoPlayer
                    playerView.setKeepContentOnPlayerReset(true)
                    playerView.controllerAutoShow = false

                    parent
                },
                update = { parent ->
                    // Re-attach the player when Compose recomposes (e.g., returning to this tab)
                    val playerView = parent.findViewById<androidx.media3.ui.PlayerView>(R.id.player_view)
                    if (playerView.player != exoPlayer) {
                        playerView.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Layer 3: Gradient Overlay (Transparent at top, themed at bottom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent) + gradientColors,
                            startY = 150f
                        )
                    )
            )

            // Layer 4: Text and Button aligned to the bottom center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = titleTop,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = titleMain,
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFFFD54F),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(20.dp))
                // Gradient Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.horizontalGradient(colors = buttonGradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
