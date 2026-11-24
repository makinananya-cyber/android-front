package com.hisaabwise.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * The palette — values copied from the iOS asset catalogue (61 colour sets). Light only; dark mode
 * is a later palette swap, so use these semantic names, never raw hex at a call site.
 */
object HWColors {
    // Surface (the five in-app screens)
    val Background = Color(0xFFFFF9F0)
    val BackgroundSecondary = Color(0xFFF7F2EB)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF081F5C)
    val InkSecondary = Color(0xA8081F5C) // 0.66
    val InkTertiary = Color(0x70081F5C)  // 0.44
    val Separator = Color(0x21334EAC)    // 0.13
    val SeparatorStrong = Color(0x3D334EAC) // 0.24

    // Brand (Landing, auth)
    val BrandBackground = Color(0xFF081F5C)
    val BrandBackgroundDeep = Color(0xFF05143C)
    val BrandBackgroundLift = Color(0xFF0E2E73)
    val BrandInk = Color(0xFFFFF9F0)
    val BrandInkSecondary = Color(0xFFBAD6EB)
    val BrandInkAccent = Color(0xFFD0E3FF)
    val BrandSurface = Color(0x12D0E3FF) // 0.07
    val BrandSeparator = Color(0x29D0E3FF) // 0.16
    val BrandSeparatorStrong = Color(0x6BD0E3FF) // 0.42
    val BrandDanger = Color(0xFFFFC9C0)

    // Accent
    val Accent = Color(0xFF334EAC)
    val AccentSoft = Color(0xFFD0E3FF)
    val AccentDeep = Color(0xFF081F5C)
    val AccentMuted = Color(0xFF7096D1)
    val Tint = Color(0xFFD0E3FF)
    val TintSecondary = Color(0xFFBAD6EB)

    // Feedback
    val Danger = Color(0xFFC0453A)
    val DangerSoft = Color(0x17C0453A) // 0.09
    val EmptyTrack = Color(0xFFE5E2DA)
    val Locked = Color(0xFFC9CFDF)
    val LockedSoft = Color(0xFFE4E8F2)

    // Category slots 1–6 (the server sends `slot`)
    val CategoryRent = Color(0xFF3D5AF1)
    val CategoryGroceries = Color(0xFFF4531F)
    val CategoryTransport = Color(0xFF009B84)
    val CategoryUtilities = Color(0xFFCE8500)
    val CategoryEntertainment = Color(0xFF7C4DEF)
    val CategoryOther = Color(0xFFE0357F)
    val categories = listOf(CategoryRent, CategoryGroceries, CategoryTransport, CategoryUtilities, CategoryEntertainment, CategoryOther)

    /** Colour for a server category `slot` (1-based). */
    fun categorySlot(slot: Int): Color = categories[((slot - 1).mod(categories.size))]

    // Savings meter gradient
    val MeterNothing = Color(0xFFF2554E)
    val MeterLow = Color(0xFFFA8A47)
    val MeterHalf = Color(0xFFF9C548)
    val MeterHigh = Color(0xFFA2CB42)
    val MeterReached = Color(0xFF1FC186)
    val meterStops = listOf(MeterNothing, MeterLow, MeterHalf, MeterHigh, MeterReached)

    // Verdicts (hit / near / miss)
    val VerdictHitSoft = Color(0xFFDEF6EC)
    val VerdictHitInk = Color(0xFF0E6B4C)
    val VerdictNearSoft = Color(0xFFFEF1D7)
    val VerdictNearInk = Color(0xFF875507)
    val VerdictMissSoft = Color(0xFFFDE7E4)
    val VerdictMissInk = Color(0xFF96301F)

    // Tip card
    val TipInk = Color(0xFF5E4409)

    // Streak gradient
    val StreakMid = Color(0xFF16357C)
}

/** A Learn unit's accent (sun · mint · coral · sky · violet). */
data class UnitAccent(val base: Color, val soft: Color, val deep: Color)

enum class UnitTint(val accent: UnitAccent) {
    Sun(UnitAccent(Color(0xFFF0B429), Color(0xFFFFF1D4), Color(0xFFC98A10))),
    Mint(UnitAccent(Color(0xFF2FA37C), Color(0xFFDAF2E9), Color(0xFF1E7B5C))),
    Coral(UnitAccent(Color(0xFFE8705C), Color(0xFFFDE3DD), Color(0xFFC24E3B))),
    Sky(UnitAccent(HWColors.Accent, HWColors.AccentSoft, HWColors.AccentDeep)),
    Violet(UnitAccent(Color(0xFF7B6BD6), Color(0xFFE9E5FB), Color(0xFF5B4CB0)));

    companion object {
        /** Articles and units also arrive as a 1-based accent number. */
        fun forIndex(index: Int): UnitTint = entries[(index - 1).mod(entries.size)]
    }
}

/** hit / near / miss tones for verdict badges and trend bars. */
enum class VerdictTint(val soft: Color, val ink: Color, val bar: Color) {
    Hit(HWColors.VerdictHitSoft, HWColors.VerdictHitInk, HWColors.MeterReached),
    Near(HWColors.VerdictNearSoft, HWColors.VerdictNearInk, HWColors.MeterHalf),
    Miss(HWColors.VerdictMissSoft, HWColors.VerdictMissInk, HWColors.MeterNothing),
}
