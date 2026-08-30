package com.missabusiness.app.ui.screens

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * - La vidéo couvre tout l'écran (mode "cover") : ratio préservé, mise à
 *   l'échelle pour remplir la vue, centrée, débordement recadré
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

    // Applique le cadrage : la vidéo couvre tout l'écran (mode "cover").
    // Ratio préservé, mise à l'échelle pour remplir la vue, centrée ;
    // seul le débordement (haut/bas ou gauche/droite) est recadré.
    // (Sans transform, TextureView étire son buffer aux dimensions de la vue.)
    fun applyMatrix() {
        val vw = player.videoWidth.toFloat()
        val vh = player.videoHeight.toFloat()
        if (vw <= 0f || vh <= 0f || textureView.width == 0) return
        val scale = maxOf(textureView.width / vw, textureView.height / vh)
        val matrix = Matrix()
        matrix.setScale(scale, scale, textureView.width / 2f, textureView.height / 2f)
        textureView.setTransform(matrix)
    }

    // La lecture démarre uniquement quand le lecteur est préparé ET la surface
    // disponible, pour que la première frame décodée soit immédiatement visible.
    fun tryStart() {
        if (prepared.value && surfaceReady.value) {
            applyMatrix()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            // Tap n'importe où = passer l'intro.
            .pointerInput(Unit) {
                detectTapGestures { onFinished() }
            },
    ) {
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
                        ) {
                            // Recentre si la vue change de taille (ex. rotation).
                            applyMatrix()
                        }

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
            // Masqué jusqu'à la première frame réelle, puis plein écran.
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (firstFrameShown.value) 1f else 0f),
        )
    }
}