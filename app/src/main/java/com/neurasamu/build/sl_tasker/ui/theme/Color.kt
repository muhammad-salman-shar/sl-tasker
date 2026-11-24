package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppThemeId(
    val displayName: String,
    val subtitle: String,
    val isLight: Boolean,
    val isProOnly: Boolean
) {
    HUNTER_DARK("Dark Hunter", "Deep obsidian & cobalt", false, false),
    SERENE_SAGE("Serene Sage", "Peaceful linen & botanical sage", true, false),
    ROSE_CREAM("Soft Rose-Cream", "Warm ivory & blush rose", true, true),
    MINIMAL_LAVENDER("Minimal Lavender", "Calm lilac & royal iris", true, true),
    CRIMSON_MONARCH("Crimson Monarch", "Shadow monarch ruby & obsidian", false, true),
    CYBER_NEON("Cyber Neon", "Electric cyan & midnight synth", false, true),
    GOLDEN_SOVEREIGN("Golden Sovereign", "Imperial gold & onyx prestige", false, true),
    FOREST_EMERALD("Forest Emerald", "Deep pine & emerald aura", false, true);

    companion object {
        fun fromStored(raw: String?): AppThemeId {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: HUNTER_DARK
        }
    }
}

data class SemanticColorTokens(
    val background: Color,
    val backgroundDeep: Color,
    val surface: Color,
    val cardBg: Color,
    val cardElevated: Color,
    val onSurfacePrimary: Color,
    val onSurfaceSecondary: Color,
    val onSurfaceMuted: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val onAccent: Color,
    val border: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val danger: Color,
    val onDanger: Color,
    val success: Color,
    val warning: Color,
    val purpleAccent: Color,
    val cyanAccent: Color
)

private val HunterDarkTokens = SemanticColorTokens(
    background = Color(0xFF050A18),
    backgroundDeep = Color(0xFF02050E),
    surface = Color(0xFF0A1228),
    cardBg = Color(0xFF0F1830),
    cardElevated = Color(0xFF15203A),
    onSurfacePrimary = Color(0xFFE8EEF9),
    onSurfaceSecondary = Color(0xFF9AA8C4),
    onSurfaceMuted = Color(0xFF6E7D9E),
    accent = Color(0xFF3B82F6),
    accentLight = Color(0xFF60A5FA),
    accentDark = Color(0xFF2563EB),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFF1E2A45),
    badgeBg = Color(0xFF162447),
    badgeText = Color(0xFF93C5FD),
    danger = Color(0xFFEF4444),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    purpleAccent = Color(0xFF8B5CF6),
    cyanAccent = Color(0xFF00D9FF)
)

private val RoseCreamTokens = SemanticColorTokens(
    background = Color(0xFFFDF6F7),
    backgroundDeep = Color(0xFFF7EAED),
    surface = Color(0xFFF5E6EA),
    cardBg = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFFF8F9),
    onSurfacePrimary = Color(0xFF2B121A),
    onSurfaceSecondary = Color(0xFF5C3642),
    onSurfaceMuted = Color(0xFF734A57),
    accent = Color(0xFFBE3E64),
    accentLight = Color(0xFFD6567C),
    accentDark = Color(0xFF9B2C4E),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFFE5C8CF),
    badgeBg = Color(0xFFFCE7EE),
    badgeText = Color(0xFF8E2446),
    danger = Color(0xFFC81E3A),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    purpleAccent = Color(0xFF7E3AF2),
    cyanAccent = Color(0xFF0E7490)
)

private val MinimalLavenderTokens = SemanticColorTokens(
    background = Color(0xFFF6F4FC),
    backgroundDeep = Color(0xFFECE7F8),
    surface = Color(0xFFEBE5F7),
    cardBg = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFFAF8FF),
    onSurfacePrimary = Color(0xFF1C1635),
    onSurfaceSecondary = Color(0xFF443B6B),
    onSurfaceMuted = Color(0xFF5E5487),
    accent = Color(0xFF6344C6),
    accentLight = Color(0xFF7C5CE0),
    accentDark = Color(0xFF4E32A8),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFFD4CAEE),
    badgeBg = Color(0xFFEDE9FE),
    badgeText = Color(0xFF4C2FA3),
    danger = Color(0xFFC81E3A),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    purpleAccent = Color(0xFF6D28D9),
    cyanAccent = Color(0xFF0369A1)
)

