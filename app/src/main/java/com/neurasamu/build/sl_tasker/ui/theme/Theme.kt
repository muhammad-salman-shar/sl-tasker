package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.neurasamu.build.sl_tasker.data.model.ThemeMode

enum class AppThemeId(
    val displayName: String,
    val subtitle: String,
    val isLightPalette: Boolean,
    val isProOnly: Boolean
) {
    DARK_HUNTER("Dark Hunter", "Deep obsidian & electric cobalt", false, false),
    SERENE_SAGE("Serene Sage", "Peaceful linen & botanical sage", true, false),
    SOFT_ROSE_CREAM("Soft Rose-Cream", "Warm ivory & rosewood contrast", true, true),
    MINIMAL_LAVENDER("Minimal Lavender", "Calm lilac & royal iris ink", true, true),
    CRIMSON_MONARCH("Crimson Monarch", "Shadow monarch ruby & obsidian", false, true),
    CYBER_NEON("Cyber Neon", "Electric cyan & midnight synth", false, true),
    GOLDEN_SOVEREIGN("Golden Sovereign", "Imperial gold & onyx prestige", false, true),
    FOREST_EMERALD("Forest Emerald", "Deep pine & emerald aura", false, true)
}

data class SemanticThemeTokens(
    val id: AppThemeId,
    val isLight: Boolean,
    val bgPrimary: Color,
    val bgSecondary: Color,
    val cardBg: Color,
    val surfaceBg: Color,
    val border: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnPrimary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
)

object ThemeState {
    var mode by mutableStateOf(ThemeMode.DARK)
    var isLiteMode by mutableStateOf(false)
    var activeTheme by mutableStateOf(AppThemeId.DARK_HUNTER)

    val isLight: Boolean
        get() = activeTheme.isLightPalette

