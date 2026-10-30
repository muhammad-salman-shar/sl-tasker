package com.neurasamu.build.sl_tasker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnBlue,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGold,
    onSecondary = TextOnGold,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = AccentGold,
    tertiary = AccentCyan,
    onTertiary = TextOnGold,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = TextOnBlue,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

@Composable
fun SoloLevelingTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content
    )
}
