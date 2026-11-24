package com.hisaabwise.feature.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWMark
import com.hisaabwise.feature.auth.components.AuthPage
import com.hisaabwise.feature.auth.components.BrandHeadlineBrush
import com.hisaabwise.feature.auth.components.CtaGlow
import com.hisaabwise.feature.auth.components.FlexSpace
import com.hisaabwise.feature.auth.components.authEnters
import kotlinx.coroutines.delay

/** The four straplines, in the design's order (the first is a [FIX] that names no currency). */
private val STRAPLINES = listOf(
    R.string.landing_strapline_1,
    R.string.landing_strapline_2,
    R.string.landing_strapline_3,
    R.string.landing_strapline_4,
)

@Composable
fun LandingRoute(onGetStarted: () -> Unit) {
    val vm = viewModel { LandingViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effects.collect { if (it is LandingEffect.GoToSignIn) onGetStarted() }
    }
    LandingScreen(state, vm::dispatch)
}

/**
 * The first thing anyone sees (port of iOS `LandingView`): wordmark, hero in a glass card,
 * headline with a rotating strapline, and the call to action over its footnote. Under Reduce
 * Motion nothing moves at all, including the strapline: the first one stays for the session.
 */
@Composable
fun LandingScreen(state: LandingState, onIntent: (LandingIntent) -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    // The clock lives with the screen; the view model only owns the index. Keyed on the setting so
    // turning Reduce Motion off mid-session starts the rotation.
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(LandingState.ROTATION_MILLIS)
            onIntent(LandingIntent.Advance)
        }
    }

    AuthPage {
        Wordmark(Modifier.authEnters(0))
        FlexSpace(12.dp)
        Hero(reduceMotion, Modifier.authEnters(1))
        FlexSpace(18.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Headline(Modifier.authEnters(2))
            Strapline(state.strapline, reduceMotion, Modifier.authEnters(3))
        }
        FlexSpace(22.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.authEnters(4)) {
                CtaGlow {
                    HWButton(
                        stringResource(R.string.landing_cta),
                        onClick = { onIntent(LandingIntent.GetStarted) },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                    )
                }
            }
            Text(
                stringResource(R.string.landing_free),
                style = HWType.Caption.copy(letterSpacing = 0.2.sp),
                color = HWColors.BrandInkSecondary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.authEnters(5),
            )
        }
    }
}

/** `.brand-row` — the mark beside "Hisaab**Wise**"; one element that says the name once. */
@Composable
private fun Wordmark(modifier: Modifier) {
    val name = stringResource(R.string.shell_mark_accessibilityLabel)
    Row(
        modifier.clearAndSetSemantics { contentDescription = name },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HWMark(34.dp)
        // One text, two colours, so the brand name never reorders under RTL.
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = HWColors.BrandInk)) { append("Hisaab") }
                withStyle(SpanStyle(color = HWColors.BrandInkAccent)) { append("Wise") }
            },
            style = HWType.Subheading.copy(letterSpacing = 0.6.sp),
        )
    }
}

/** `.hero-card` — the illustration in a glass card, floating ±5 dp over 6 s (still under Reduce Motion). */
@Composable
private fun Hero(reduceMotion: Boolean, modifier: Modifier) {
    val lift = with(LocalDensity.current) { 5.dp.toPx() }
    val offset = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "float")
        val value by transition.animateFloat(
            initialValue = lift,
            targetValue = -lift,
            animationSpec = infiniteRepeatable(tween(6000, easing = HWMotion.EaseInOut), RepeatMode.Reverse),
            label = "float",
        )
        value
    }
    Box(
        modifier
            .graphicsLayer { translationY = offset }
            .widthIn(max = 344.dp)
            .fillMaxWidth()
            .shadow(18.dp, HWRadius.Card, ambientColor = HWColors.BrandBackgroundDeep, spotColor = HWColors.BrandBackgroundDeep)
            .clip(HWRadius.Card)
            .background(HWColors.BrandBackgroundLift)
            .background(HWColors.BrandSurface)
            .border(1.dp, HWColors.BrandSeparator, HWRadius.Card)
            .padding(horizontal = 14.dp, vertical = 18.dp),
    ) {
        Image(
            painterResource(R.drawable.auth_hero_book),
            contentDescription = stringResource(R.string.landing_hero_accessibilityLabel),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** `.headline` — two lines, the second in the sky→venus→sky wash; the screen's heading. */
@Composable
private fun Headline(modifier: Modifier) {
    val style = HWType.Title.copy(letterSpacing = (-0.4).sp, textAlign = TextAlign.Center)
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.landing_headline_first), style = style, color = HWColors.BrandInk)
        Text(stringResource(R.string.landing_headline_second), style = style.copy(brush = BrandHeadlineBrush))
    }
}

/**
 * `.pill` — a pulsing dot and the rotating sentence. The pill is one labelled element whose
 * *value* is the sentence, so TalkBack reads it on focus and does not re-announce every 3.8 s.
 */
@Composable
private fun Strapline(index: Int, reduceMotion: Boolean, modifier: Modifier) {
    val label = stringResource(R.string.landing_strapline_accessibilityLabel)
    val sentence = stringResource(STRAPLINES[index.coerceIn(STRAPLINES.indices)])
    Row(
        modifier
            .widthIn(max = 334.dp)
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(HWRadius.Large)
            .background(HWColors.BrandSurface)
            .border(1.dp, HWColors.BrandSeparator, HWRadius.Large)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = sentence
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PulseDot(reduceMotion)
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                // Out upward, in from below — the design's `.out` / `.in-up`.
                (slideInVertically(tween(HWMotion.MEDIUM, easing = HWMotion.EaseInOut)) { it / 2 } + fadeIn(tween(HWMotion.MEDIUM))) togetherWith
                    (slideOutVertically(tween(HWMotion.MEDIUM, easing = HWMotion.EaseInOut)) { -it / 2 } + fadeOut(tween(HWMotion.MEDIUM)))
            },
            modifier = Modifier.weight(1f),
            label = "strapline",
        ) { shown ->
            Text(
                stringResource(STRAPLINES[shown.coerceIn(STRAPLINES.indices)]),
                style = HWType.BodyLarge,
                color = HWColors.BrandInkSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** `.pill-dot` with its ping: a ring expanding out of the dot and fading, every 2.2 s. */
@Composable
private fun PulseDot(reduceMotion: Boolean) {
    val ping = if (reduceMotion) {
        null
    } else {
        val transition = rememberInfiniteTransition(label = "ping")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2200, easing = HWMotion.EaseOut), RepeatMode.Restart),
            label = "ping",
        )
        value
    }
    Box(
        Modifier
            .size(7.dp)
            .drawBehind {
                if (ping != null) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        HWColors.BrandInkAccent.copy(alpha = 0.55f * (1f - ping)),
                        radius = r * (1f + 2.6f * ping),
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            .clip(CircleShape)
            .background(HWColors.BrandInkAccent),
    )
}