    val tokens: SemanticThemeTokens
        get() = when (activeTheme) {
            AppThemeId.DARK_HUNTER -> SemanticThemeTokens(
                id = AppThemeId.DARK_HUNTER,
                isLight = false,
                bgPrimary = Color(0xFF040710),
                bgSecondary = Color(0xFF091124),
                cardBg = Color(0xFF0D162C),
                surfaceBg = Color(0xFF13203D),
                border = Color(0xFF23355B),
                primaryAccent = Color(0xFF3B82F6),
                secondaryAccent = Color(0xFF00E5FF),
                textPrimary = Color(0xFFF1F5FF),
                textSecondary = Color(0xFFAEC0E0),
                textMuted = Color(0xFF6B7FA3),
                textOnPrimary = Color(0xFFFFFFFF),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                danger = Color(0xFFEF4444)
            )
            AppThemeId.SERENE_SAGE -> SemanticThemeTokens(
                id = AppThemeId.SERENE_SAGE,
                isLight = true,
                bgPrimary = Color(0xFFF4F7F4),
                bgSecondary = Color(0xFFE7EFE8),
                cardBg = Color(0xFFFFFFFF),
                surfaceBg = Color(0xFFE9F2EB),
                border = Color(0xFFC1D4C6),
                primaryAccent = Color(0xFF1E6F50),
                secondaryAccent = Color(0xFF0D9488),
                textPrimary = Color(0xFF13261E),
                textSecondary = Color(0xFF365447),
                textMuted = Color(0xFF5D7B6E),
                textOnPrimary = Color(0xFFFFFFFF),
                success = Color(0xFF15803D),
                warning = Color(0xFFB45309),
                danger = Color(0xFFDC2626)
            )
            AppThemeId.SOFT_ROSE_CREAM -> SemanticThemeTokens(
                id = AppThemeId.SOFT_ROSE_CREAM,
                isLight = true,
                bgPrimary = Color(0xFFFDF8F6),
                bgSecondary = Color(0xFFF7ECE7),
                cardBg = Color(0xFFFFFFFF),
                surfaceBg = Color(0xFFF5E6E0),
                border = Color(0xFFE2C6BC),
                primaryAccent = Color(0xFFB8335A),
                secondaryAccent = Color(0xFFD97706),
                textPrimary = Color(0xFF261419),
                textSecondary = Color(0xFF5E3A43),
                textMuted = Color(0xFF8A656E),
                textOnPrimary = Color(0xFFFFFFFF),
                success = Color(0xFF15803D),
                warning = Color(0xFFB45309),
                danger = Color(0xFFDC2626)
            )
            AppThemeId.MINIMAL_LAVENDER -> SemanticThemeTokens(
                id = AppThemeId.MINIMAL_LAVENDER,
                isLight = true,
                bgPrimary = Color(0xFFF6F4FC),
                bgSecondary = Color(0xFFECE7F8),
                cardBg = Color(0xFFFFFFFF),
                surfaceBg = Color(0xFFEDE8FA),
                border = Color(0xFFCCC2EB),
                primaryAccent = Color(0xFF5B3CC4),
                secondaryAccent = Color(0xFF7C3AED),
                textPrimary = Color(0xFF1B1433),
                textSecondary = Color(0xFF473B6B),
                textMuted = Color(0xFF726696),
                textOnPrimary = Color(0xFFFFFFFF),
                success = Color(0xFF15803D),
                warning = Color(0xFFB45309),
                danger = Color(0xFFDC2626)
            )
            AppThemeId.CRIMSON_MONARCH -> SemanticThemeTokens(
                id = AppThemeId.CRIMSON_MONARCH,
                isLight = false,
                bgPrimary = Color(0xFF0A0406),
                bgSecondary = Color(0xFF18080D),
                cardBg = Color(0xFF1F0B12),
                surfaceBg = Color(0xFF2D101A),
                border = Color(0xFF541C2E),
                primaryAccent = Color(0xFFF43F5E),
                secondaryAccent = Color(0xFFFB7185),
                textPrimary = Color(0xFFFFF1F3),
                textSecondary = Color(0xFFE5B3BE),
                textMuted = Color(0xFF9E6874),
                textOnPrimary = Color(0xFFFFFFFF),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                danger = Color(0xFFFF4D6D)
            )
            AppThemeId.CYBER_NEON -> SemanticThemeTokens(
                id = AppThemeId.CYBER_NEON,
                isLight = false,
                bgPrimary = Color(0xFF030812),
                bgSecondary = Color(0xFF071326),
                cardBg = Color(0xFF0B1C36),
                surfaceBg = Color(0xFF11294D),
                border = Color(0xFF1D467A),
                primaryAccent = Color(0xFF00E5FF),
                secondaryAccent = Color(0xFFD946EF),
                textPrimary = Color(0xFFECFEFF),
                textSecondary = Color(0xFFA5F3FC),
                textMuted = Color(0xFF5E9EB0),
                textOnPrimary = Color(0xFF030812),
                success = Color(0xFF10B981),
                warning = Color(0xFFFBBF24),
                danger = Color(0xFFF43F5E)
            )
            AppThemeId.GOLDEN_SOVEREIGN -> SemanticThemeTokens(
                id = AppThemeId.GOLDEN_SOVEREIGN,
                isLight = false,
                bgPrimary = Color(0xFF090805),
                bgSecondary = Color(0xFF16130B),
                cardBg = Color(0xFF1E190E),
                surfaceBg = Color(0xFF2B2414),
                border = Color(0xFF524424),
                primaryAccent = Color(0xFFFBBF24),
                secondaryAccent = Color(0xFFF59E0B),
                textPrimary = Color(0xFFFFFBEB),
                textSecondary = Color(0xFFE7D6A3),
                textMuted = Color(0xFF9C8B5C),
                textOnPrimary = Color(0xFF141005),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                danger = Color(0xFFEF4444)
            )
            AppThemeId.FOREST_EMERALD -> SemanticThemeTokens(
                id = AppThemeId.FOREST_EMERALD,
                isLight = false,
                bgPrimary = Color(0xFF040C08),
                bgSecondary = Color(0xFF091A12),
                cardBg = Color(0xFF0E2419),
                surfaceBg = Color(0xFF153324),
                border = Color(0xFF24543C),
                primaryAccent = Color(0xFF10B981),
                secondaryAccent = Color(0xFF34D399),
                textPrimary = Color(0xFFECFDF5),
                textSecondary = Color(0xFFA7F3D0),
                textMuted = Color(0xFF5E9E82),
                textOnPrimary = Color(0xFF04120B),
                success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B),
                danger = Color(0xFFEF4444)
            )
        }

    fun selectTheme(id: AppThemeId) {
        activeTheme = id
        mode = if (id.isLightPalette) ThemeMode.LIGHT else ThemeMode.DARK
    }

    fun applyStoredName(raw: String?) {
        val found = AppThemeId.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        if (found != null) selectTheme(found)
    }
}

@Composable
fun SoloLevelingTheme(
    themeMode: ThemeMode = ThemeState.mode,
    content: @Composable () -> Unit
) {
    val t = ThemeState.tokens
    val scheme = if (t.isLight) {
        lightColorScheme(
            primary = t.primaryAccent,
            onPrimary = t.textOnPrimary,
            secondary = t.secondaryAccent,
            onSecondary = t.textOnPrimary,
            tertiary = t.warning,
            background = t.bgPrimary,
            onBackground = t.textPrimary,
            surface = t.cardBg,
            onSurface = t.textPrimary,
            surfaceVariant = t.surfaceBg,
            onSurfaceVariant = t.textSecondary,
            outline = t.border,
            error = t.danger,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = t.primaryAccent,
            onPrimary = t.textOnPrimary,
            secondary = t.secondaryAccent,
            onSecondary = t.textOnPrimary,
            tertiary = t.warning,
            background = t.bgPrimary,
            onBackground = t.textPrimary,
            surface = t.cardBg,
            onSurface = t.textPrimary,
            surfaceVariant = t.surfaceBg,
            onSurfaceVariant = t.textSecondary,
            outline = t.border,
            error = t.danger,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}
