package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Global theme state. Setting isLight triggers recomposition everywhere.
 * mode: 0 = follow system, 1 = always dark, 2 = always light
 */
object ThemeState {
    var mode by mutableStateOf(0)
    var isLight by mutableStateOf(false)
}

// ==== RAW DARK PALETTE ====
private val DarkBgRaw = Color(0xFF050A18)
private val DarkBgDeepRaw = Color(0xFF02050E)
private val DarkSurfaceRaw = Color(0xFF0A1228)
private val DarkCardRaw = Color(0xFF0F1830)
private val DarkCardElevatedRaw = Color(0xFF15203A)
private val DarkBorderRaw = Color(0xFF1E2A45)
private val DarkTextPrimaryRaw = Color(0xFFE8EEF9)
private val DarkTextSecondaryRaw = Color(0xFF8B9AB8)
private val DarkTextMutedRaw = Color(0xFF5A6785)

// ==== RAW LIGHT PALETTE ====
private val LightBgRaw = Color(0xFFF5F7FB)
private val LightBgDeepRaw = Color(0xFFEAEEF5)
private val LightSurfaceRaw = Color(0xFFF0F2F7)
private val LightCardRaw = Color(0xFFFFFFFF)
private val LightCardElevatedRaw = Color(0xFFF8FAFC)
private val LightBorderRaw = Color(0xFFE2E8F0)
private val LightTextPrimaryRaw = Color(0xFF0F1830)
private val LightTextSecondaryRaw = Color(0xFF475569)
private val LightTextMutedRaw = Color(0xFF94A3B8)

// ==== DYNAMIC COLORS (compose-aware getters) ====
val DarkBackground: Color get() = if (ThemeState.isLight) LightBgRaw else DarkBgRaw
val DarkBackgroundDeep: Color get() = if (ThemeState.isLight) LightBgDeepRaw else DarkBgDeepRaw
val DarkSurface: Color get() = if (ThemeState.isLight) LightSurfaceRaw else DarkSurfaceRaw
val DarkCard: Color get() = if (ThemeState.isLight) LightCardRaw else DarkCardRaw
val DarkCardElevated: Color get() = if (ThemeState.isLight) LightCardElevatedRaw else DarkCardElevatedRaw
val DarkBorder: Color get() = if (ThemeState.isLight) LightBorderRaw else DarkBorderRaw
val TextPrimary: Color get() = if (ThemeState.isLight) LightTextPrimaryRaw else DarkTextPrimaryRaw
val TextSecondary: Color get() = if (ThemeState.isLight) LightTextSecondaryRaw else DarkTextSecondaryRaw
val TextMuted: Color get() = if (ThemeState.isLight) LightTextMutedRaw else DarkTextMutedRaw

// ==== STATIC BRAND (same in both themes) ====
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueLight = Color(0xFF60A5FA)
val PrimaryBlueDark = Color(0xFF2563EB)
val AccentGold = Color(0xFFFBBF24)
val AccentGoldLight = Color(0xFFFCD34D)
val AccentCyan = Color(0xFF00D9FF)
val SuccessGreen = Color(0xFF10B981)
val WarningOrange = Color(0xFFF59E0B)
val DangerRed = Color(0xFFEF4444)
val DangerRedLight = Color(0xFFFF6B6B)
val PurpleMagic = Color(0xFF8B5CF6)
val TextOnGold = Color(0xFF0A0F1A)
val TextOnBlue = Color(0xFFFFFFFF)

// ==== RANKS ====
val RankE = Color(0xFF94A3B8)
val RankD = Color(0xFF10B981)
val RankC = Color(0xFF3B82F6)
val RankB = Color(0xFF8B5CF6)
val RankA = Color(0xFFF59E0B)
val RankS = Color(0xFFFBBF24)

// ==== Backward-compatible aliases ====
val PrimaryManaBlue: Color get() = PrimaryBlue
val SecondaryNeonBlue: Color get() = PrimaryBlueLight
val AccentShadowPurple: Color get() = PurpleMagic
val GoldWarning: Color get() = AccentGold
val DangerPenaltyRed: Color get() = DangerRed
