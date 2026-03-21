package com.kycis.demo.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = White,
    primaryContainer = Blue200,
    onPrimaryContainer = Gray900,
    secondary = Green700,
    onSecondary = White,
    secondaryContainer = Green200,
    onSecondaryContainer = Gray900,
    tertiary = Blue500,
    onTertiary = White,
    error = Red500,
    onError = White,
    errorContainer = Red200,
    onErrorContainer = Red700,
    background = LightBackground,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    outline = Gray500
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = Gray900,
    primaryContainer = Blue700,
    onPrimaryContainer = White,
    secondary = Green200,
    onSecondary = Gray900,
    secondaryContainer = Green700,
    onSecondaryContainer = White,
    tertiary = Blue500,
    onTertiary = White,
    error = Red200,
    onError = Gray900,
    errorContainer = Red700,
    onErrorContainer = White,
    background = Gray900,
    onBackground = White,
    surface = Gray900,
    onSurface = White,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray300,
    outline = Gray500
)

@Composable
fun KycDemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}