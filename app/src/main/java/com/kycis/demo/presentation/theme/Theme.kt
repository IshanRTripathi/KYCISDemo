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

// Import colors
import com.kycis.demo.presentation.theme.*

private val LightColorScheme = lightColorScheme(
    primary = ThemePrimary,
    onPrimary = White,
    primaryContainer = ThemePrimaryLight,
    onPrimaryContainer = Gray900,
    secondary = Blue500,
    onSecondary = White,
    secondaryContainer = BlueLight,
    onSecondaryContainer = Gray900,
    tertiary = ThemePrimaryVariant,
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
    primary = ThemePrimary,
    onPrimary = Gray900,
    primaryContainer = ThemePrimaryVariant,
    onPrimaryContainer = White,
    secondary = Blue200,
    onSecondary = Gray900,
    secondaryContainer = Blue700,
    onSecondaryContainer = White,
    tertiary = ThemePrimaryLight,
    onTertiary = Gray900,
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