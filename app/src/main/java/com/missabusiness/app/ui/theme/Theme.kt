package com.missabusiness.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
    onBackground = Blue20,
    surface = LightSurface,
    onSurface = Blue20,
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

@Composable
fun Erp360Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
