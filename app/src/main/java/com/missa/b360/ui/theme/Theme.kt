package com.missa.b360.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Blue60,
    onPrimary = White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue20,
    secondary = Green60,
    onSecondary = White,
    secondaryContainer = Green90,
    onSecondaryContainer = Green40,
    error = Red40,
    onError = White,
    errorContainer = Red80,
    background = LightBackground,
    onBackground = MissaInk,
    surface = LightSurface,
    onSurface = MissaInk,
    surfaceVariant = MissaSoftBlue,
    onSurfaceVariant = MissaMuted,
    outline = MissaBorder,
    outlineVariant = MissaBorder,
)

private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue40,
    onPrimaryContainer = Blue90,
    secondary = Green60,
    onSecondary = White,
    secondaryContainer = Green40,
    onSecondaryContainer = Green90,
    error = Red80,
    onError = Red40,
    background = DarkBackground,
    onBackground = Blue90,
    surface = DarkSurface,
    onSurface = Blue90,
)

/**
 * Rayon et hiérarchie visuelle homogènes : boutons et champs doux, cartes compactes.
 * Les écrans sont volontairement maintenus dans une direction claire pour correspondre
 * aux maquettes mobiles de référence, même si l'appareil utilise un thème sombre.
 */
private val MissaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun Erp360Theme(
    darkTheme: Boolean = false,
    // Le bleu Missa est une composante de la direction artistique : ne pas le remplacer
    // silencieusement par la palette Monet de l'appareil.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MissaShapes,
        content = content,
    )
}
