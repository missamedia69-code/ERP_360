package com.missabusiness.app.ui.screens

import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.missabusiness.app.R

/**
 * Écran d'introduction : joue la vidéo res/raw/splash_intro.mp4 en plein écran,
 * juste après le splash système Android, puis bascule vers l'application.
 *
 * - Lecture vidéo via MediaPlayer + TextureView (aucune dépendance externe)
 * - La zone vidéo conserve strictement le format portrait 9:16, sans zoom,
 *   recadrage ni déformation ; elle est centrée dans la surface disponible
 * - Masquée (fond blanc) jusqu'au rendu de la première frame : aucune image
 *   fantôme de fin de vidéo
 * - Tap n'importe où = passer l'intro
 */
@Composable
fun SplashVideoScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // La vue vidéo est créée une seule fois et ne change jamais de taille
    // pendant la lecture (un resize détruirait la surface et bloquerait la vidéo).
    val textureView = remember { TextureView(context) }
    val prepared = remember { mutableStateOf(false) }
    val surfaceReady = remember { mutableStateOf(false) }

    // Lecteur préparé une seule fois ; démarré quand la surface est prête.
    val player = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build(),
            )
            setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/${R.raw.splash_intro}"),
            )
            setOnCompletionListener { onFinished() }
            prepareAsync()
        }
    }

    // Passe à true au rendu de la première frame : avant, la vue reste masquée
    // (fond blanc) pour ne montrer aucune image résiduelle.
    val firstFrameShown = remember { mutableStateOf(false) }

    // La lecture démarre uniquement quand le lecteur est préparé ET la surface
    // disponible, pour que la première frame décodée soit immédiatement visible.
    fun tryStart() {
        if (prepared.value && surfaceReady.value) {
            player.start()
        }
    }
    player.setOnPreparedListener {
        prepared.value = true
        tryStart()
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Immersif : barres système masquées pendant l'intro, restaurées après.
    DisposableEffect(Unit) {
        val window = activity?.window
        val controller = window?.let {
            WindowCompat.getInsetsController(it, it.decorView)
        }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            // Tap n'importe où = passer l'intro.
            .pointerInput(Unit) {
                detectTapGestures { onFinished() }
            },
    ) {
        // Un Pixel 8 est plus haut qu'un écran 9:16. La vue prend donc la plus
        // grande taille 9:16 possible, reste centrée et laisse le fond blanc
        // remplir l'espace restant : aucun bord de la vidéo n'est coupé.
        val videoAspectRatio = 9f / 16f
        val screenAspectRatio = maxWidth / maxHeight
        val videoModifier = if (screenAspectRatio > videoAspectRatio) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(videoAspectRatio)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio)
        }

        AndroidView(
            factory = {
                textureView.apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            player.setSurface(Surface(surfaceTexture))
                            surfaceReady.value = true
                            tryStart()
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(
                            surfaceTexture: SurfaceTexture,
                        ): Boolean {
                            player.setSurface(null)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(
                            surfaceTexture: SurfaceTexture,
                        ) {
                            if (!firstFrameShown.value) firstFrameShown.value = true
                        }
                    }
                }
            },
            // La TextureView elle-même est au format 9:16 et centrée. Sans
            // matrice de transformation, le buffer vidéo reste intégralement visible.
            modifier = videoModifier
                .align(Alignment.Center)
                .alpha(if (firstFrameShown.value) 1f else 0f),
        )
    }
}