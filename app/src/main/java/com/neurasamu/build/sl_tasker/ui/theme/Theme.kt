package com.neurasamu.build.sl_tasker.ui.theme

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

private fun darkScheme() = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnBlue,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE8EEF9),
    secondary = AccentGold,
    onSecondary = TextOnGold,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF15203A),
    onSecondaryContainer = AccentGold,
    tertiary = AccentCyan,
    onTertiary = TextOnGold,
    background = androidx.compose.ui.graphics.Color(0xFF050A18),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE8EEF9),
    surface = androidx.compose.ui.graphics.Color(0xFF0A1228),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE8EEF9),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF0F1830),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF8B9AB8),
    error = DangerRed,
    onError = TextOnBlue,
    outline = androidx.compose.ui.graphics.Color(0xFF1E2A45)
)

@Composable
fun SoloLevelingTheme(content: @Composable () -> Unit) {
    // Locked to Dark Theme only
    val isLight = false
    ThemeState.mode = 1
    ThemeState.isLight = false

    val scheme = darkScheme()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = if (isLight) androidx.compose.ui.graphics.Color(0xFFF5F7FB) else androidx.compose.ui.graphics.Color(0xFF050A18)
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