private val SereneSageTokens = SemanticColorTokens(
    background = Color(0xFFF3F8F5),
    backgroundDeep = Color(0xFFE4EFE9),
    surface = Color(0xFFE3EEE8),
    cardBg = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF8FCFA),
    onSurfacePrimary = Color(0xFF122920),
    onSurfaceSecondary = Color(0xFF345447),
    onSurfaceMuted = Color(0xFF4C6E60),
    accent = Color(0xFF1F7A5A),
    accentLight = Color(0xFF2B946F),
    accentDark = Color(0xFF165B43),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFFC2D8CC),
    badgeBg = Color(0xFFDCFCE7),
    badgeText = Color(0xFF14533C),
    danger = Color(0xFFC81E3A),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    purpleAccent = Color(0xFF6D28D9),
    cyanAccent = Color(0xFF0F766E)
)

private val CrimsonMonarchTokens = SemanticColorTokens(
    background = Color(0xFF0A0406),
    backgroundDeep = Color(0xFF050203),
    surface = Color(0xFF18080D),
    cardBg = Color(0xFF1F0B12),
    cardElevated = Color(0xFF2B1019),
    onSurfacePrimary = Color(0xFFFFF1F3),
    onSurfaceSecondary = Color(0xFFE5B3BE),
    onSurfaceMuted = Color(0xFF9E6874),
    accent = Color(0xFFF43F5E),
    accentLight = Color(0xFFFB7185),
    accentDark = Color(0xFFE11D48),
    onAccent = Color(0xFFFFFFFF),
    border = Color(0xFF541C2E),
    badgeBg = Color(0xFF38111E),
    badgeText = Color(0xFFFDA4AF),
    danger = Color(0xFFFF4D6D),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF22C55E),
    warning = Color(0xFFF59E0B),
    purpleAccent = Color(0xFFC084FC),
    cyanAccent = Color(0xFF38BDF8)
)

private val CyberNeonTokens = SemanticColorTokens(
    background = Color(0xFF030812),
    backgroundDeep = Color(0xFF01040A),
    surface = Color(0xFF071326),
    cardBg = Color(0xFF0B1C36),
    cardElevated = Color(0xFF11294D),
    onSurfacePrimary = Color(0xFFECFEFF),
    onSurfaceSecondary = Color(0xFFA5F3FC),
    onSurfaceMuted = Color(0xFF5E9EB0),
    accent = Color(0xFF00E5FF),
    accentLight = Color(0xFF67E8F9),
    accentDark = Color(0xFF06B6D4),
    onAccent = Color(0xFF030812),
    border = Color(0xFF1D467A),
    badgeBg = Color(0xFF0E294D),
    badgeText = Color(0xFF67E8F9),
    danger = Color(0xFFF43F5E),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF10B981),
    warning = Color(0xFFFBBF24),
    purpleAccent = Color(0xFFD946EF),
    cyanAccent = Color(0xFF22D3EE)
)

private val GoldenSovereignTokens = SemanticColorTokens(
    background = Color(0xFF090805),
    backgroundDeep = Color(0xFF040302),
    surface = Color(0xFF16130B),
    cardBg = Color(0xFF1E190E),
    cardElevated = Color(0xFF2B2414),
    onSurfacePrimary = Color(0xFFFFFBEB),
    onSurfaceSecondary = Color(0xFFE7D6A3),
    onSurfaceMuted = Color(0xFF9C8B5C),
    accent = Color(0xFFFBBF24),
    accentLight = Color(0xFFFDE68A),
    accentDark = Color(0xFFD97706),
    onAccent = Color(0xFF141005),
    border = Color(0xFF524424),
    badgeBg = Color(0xFF332A14),
    badgeText = Color(0xFFFDE68A),
    danger = Color(0xFFEF4444),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF22C55E),
    warning = Color(0xFFF59E0B),
    purpleAccent = Color(0xFFA855F7),
    cyanAccent = Color(0xFF38BDF8)
)

private val ForestEmeraldTokens = SemanticColorTokens(
    background = Color(0xFF040C08),
    backgroundDeep = Color(0xFF020604),
    surface = Color(0xFF091A12),
    cardBg = Color(0xFF0E2419),
    cardElevated = Color(0xFF153324),
    onSurfacePrimary = Color(0xFFECFDF5),
    onSurfaceSecondary = Color(0xFFA7F3D0),
    onSurfaceMuted = Color(0xFF5E9E82),
    accent = Color(0xFF10B981),
    accentLight = Color(0xFF34D399),
    accentDark = Color(0xFF059669),
    onAccent = Color(0xFF04120B),
    border = Color(0xFF24543C),
    badgeBg = Color(0xFF133625),
    badgeText = Color(0xFF6EE7B7),
    danger = Color(0xFFEF4444),
    onDanger = Color(0xFFFFFFFF),
    success = Color(0xFF22C55E),
    warning = Color(0xFFF59E0B),
    purpleAccent = Color(0xFF8B5CF6),
    cyanAccent = Color(0xFF2DD4BF)
)

