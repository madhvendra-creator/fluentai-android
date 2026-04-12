package com.supereva.fluentai.ui.video

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.supereva.fluentai.R

/**
 * Singleton ExoPlayer instance for the hero video card.
 *
 * The player is created once and kept "warm" across tab navigations.
 * It is never destroyed during normal tab switching — only when the
 * app process is explicitly killed or [release] is called.
 *
 * Both HomeScreen and TranslationScreen share this single instance
 * via attach/detach on [androidx.media3.ui.PlayerView].
 */
object HeroVideoPlayer {

    @Volatile
    private var exoPlayer: ExoPlayer? = null

    /**
     * Returns the singleton ExoPlayer, creating it on first access.
     * Must be called with an Application context.
     */
    fun get(context: Context): ExoPlayer {
        return exoPlayer ?: synchronized(this) {
            exoPlayer ?: createPlayer(context.applicationContext).also { exoPlayer = it }
        }
    }

    private fun createPlayer(appContext: Context): ExoPlayer {
        return ExoPlayer.Builder(appContext).build().apply {
            val uri = android.net.Uri.parse("android.resource://${appContext.packageName}/${R.raw.ai_avatar_video}")

            // The video is 8000ms long. Trim the last 1s to avoid the baked-in slide transition.
            val cutOffTimeMs = 7000L

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(cutOffTimeMs)
                        .build()
                )
                .build()

            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    /**
     * Call from Application.onTerminate or when the player is no longer needed.
     * In practice, Android rarely calls onTerminate — the OS kills the process.
     */
    fun release() {
        synchronized(this) {
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
