package com.arnix.ide.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = IdePrimary,
    onPrimary = IdeBackground,
    primaryContainer = IdePrimaryContainer,
    onPrimaryContainer = IdeTextPrimary,
    secondary = IdeSecondary,
    onSecondary = IdeBackground,
    tertiary = IdeTertiary,
    background = IdeBackground,
    onBackground = IdeTextPrimary,
    surface = IdeSurface,
    onSurface = IdeTextPrimary,
    surfaceVariant = IdeSurfaceVariant,
    onSurfaceVariant = IdeTextSecondary,
    outline = IdeBorder,
    error = IdeError
)

private val LightColorScheme = lightColorScheme(
    primary = IdePrimaryContainer,
    onPrimary = IdeTextPrimary,
    primaryContainer = IdePrimary,
    secondary = IdeSecondary,
    background = IdeSurfaceVariant,
    onBackground = IdeTextPrimary,
    surface = IdeSurface,
    onSurface = IdeTextPrimary,
    outline = IdeBorder,
    error = IdeError
)

@Composable
fun ArnixTheme(
    darkTheme: Boolean = true, // IDE default is dark
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