object ThemeState {
    var activeTheme by mutableStateOf(AppThemeId.HUNTER_DARK)
    var mode by mutableStateOf(1)
    var isLiteMode by mutableStateOf(false)

    val isLight: Boolean
        get() = activeTheme.isLight

    val tokens: SemanticColorTokens
        get() = when (activeTheme) {
            AppThemeId.HUNTER_DARK -> HunterDarkTokens
            AppThemeId.SERENE_SAGE -> SereneSageTokens
            AppThemeId.ROSE_CREAM -> RoseCreamTokens
            AppThemeId.MINIMAL_LAVENDER -> MinimalLavenderTokens
            AppThemeId.CRIMSON_MONARCH -> CrimsonMonarchTokens
            AppThemeId.CYBER_NEON -> CyberNeonTokens
            AppThemeId.GOLDEN_SOVEREIGN -> GoldenSovereignTokens
            AppThemeId.FOREST_EMERALD -> ForestEmeraldTokens
        }

    fun selectTheme(themeId: AppThemeId) {
        activeTheme = themeId
        mode = if (themeId.isLight) 2 else 1
    }

    fun applyStoredName(raw: String?) {
        val found = AppThemeId.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        if (found != null) selectTheme(found)
    }
}

// ==== SEMANTIC PALETTE TOKENS (DYNAMICALLY RESOLVED) ====
val DarkBackground: Color get() = ThemeState.tokens.background
val DarkBackgroundDeep: Color get() = ThemeState.tokens.backgroundDeep
val DarkSurface: Color get() = ThemeState.tokens.surface
val DarkCard: Color get() = ThemeState.tokens.cardBg
val DarkCardElevated: Color get() = ThemeState.tokens.cardElevated
val DarkBorder: Color get() = ThemeState.tokens.border
val TextPrimary: Color get() = ThemeState.tokens.onSurfacePrimary
val TextSecondary: Color get() = ThemeState.tokens.onSurfaceSecondary
val TextMuted: Color get() = ThemeState.tokens.onSurfaceMuted

val BadgeBg: Color get() = ThemeState.tokens.badgeBg
val BadgeText: Color get() = ThemeState.tokens.badgeText

// ==== BRAND & STATUS TOKENS ====
val PrimaryBlue: Color get() = ThemeState.tokens.accent
val PrimaryBlueLight: Color get() = ThemeState.tokens.accentLight
val PrimaryBlueDark: Color get() = ThemeState.tokens.accentDark
val AccentGold = Color(0xFFD97706)
val AccentGoldLight = Color(0xFFFBBF24)
val AccentCyan: Color get() = ThemeState.tokens.cyanAccent
val SuccessGreen: Color get() = ThemeState.tokens.success
val WarningOrange: Color get() = ThemeState.tokens.warning
val DangerRed: Color get() = ThemeState.tokens.danger
val DangerRedLight = Color(0xFFFF6B6B)
val PurpleMagic: Color get() = ThemeState.tokens.purpleAccent
val TextOnGold = Color(0xFF0A0F1A)
val TextOnBlue: Color get() = ThemeState.tokens.onAccent
val OnDanger: Color get() = ThemeState.tokens.onDanger

// ==== RANKS ====
val RankE = Color(0xFF64748B)
val RankD = Color(0xFF10B981)
val RankC = Color(0xFF2563EB)
val RankB = Color(0xFF7C3AED)
val RankA = Color(0xFFD97706)
val RankS = Color(0xFFEAB308)

// ==== Backward-compatible aliases ====
val PrimaryManaBlue: Color get() = PrimaryBlue
val SecondaryNeonBlue: Color get() = PrimaryBlueLight
val AccentShadowPurple: Color get() = PurpleMagic
val GoldWarning: Color get() = AccentGold
val DangerPenaltyRed: Color get() = DangerRed

// ==== Liquid Glass tokens ====
val GlassFill: Color get() = Color(0xFFFFFFFF)
val GlassRim: Color get() = if (ThemeState.isLight) ThemeState.tokens.border else Color(0xFFFFFFFF)
val GlassTintBlue: Color get() = ThemeState.tokens.accent
val GlassTintViolet: Color get() = ThemeState.tokens.purpleAccent
val GlassTintEmerald: Color get() = ThemeState.tokens.success
val GlassTintCyan: Color get() = ThemeState.tokens.cyanAccent
val GlassTintAmber = Color(0xFFF59E0B)
val GlassTintRuby: Color get() = ThemeState.tokens.danger
val AmbientBase: Color get() = ThemeState.tokens.backgroundDeep
