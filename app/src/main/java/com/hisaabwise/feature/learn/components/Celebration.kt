package com.hisaabwise.feature.learn.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.UnitTint

// region Stat chip

enum class StatChipTone { Streak, Experience }

/** The design's `.stat` pill — streak or XP. The figure and the sentence are the server's. */
@Composable
fun StatChip(tone: StatChipTone, value: String, accessibilityLabel: String, modifier: Modifier = Modifier) {
    val sun = UnitTint.Sun.accent
    val (icon, glyphInk, ink) = when (tone) {
        StatChipTone.Streak -> Triple(Icons.Filled.LocalFireDepartment, sun.base, sun.deep)
        StatChipTone.Experience -> Triple(Icons.Filled.Star, HWColors.AccentMuted, HWColors.Accent)
    }
    Row(
        modifier
            .shadow(3.dp, HWRadius.Medium, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Medium)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Medium)
            .clearAndSetSemantics { contentDescription = accessibilityLabel }
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = glyphInk, modifier = Modifier.size(18.dp))
        Text(value, style = HWType.Body.copy(fontWeight = FontWeight.Black), color = ink)
    }
}

// endregion

// region Completion pieces

/** `.done-badge` — the 110 dp tick. Springs in unless reduce motion is on. */
@Composable
fun DoneBadge(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(if (reduceMotion) 1f else 0.4f) }
    LaunchedEffect(Unit) { if (!reduceMotion) scale.animateTo(1f, tween(HWMotion.LONG, easing = HWMotion.EaseBack)) }
    val sun = UnitTint.Sun.accent
    Box(
        modifier
            .size(110.dp)
            .scale(scale.value)
            .shadow(12.dp, CircleShape, ambientColor = sun.deep.copy(alpha = 0.4f), spotColor = sun.deep.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(sun.base, sun.deep)))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, null, tint = HWColors.BrandInk, modifier = Modifier.size(56.dp))
    }
}

enum class CompletionStatTone { Experience, Accuracy, Streak }

/** `.dstat` — one of the three tiles. */
@Composable
fun CompletionStat(tone: CompletionStatTone, value: String, caption: String, accessibilityLabel: String, modifier: Modifier = Modifier) {
    val ink = when (tone) {
        CompletionStatTone.Experience -> HWColors.Accent
        CompletionStatTone.Accuracy -> UnitTint.Mint.accent.deep
        CompletionStatTone.Streak -> UnitTint.Sun.accent.deep
    }
    Column(
        modifier
            .shadow(3.dp, HWRadius.Large, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Large)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Large)
            .clearAndSetSemantics { contentDescription = accessibilityLabel }
            .padding(horizontal = 10.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(value, style = HWType.Heading, color = ink, textAlign = TextAlign.Center)
        Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted, textAlign = TextAlign.Center)
    }
}

/** `.done-stats` — three tiles of equal height; a column at accessibility font scales. */
@Composable
fun CompletionStats(tiles: List<@Composable (Modifier) -> Unit>, modifier: Modifier = Modifier) {
    if (isAccessibilityFontScale()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.forEach { it(Modifier.fillMaxWidth()) }
        }
    } else {
        Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
        }
    }
}

data class WeekDay(val label: String, val isComplete: Boolean, val isToday: Boolean, val accessibilityLabel: String)

/** `.week` — seven server-decided days; rows at accessibility font scales. */
@Composable
fun WeekStrip(days: List<WeekDay>, modifier: Modifier = Modifier) {
    val sun = UnitTint.Sun.accent
    if (isAccessibilityFontScale()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { day ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(
                        if (day.isComplete) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                        tint = if (day.isComplete) sun.deep else HWColors.InkTertiary,
                    )
                    Text(day.accessibilityLabel, style = HWType.Body, color = HWColors.Ink, modifier = Modifier.weight(1f))
                }
            }
        }
        return
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        days.forEach { day ->
            Column(
                Modifier.clearAndSetSemantics { contentDescription = day.accessibilityLabel },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(day.label, style = HWType.Micro.copy(fontWeight = FontWeight.Black), color = if (day.isComplete) sun.deep else HWColors.InkTertiary)
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (day.isComplete) sun.base else HWColors.EmptyTrack)
                        .then(if (day.isToday) Modifier.border(2.dp, sun.deep, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (day.isComplete) Icon(Icons.Filled.Check, null, tint = HWColors.BrandInk, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * `.confetti` — 34 deterministic falling pieces. Draws nothing under reduce motion; the badge, the
 * XP tile and the announcement stand in for it.
 */
@Composable
fun Confetti(modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return
    val colours = listOf(
        UnitTint.Sun.accent.base, UnitTint.Mint.accent.base, UnitTint.Coral.accent.base,
        UnitTint.Violet.accent.base, HWColors.Accent, HWColors.AccentSoft,
    )
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (elapsed < 4_000L) {
            elapsed = withFrameMillis { it } - start
        }
    }
    Canvas(modifier.fillMaxSize().clearAndSetSemantics {}) {
        val fall = 560.dp.toPx()
        for (index in 0 until 34) {
            val across = ((index * 37) % 100) / 100f
            val width = (6 + (index * 13) % 7).dp.toPx()
            val duration = 1600 + ((index * 7) % 15) * 100
            val delay = ((index * 11) % 6) * 100
            val t = ((elapsed - delay).toFloat() / duration).coerceIn(0f, 1f)
            if (t >= 1f) continue
            val x = across * size.width
            val y = -20.dp.toPx() + (fall + 20.dp.toPx()) * t
            val height = width * 1.6f
            rotate(720f * t, Offset(x + width / 2, y + height / 2)) {
                drawRoundRect(
                    color = colours[index % colours.size].copy(alpha = 1f - t),
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

// endregion
