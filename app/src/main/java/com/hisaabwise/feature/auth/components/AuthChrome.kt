package com.hisaabwise.feature.auth.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.components.BrandGround
import com.hisaabwise.core.designsystem.components.HWIconButton

/**
 * The brand ground plus the iOS `HWBrandGround` aurora: a planetary bloom off the top-left, a muted
 * one off the bottom-right, and a still eight-star field. Decoration only.
 */
@Composable
fun AuthGround(content: @Composable () -> Unit) {
    BrandGround {
        Box(
            Modifier
                .matchParentSize()
                .clearAndSetSemantics { }
                .drawBehind {
                    fun blob(colour: Color, alpha: Float, diameterDp: Float, center: Offset) {
                        val r = diameterDp * density / 2f
                        drawCircle(
                            Brush.radialGradient(
                                0f to colour.copy(alpha = alpha),
                                0.68f to colour.copy(alpha = 0f),
                                center = center,
                                radius = r,
                            ),
                            radius = r,
                            center = center,
                        )
                    }
                    // `.blob--planetary`: 430 dp, centred at (90, 60) dp off the top-left corner.
                    blob(HWColors.Accent, 0.85f, 430f, Offset(90f * density, 60f * density))
                    // `.blob--universe`: 450 dp, centred 70 dp in from the right and 40 dp up from the bottom.
                    blob(HWColors.AccentMuted, 0.55f, 450f, Offset(size.width - 70f * density, size.height - 40f * density))
                    STARS.forEach { (x, y, d, ink) ->
                        drawCircle(ink.copy(alpha = 0.5f), radius = d * density / 2f, center = Offset(size.width * x, size.height * y))
                    }
                },
        )
        content()
    }
}

private data class Star(val x: Float, val y: Float, val diameter: Float, val ink: Color)

private val STARS = listOf(
    Star(0.20f, 0.12f, 1.4f, HWColors.BrandInk),
    Star(0.68f, 0.24f, 1.2f, HWColors.BrandInkAccent),
    Star(0.42f, 0.38f, 1.0f, HWColors.BrandInkSecondary),
    Star(0.84f, 0.55f, 1.6f, HWColors.BrandInk),
    Star(0.12f, 0.62f, 1.1f, HWColors.BrandInkAccent),
    Star(0.56f, 0.78f, 1.3f, HWColors.BrandInkSecondary),
    Star(0.30f, 0.88f, 1.0f, HWColors.BrandInk),
    Star(0.90f, 0.90f, 1.2f, HWColors.BrandInkAccent),
)

/**
 * The scroll-with-a-minimum-height-of-one-screen layout every auth screen uses (iOS: a ScrollView
 * whose content has `minHeight: proxy.size.height`). When everything fits, the weighted spacers
 * spread the bands like the design's `space-between`; when the type grows, the screen scrolls.
 */
@Composable
fun AuthPage(
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AuthGround {
        // The back button sits in a fixed bar above the scrolling page, so content never slides under it.
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            if (onBack != null) {
                Box(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp)) {
                    HWIconButton(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.auth_back),
                        onBack,
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val minHeight = maxHeight
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = minHeight)
                            .padding(horizontal = 22.dp, vertical = if (onBack != null) 8.dp else 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content,
                    )
                }
            }
        }
    }
}

/** The sky→venus→sky wash the second headline line carries. */
val BrandHeadlineBrush: Brush
    get() = Brush.linearGradient(listOf(HWColors.BrandInkAccent, HWColors.BrandInkSecondary, HWColors.BrandInkAccent))

/** `.head` — a two-line title (second line in the wash) over a subtitle; one heading for TalkBack. */
@Composable
fun AuthHead(first: String, second: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { heading() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column {
            Text(first, style = HWType.Title.copy(letterSpacing = (-0.5).sp), color = HWColors.BrandInk)
            Text(second, style = HWType.Title.copy(letterSpacing = (-0.5).sp, brush = BrandHeadlineBrush))
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal),
                color = HWColors.BrandInkSecondary.copy(alpha = 0.9f),
            )
        }
    }
}

/** `.hairline` — a one-pixel gradient rule. Decoration. */
@Composable
fun AuthHairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, HWColors.BrandSeparator, Color.Transparent)))
            .clearAndSetSemantics { },
    )
}

/**
 * `.cta-glow` — a blurred sky pill behind a primary button, outside its bounds. Drawn only where
 * Compose can blur (API 31+); older devices get the button without the glow.
 */
@Composable
fun CtaGlow(content: @Composable () -> Unit) {
    Box {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(start = 18.dp, end = 18.dp, top = 6.dp)
                    .blur(24.dp)
                    .graphicsLayer { alpha = 0.5f }
                    .background(HWColors.BrandInkAccent, HWRadius.Pill)
                    .clearAndSetSemantics { },
            )
        }
        content()
    }
}

/**
 * The entrance stagger (iOS `hwEnters(step:)`): each band fades in and rises 12 dp, 70 ms after
 * the one before it. Nothing moves under Reduce Motion.
 */
fun Modifier.authEnters(step: Int): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val rise = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!reduceMotion) progress.animateTo(1f, tween(HWMotion.LONG, delayMillis = step * 70, easing = HWMotion.EaseOut))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

/** A form-level failure on the brand ground: danger ink, body size, centred. */
@Composable
fun AuthFormFailure(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = HWType.Body,
        color = HWColors.BrandDanger,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

/** A secondary-ink caption (support hints, `.hint`). */
@Composable
fun AuthNote(text: String, modifier: Modifier = Modifier, centred: Boolean = true, alpha: Float = 1f, style: TextStyle = HWType.Caption) {
    Text(
        text,
        style = style,
        color = HWColors.BrandInkSecondary.copy(alpha = alpha),
        textAlign = if (centred) TextAlign.Center else TextAlign.Start,
        modifier = modifier.fillMaxWidth(),
    )
}

/** The design's `space-between` gap: at least [min], and a share of whatever height is left. */
@Composable
fun ColumnScope.FlexSpace(min: Dp) {
    Spacer(Modifier.height(min))
    Spacer(Modifier.weight(1f))
}

/** `HWLink` — an inline text action in the accent ink, at least 44 dp tall. */
@Composable
fun AuthLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchTarget),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = HWColors.BrandInkAccent),
    ) {
        Text(text, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold))
    }
}
