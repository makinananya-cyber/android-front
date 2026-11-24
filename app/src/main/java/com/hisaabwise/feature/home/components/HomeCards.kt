package com.hisaabwise.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.UnitTint
import com.hisaabwise.core.walkthrough.LocalWalkthrough
import com.hisaabwise.core.walkthrough.WalkthroughTarget
import com.hisaabwise.core.walkthrough.walkthroughTarget
import com.hisaabwise.feature.home.ArticleIcon
import com.hisaabwise.feature.home.HWMarkdown

/**
 * Whether paired layouts (ring + key, streak + reads) stack, and the meter shows its text form: the
 * Android stand-in for iOS accessibility type sizes.
 */
@Composable
internal fun stacksPairs(): Boolean = LocalDensity.current.fontScale >= 1.5f

/** The design's five article glyphs. */
internal fun ArticleIcon.vector(): ImageVector = when (this) {
    ArticleIcon.Shield -> Icons.Outlined.GppGood
    ArticleIcon.Globe -> Icons.Outlined.Public
    ArticleIcon.Steps -> Icons.Outlined.Stairs
    ArticleIcon.Lightbulb -> Icons.Outlined.Lightbulb
    ArticleIcon.Alert -> Icons.Outlined.WarningAmber
}

/**
 * Tags a tour target, scrolls it into view when the tour reaches it (iOS `hwWalkthroughScrolls`), and
 * withdraws its rect when it leaves composition — so the first-run add button stops counting as present
 * once the account has spending.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.homeTourTarget(target: WalkthroughTarget): Modifier = composed {
    val model = LocalWalkthrough.current ?: return@composed this
    val requester = remember { BringIntoViewRequester() }
    val tour by model.state.collectAsState()
    val step = model.current(tour)
    val active = step != null && (
        (step.target == target && !(step.whenPresent != null && tour.rects.containsKey(step.whenPresent))) ||
            (step.instead == target && step.whenPresent != null && tour.rects.containsKey(step.whenPresent))
        )
    LaunchedEffect(active) { if (active) runCatching { requester.bringIntoView() } }
    DisposableEffect(model, target) { onDispose { model.report(target, null) } }
    this.bringIntoViewRequester(requester).walkthroughTarget(target)
}

/** A soft radial glow in the top-trailing corner (the design's `::after` orbs). Static. */
internal fun Modifier.cornerGlow(color: Color, radius: Float = 220f): Modifier = drawBehind {
    val center = Offset(size.width - radius * 0.25f, -radius * 0.15f)
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius), radius = radius, center = center)
}

/**
 * The `.tip` card: a lightbulb on a sun tile, the tip (markdown emphasis), and **Show me another**.
 * Draws its own warm surface rather than sitting in a white card.
 */
@Composable
fun HWTipCard(text: String, onAnother: () -> Unit, modifier: Modifier = Modifier) {
    val sun = UnitTint.Sun.accent
    val reduceMotion = LocalReduceMotion.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Card)
            .background(Brush.linearGradient(listOf(sun.soft, HWColors.Surface)))
            .border(1.dp, sun.base.copy(alpha = 0.35f), HWRadius.Card)
            .cornerGlow(sun.base.copy(alpha = 0.28f))
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(HWRadius.Small).background(sun.base),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Lightbulb, null, tint = HWColors.BrandInk, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(9.dp))
            Text(
                stringResource(R.string.home_tip_caption).uppercase(),
                style = HWType.Eyebrow.copy(fontWeight = FontWeight.ExtraBold),
                color = sun.deep,
                modifier = Modifier.semantics { heading() },
            )
        }
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                val spec = tween<Float>(if (reduceMotion) 0 else 250)
                fadeIn(spec) togetherWith fadeOut(spec)
            },
            label = "tip",
        ) { shown ->
            Text(HWMarkdown.annotated(shown), style = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal, lineHeight = HWType.BodyLarge.lineHeight * 1.15f), color = HWColors.TipInk)
        }
        Row(
            Modifier
                .heightIn(min = MinTouchTarget)
                .clip(HWRadius.Small)
                .clickable(role = Role.Button, onClick = onAnother)
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Refresh, null, tint = sun.deep, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.home_tip_another), style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold), color = sun.deep)
        }
    }
}

/**
 * The `.mini` streak card — the one dark panel among the in-app cards, and one button as a whole.
 * TalkBack reads "Learning, 4 day streak. <summary>", with the next lesson as the action hint.
 */
@Composable
fun HWStreakCard(
    streak: Int,
    summary: String,
    nextLesson: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val caption = stringResource(R.string.home_learning_caption)
    val value = stringResource(R.string.home_learning_accessibilityValue, streak.toString(), summary)
    val hint = stringResource(R.string.home_learning_hint, nextLesson)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(14.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.22f), spotColor = HWColors.Ink.copy(alpha = 0.22f))
            .clip(HWRadius.Card)
            .background(
                Brush.linearGradient(
                    0f to HWColors.AccentDeep,
                    0.6f to HWColors.StreakMid,
                    1f to HWColors.Accent,
                ),
            )
            .cornerGlow(HWColors.BrandInkAccent.copy(alpha = 0.30f), radius = 190f)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = caption
                stateDescription = value
                role = Role.Button
                onClick(label = hint) { onClick(); true }
            }
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.BrandInkSecondary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = UnitTint.Sun.accent.base, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(7.dp))
            Text(streak.toString(), style = HWType.Display, color = HWColors.BrandInk)
        }
        Text(summary, style = HWType.Body, color = HWColors.BrandInkSecondary)
        Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_learning_continue), style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.BrandInkAccent)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = HWColors.BrandInkAccent, modifier = Modifier.size(18.dp))
        }
    }
}

/** One "Read more about" row: the article's glyph on a white tile, its title, a chevron. */
@Composable
fun HWReadRow(title: String, icon: ArticleIcon, accent: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = UnitTint.forIndex(accent.coerceAtLeast(1)).accent
    val hint = stringResource(R.string.home_reads_hint)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Medium)
            .background(HWColors.BackgroundSecondary)
            .border(1.dp, HWColors.SeparatorStrong, HWRadius.Medium)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = title
                role = Role.Button
                onClick(label = hint) { onClick(); true }
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(HWRadius.Small)
                .background(HWColors.Surface)
                .border(1.dp, tint.base.copy(alpha = 0.30f), HWRadius.Small),
            contentAlignment = Alignment.Center,
        ) { Icon(icon.vector(), null, tint = tint.deep, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(8.dp))
        Text(title, style = HWType.Body.copy(fontWeight = FontWeight.Bold), color = HWColors.Ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = HWColors.AccentMuted, modifier = Modifier.size(18.dp))
    }
}
