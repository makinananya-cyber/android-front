package com.hisaabwise.core.designsystem

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The iOS type scale (system font, scales with the user's font size). */
object HWType {
    val Display = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.ExtraBold)
    val Title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
    val Heading = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
    val Subheading = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
    val BodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal)
    val Caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val Micro = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)

    /** Small caps eyebrow ("WHERE YOUR MONEY WENT"). */
    val Eyebrow = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
}

/** Corner radii: 3 / 11 / 14 / 18 / 24 / pill. */
object HWRadius {
    val Hairline = RoundedCornerShape(3.dp)
    val Small = RoundedCornerShape(11.dp)
    val Medium = RoundedCornerShape(14.dp)
    val Large = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Motion curves and durations from the iOS design system. */
object HWMotion {
    val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOut = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
    val EaseBack = CubicBezierEasing(0.34f, 1.4f, 0.5f, 1f)
    const val QUICK = 200
    const val SHORT = 250
    const val MEDIUM = 420
    const val LONG = 500
}

/** True when the user turned animations off ("Remove animations"): no confetti, no rotation, fades only. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Minimum touch target, as on iOS (44 pt). */
val MinTouchTarget = 44.dp

@Composable
fun HisaabWiseTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val colors = lightColorScheme(
        primary = HWColors.Accent,
        onPrimary = HWColors.Surface,
        primaryContainer = HWColors.AccentSoft,
        onPrimaryContainer = HWColors.Ink,
        secondary = HWColors.AccentMuted,
        background = HWColors.Background,
        onBackground = HWColors.Ink,
        surface = HWColors.Surface,
        onSurface = HWColors.Ink,
        surfaceVariant = HWColors.BackgroundSecondary,
        onSurfaceVariant = HWColors.InkSecondary,
        outline = HWColors.SeparatorStrong,
        outlineVariant = HWColors.Separator,
        error = HWColors.Danger,
    )
    val typography = Typography(
        displayLarge = HWType.Display,
        headlineLarge = HWType.Title,
        headlineMedium = HWType.Heading,
        titleLarge = HWType.Subheading,
        titleMedium = HWType.BodyLarge,
        bodyLarge = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal),
        bodyMedium = HWType.Body,
        labelLarge = HWType.BodyLarge,
        labelMedium = HWType.Caption,
        labelSmall = HWType.Micro,
    )
    val shapes = Shapes(
        extraSmall = HWRadius.Hairline,
        small = HWRadius.Small,
        medium = HWRadius.Medium,
        large = HWRadius.Large,
        extraLarge = HWRadius.Card,
    )
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
    }
}
