package com.missa.b360.ui.screens

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.missa.b360.R
import kotlinx.coroutines.delay

private const val SPLASH_VIDEO_ASPECT_RATIO = 9f / 16f

/**
 * Écran de splash vidéo portrait (9:16, `res/raw/splash_intro.mp4`).
 *
 * La vidéo est entièrement contenue et centrée sur tous les écrans : elle ne peut pas
 * être recadrée ni déborder. Les éventuelles marges sur les écrans au format différent
 * de 9:16 restent blanches. TextureView reste transparent avant la première image,
 * évitant un flash noir pendant l'initialisation ou une recréation d'activité.
 */
@Composable
fun SplashVideoScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember { mutableStateOf(false) }
    var surfaceReady by remember { mutableStateOf(false) }
    var videoSurface by remember { mutableStateOf<Surface?>(null) }

    fun tryStart() {
        if (prepared && surfaceReady) player?.start()
    }

    // Filet de sécurité : ne jamais rester bloqué sur le splash plus de 5 s.
    LaunchedEffect(Unit) {
        delay(5_000)
        onFinished()
    }

    DisposableEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.splash_intro)
        if (mediaPlayer != null) {
            player = mediaPlayer
            // La surface peut avoir été créée avant l'effet du lecteur.
            videoSurface?.let(mediaPlayer::setSurface)
            // Affiche toujours l'intégralité de la vidéo, sans zoom ni découpage.
            mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            mediaPlayer.setOnPreparedListener {
                prepared = true
                tryStart()
            }
            mediaPlayer.setOnCompletionListener { onFinished() }
        }
        onDispose {
            mediaPlayer?.setSurface(null)
            videoSurface?.release()
            videoSurface = null
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop()
                } catch (_: IllegalStateException) {
                    // Le lecteur peut déjà être arrêté après la fin de la vidéo.
                }
                mediaPlayer.release()
            }
            player = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        // Ajustement au plus grand rectangle 9:16 qui tient entièrement à l'écran.
        val videoModifier = if (maxWidth > maxHeight * SPLASH_VIDEO_ASPECT_RATIO) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(SPLASH_VIDEO_ASPECT_RATIO)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(SPLASH_VIDEO_ASPECT_RATIO)
        }

        AndroidView(
            modifier = videoModifier,
            factory = { ctx ->
                TextureView(ctx).apply {
                    // Avant le premier frame, la surface laisse voir le fond blanc du parent.
                    setOpaque(false)
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            val surface = Surface(surfaceTexture)
                            videoSurface?.release()
                            videoSurface = surface
                            player?.setSurface(surface)
                            surfaceReady = true
                            tryStart()
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            player?.setSurface(null)
                            videoSurface?.release()
                            videoSurface = null
                            surfaceReady = false
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                    }
                }
            },
        )
    }
}
