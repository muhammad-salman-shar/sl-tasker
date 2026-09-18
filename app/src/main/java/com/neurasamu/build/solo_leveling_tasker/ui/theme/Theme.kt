package com.neurasamu.build.solo_leveling_tasker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryManaBlue,
    onPrimary = DarkBackground,
    primaryContainer = DarkCard,
    onPrimaryContainer = PrimaryManaBlue,
    secondary = SecondaryNeonBlue,
    onSecondary = DarkBackground,
    tertiary = AccentShadowPurple,
    onTertiary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = DangerPenaltyRed,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = SecondaryNeonBlue,
    onPrimary = TextPrimary,
    primaryContainer = DarkBorder,
    onPrimaryContainer = PrimaryManaBlue,
    secondary = PrimaryManaBlue,
    onSecondary = DarkBackground,
    tertiary = AccentShadowPurple,
    onTertiary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = DangerPenaltyRed,
    onError = TextPrimary
)

@Composable
fun SoloLevelingTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
