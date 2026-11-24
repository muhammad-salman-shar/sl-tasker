package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun SoloLevelingTheme(
    darkTheme: Boolean = !ThemeState.isLight,
    content: @Composable () -> Unit
) {
    val t = ThemeState.tokens
    val scheme = if (ThemeState.isLight) {
        lightColorScheme(
            primary = t.accent,
            onPrimary = t.onAccent,
            secondary = t.cyanAccent,
            onSecondary = t.onAccent,
            tertiary = t.warning,
            background = t.background,
            onBackground = t.onSurfacePrimary,
            surface = t.cardBg,
            onSurface = t.onSurfacePrimary,
            surfaceVariant = t.surface,
            onSurfaceVariant = t.onSurfaceSecondary,
            outline = t.border,
            error = t.danger,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = t.accent,
            onPrimary = t.onAccent,
            secondary = t.cyanAccent,
            onSecondary = t.onAccent,
            tertiary = t.warning,
            background = t.background,
            onBackground = t.onSurfacePrimary,
            surface = t.cardBg,
            onSurface = t.onSurfacePrimary,
            surfaceVariant = t.surface,
            onSurfaceVariant = t.onSurfaceSecondary,
            outline = t.border,
            error = t.danger,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
