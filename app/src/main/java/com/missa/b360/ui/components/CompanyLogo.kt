package com.missa.b360.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Affiche un logo d'entreprise enregistré localement, ou une icône de repli lorsqu'aucun
 * logo n'est encore choisi. L'aperçu est décodé en taille réduite afin de rester fluide.
 */
@Composable
fun CompanyLogo(
    logoUri: String?,
    contentDescription: String?,
    fallbackIcon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: androidx.compose.ui.graphics.Shape,
    fallbackTint: Color,
    fallbackBackground: Color,
) {
    val bitmap = rememberCompanyLogoBitmap(logoUri)
    Surface(
        modifier = modifier,
        shape = shape,
        color = fallbackBackground,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape),
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = contentDescription,
                    tint = fallbackTint,
                    modifier = Modifier.size(size / 2),
                )
            }
        }
    }
}

@Composable
private fun rememberCompanyLogoBitmap(logoUri: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(logoUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(logoUri) {
        bitmap = logoUri?.let { uriText ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(uriText)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, bounds)
                    }
                    val largestSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                    var sampleSize = 1
                    while (largestSide / sampleSize > LOGO_PREVIEW_MAX_SIDE) sampleSize *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

private const val LOGO_PREVIEW_MAX_SIDE = 640
