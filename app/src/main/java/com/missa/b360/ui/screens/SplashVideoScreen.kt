package com.missa.b360.ui.screens

import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.missa.b360.R
import kotlinx.coroutines.delay

/**
 * Écran de splash vidéo (~2 s, `res/raw/splash_intro.mp4`).
 * La lecture démarre quand le lecteur est préparé ET la surface disponible,
 * pour que la première frame décodée soit immédiatement visible.
 * Activable/désactivable via les réglages (RA-21, Phase C).
 */
@Composable
fun SplashVideoScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember { mutableStateOf(false) }
    var surfaceReady by remember { mutableStateOf(false) }

    fun tryStart() {
        if (prepared && surfaceReady) {
            player?.start()
        }
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
            mediaPlayer.setOnPreparedListener {
                prepared = true
                tryStart()
            }
            mediaPlayer.setOnCompletionListener { onFinished() }
        }
        onDispose {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop()
                } catch (_: IllegalStateException) {
                    // déjà arrêté
                }
                mediaPlayer.release()
            }
            player = null
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        player?.setSurface(holder.surface)
                        surfaceReady = true
                        tryStart()
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surfaceReady = false
                    }
                })
            }
        },
    )
}
