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

private fun lightScheme() = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2563EB),
    onPrimary = TextOnBlue,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFDBEAFE),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0F1830),
    secondary = androidx.compose.ui.graphics.Color(0xFFD97706),
    onSecondary = TextOnBlue,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFEF3C7),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF78350F),
    tertiary = androidx.compose.ui.graphics.Color(0xFF0891B2),
    onTertiary = TextOnBlue,
    background = androidx.compose.ui.graphics.Color(0xFFF5F7FB),
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F1830),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F1830),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F2F7),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569),
    error = androidx.compose.ui.graphics.Color(0xFFDC2626),
    onError = TextOnBlue,
    outline = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
)

@Composable
fun SoloLevelingTheme(content: @Composable () -> Unit) {
    // Determine effective theme
    val systemDark = isSystemInDarkTheme()
    val isLight = when (ThemeState.mode) {
        1 -> false
        2 -> true
        else -> !systemDark
    }
    ThemeState.isLight = isLight

    val scheme = if (isLight) lightScheme() else darkScheme()

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
