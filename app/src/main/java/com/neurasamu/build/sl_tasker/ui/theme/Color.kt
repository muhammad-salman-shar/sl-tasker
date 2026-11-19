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
    var mode by mutableStateOf(1)
    var isLight by mutableStateOf(false)
    var isLiteMode by mutableStateOf(false)
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

// ==== DARK-ONLY PALETTE ====
val DarkBackground: Color get() = DarkBgRaw
val DarkBackgroundDeep: Color get() = DarkBgDeepRaw
val DarkSurface: Color get() = DarkSurfaceRaw
val DarkCard: Color get() = DarkCardRaw
val DarkCardElevated: Color get() = DarkCardElevatedRaw
val DarkBorder: Color get() = DarkBorderRaw
val TextPrimary: Color get() = DarkTextPrimaryRaw
val TextSecondary: Color get() = DarkTextSecondaryRaw
val TextMuted: Color get() = DarkTextMutedRaw

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

// ==== Liquid Glass tokens ====
val GlassFill = Color(0xFFFFFFFF)
val GlassRim = Color(0xFFFFFFFF)
val GlassTintBlue = Color(0xFF3B82F6)
val GlassTintViolet = Color(0xFF8B5CF6)
val GlassTintEmerald = Color(0xFF10B981)
val GlassTintCyan = Color(0xFF00D9FF)
val GlassTintAmber = Color(0xFFF59E0B)
val GlassTintRuby = Color(0xFFEF4444)
val AmbientBase = Color(0xFF03060F)
