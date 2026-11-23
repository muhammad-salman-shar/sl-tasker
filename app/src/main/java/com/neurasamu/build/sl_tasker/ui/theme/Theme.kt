package com.neurasamu.build.sl_tasker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun buildColorScheme(t: SemanticColorTokens, isLight: Boolean) = if (isLight) {
    lightColorScheme(
        primary = t.accent,
        onPrimary = t.onAccent,
        primaryContainer = t.badgeBg,
        onPrimaryContainer = t.badgeText,
        secondary = AccentGold,
        onSecondary = TextOnGold,
        secondaryContainer = t.surface,
        onSecondaryContainer = t.onSurfacePrimary,
        tertiary = t.cyanAccent,
        onTertiary = t.onAccent,
        background = t.background,
        onBackground = t.onSurfacePrimary,
        surface = t.surface,
        onSurface = t.onSurfacePrimary,
        surfaceVariant = t.cardBg,
        onSurfaceVariant = t.onSurfaceSecondary,
        error = t.danger,
        onError = t.onDanger,
        outline = t.border
    )
} else {
    darkColorScheme(
        primary = t.accent,
        onPrimary = t.onAccent,
        primaryContainer = t.accentDark,
        onPrimaryContainer = t.onSurfacePrimary,
        secondary = AccentGoldLight,
        onSecondary = TextOnGold,
        secondaryContainer = t.cardElevated,
        onSecondaryContainer = AccentGoldLight,
        tertiary = t.cyanAccent,
        onTertiary = TextOnGold,
        background = t.background,
        onBackground = t.onSurfacePrimary,
        surface = t.surface,
        onSurface = t.onSurfacePrimary,
        surfaceVariant = t.cardBg,
        onSurfaceVariant = t.onSurfaceSecondary,
        error = t.danger,
        onError = t.onDanger,
        outline = t.border
    )
}

@Composable
fun SoloLevelingTheme(content: @Composable () -> Unit) {
    val isLight = ThemeState.isLight
    val tokens = ThemeState.tokens
    val scheme = buildColorScheme(tokens, isLight)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val bg = tokens.background
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = bg.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
