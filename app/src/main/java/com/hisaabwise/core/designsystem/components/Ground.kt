package com.hisaabwise.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors

/**
 * Two appearances, as on iOS: [Brand] (dark galaxy ground: Landing, Sign in, Registration,
 * Forgot password) and [Surface] (light cream: the five tabs). Components read [LocalAppearance]
 * to pick their colours.
 */
enum class Appearance { Brand, Surface }

val LocalAppearance = staticCompositionLocalOf { Appearance.Surface }

/** The light cream ground with soft blue/peach blooms. */
@Composable
fun SurfaceGround(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalAppearance provides Appearance.Surface) {
        Box(
            modifier
                .fillMaxSize()
                .background(HWColors.Background)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(listOf(HWColors.AccentSoft.copy(alpha = 0.55f), Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.7f),
                        radius = size.width * 0.7f, center = Offset(size.width, 0f),
                    )
                    drawCircle(
                        Brush.radialGradient(listOf(HWColors.AccentSoft.copy(alpha = 0.35f), Color.Transparent), center = Offset(0f, size.height), radius = size.width * 0.6f),
                        radius = size.width * 0.6f, center = Offset(0f, size.height),
                    )
                },
            content = content,
        )
    }
}

/** The galaxy-navy ground used before sign-in. */
@Composable
fun BrandGround(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalAppearance provides Appearance.Brand) {
        Box(
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(HWColors.BrandBackgroundLift, HWColors.BrandBackground, HWColors.BrandBackgroundDeep)))
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(listOf(HWColors.Accent.copy(alpha = 0.45f), Color.Transparent), center = Offset(size.width * 0.85f, size.height * 0.1f), radius = size.width * 0.8f),
                        radius = size.width * 0.8f, center = Offset(size.width * 0.85f, size.height * 0.1f),
                    )
                },
            content = content,
        )
    }
}

/** The HisaabWise mark (app logo). */
@Composable
fun HWMark(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.hw_mark),
        contentDescription = stringResource(R.string.shell_mark_accessibilityLabel),
        modifier = modifier.size(size),
    )
}
