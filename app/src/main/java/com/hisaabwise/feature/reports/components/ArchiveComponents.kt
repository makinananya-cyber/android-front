package com.hisaabwise.feature.reports.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.VerdictTint
import com.hisaabwise.core.designsystem.components.BarSegment
import com.hisaabwise.core.designsystem.components.HWHeroCard
import com.hisaabwise.core.designsystem.components.HWSplitBar
import com.hisaabwise.core.designsystem.components.HWStatTile
import com.hisaabwise.core.designsystem.components.HWVerdictBadge

/** iOS `isAccessibilitySize`: at these font sizes charts give way to text alternatives. */
@Composable
fun isAccessibilityFontSize(): Boolean = LocalDensity.current.fontScale >= 1.5f

/** A caption + value chip on a hero card (iOS `HWFigureChips`): a row, or a column at accessibility sizes. */
data class FigureSplit(val label: String, val value: String)

@Composable
fun FigureChips(splits: List<FigureSplit>, modifier: Modifier = Modifier) {
    if (isAccessibilityFontSize()) {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            splits.forEach { FigureChip(it, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            splits.forEach { FigureChip(it, Modifier.weight(1f).fillMaxHeight()) }
        }
    }
}

@Composable
private fun FigureChip(split: FigureSplit, modifier: Modifier) {
    HWStatTile(
        value = split.value,
        label = split.label,
        onHero = true,
        modifier = modifier
            .border(1.dp, HWColors.BrandSeparator, HWRadius.Large)
            .clearAndSetSemantics { contentDescription = split.label; stateDescription = split.value },
    )
}

/** The navy hero over the archive: caption, subtitle, the trend chart, and three figure chips. */
@Composable
fun ArchiveHero(
    caption: String,
    subtitle: String,
    splits: List<FigureSplit>,
    modifier: Modifier = Modifier,
    chart: @Composable () -> Unit,
) {
    HWHeroCard(modifier) {
        Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.BrandInkSecondary, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.BrandInkSecondary)
        Spacer(Modifier.height(16.dp))
        chart()
        Spacer(Modifier.height(15.dp))
        FigureChips(splits)
    }
}

/** One trend bar as the chart draws it. [height] is 0…1 of the plot. */
data class TrendBar(
    val id: String,
    val label: String,
    val height: Double,
    val verdict: VerdictTint,
    val percentageLabel: String,
    val accessibilityLabel: String,
)

/**
 * The savings-goal trend: one bar per closed month coloured by its verdict, against a dashed goal line
 * (iOS `HWTrendChart`, drawn with Canvas). At accessibility sizes it becomes a list of month + verdict badge.
 */
@Composable
fun TrendChart(bars: List<TrendBar>, goalHeight: Double, accessibilityLabel: String, modifier: Modifier = Modifier) {
    if (isAccessibilityFontSize()) {
        Column(
            modifier.fillMaxWidth().semantics { contentDescription = accessibilityLabel },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            bars.forEach { bar ->
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = bar.accessibilityLabel },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(bar.label, style = HWType.Body.copy(fontWeight = FontWeight.Bold), color = HWColors.BrandInk, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                    HWVerdictBadge(bar.percentageLabel, bar.verdict)
                }
            }
        }
        return
    }
    Column(modifier.fillMaxWidth().semantics { contentDescription = accessibilityLabel }) {
        Box(Modifier.fillMaxWidth().height(74.dp)) {
            Canvas(Modifier.matchParentSize()) {
                if (bars.isNotEmpty()) {
                    val slot = size.width / bars.size
                    val barWidth = slot * 0.58f
                    val radius = 4.dp.toPx()
                    bars.forEachIndexed { i, bar ->
                        val h = (bar.height.coerceIn(0.0, 1.0) * size.height).toFloat()
                        if (h <= 0f) return@forEachIndexed
                        val left = slot * i + (slot - barWidth) / 2
                        drawRoundRect(
                            color = bar.verdict.bar,
                            topLeft = Offset(left, size.height - h),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(minOf(radius, h / 2), minOf(radius, h / 2)),
                        )
                    }
                }
                val y = size.height - (goalHeight.coerceIn(0.0, 1.0) * size.height).toFloat()
                drawLine(
                    color = HWColors.BrandInk.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                )
            }
            // One TalkBack stop per bar over the drawing.
            Row(Modifier.matchParentSize()) {
                bars.forEach { bar ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clearAndSetSemantics {
                            contentDescription = bar.accessibilityLabel
                            stateDescription = bar.percentageLabel
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            bars.forEach { bar ->
                Text(
                    bar.label.uppercase(),
                    style = HWType.Eyebrow,
                    color = HWColors.BrandInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** "2026 ———————— ₹76,700 saved" — stacked at accessibility sizes. */
@Composable
fun YearHeader(year: String, saved: String, modifier: Modifier = Modifier) {
    val merged = modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }
    if (isAccessibilityFontSize()) {
        Column(merged, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(year.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted)
            Text(saved, style = HWType.Caption, color = HWColors.InkTertiary)
        }
    } else {
        Row(merged, verticalAlignment = Alignment.CenterVertically) {
            Text(year.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).height(1.dp).background(HWColors.Separator))
            Spacer(Modifier.width(10.dp))
            Text(saved, style = HWType.Caption, color = HWColors.InkTertiary)
        }
    }
}

/**
 * One closed month in the archive: name + year, what it cost, the verdict badge, a chevron, and the
 * category proportion bar. Read by TalkBack as one control that opens the month.
 */
@Composable
fun MonthRow(
    name: String,
    year: String,
    spent: String,
    verdict: VerdictTint,
    percentageLabel: String,
    segments: List<BarSegment>,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Card)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .clickable(role = Role.Button, onClickLabel = hint, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 13.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        if (isAccessibilityFontSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthWhen(name, year, spent, Modifier.weight(1f))
                Spacer(Modifier.width(11.dp))
                Chevron()
            }
            HWVerdictBadge(percentageLabel, verdict)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthWhen(name, year, spent, Modifier.weight(1f))
                Spacer(Modifier.width(11.dp))
                HWVerdictBadge(percentageLabel, verdict)
                Spacer(Modifier.width(11.dp))
                Chevron()
            }
        }
        if (segments.isNotEmpty()) ProportionBar(segments)
    }
}

@Composable
private fun MonthWhen(name: String, year: String, spent: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(name, style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Ink)
            Spacer(Modifier.width(5.dp))
            Text(year, style = HWType.Caption, color = HWColors.InkTertiary)
        }
        Text(spent, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkSecondary)
    }
}

@Composable
private fun Chevron() {
    Box(
        Modifier.size(26.dp).clip(HWRadius.Hairline).background(HWColors.BackgroundSecondary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = HWColors.Accent, modifier = Modifier.size(18.dp))
    }
}

/** The thin category proportion bar under a month row (decorative; hidden from TalkBack). */
@Composable
fun ProportionBar(segments: List<BarSegment>, modifier: Modifier = Modifier) {
    HWSplitBar(segments, modifier.clearAndSetSemantics {}, height = 7.dp, gap = 2.dp)
}

/** Category slot → bar segment, the archive row's mapping. */
fun proportionSegments(slots: List<Pair<Int, Double>>): List<BarSegment> =
    slots.map { (slot, share) -> BarSegment(share, HWColors.categorySlot(slot.coerceAtLeast(1))) }

/** The dashed empty-state note (iOS `HWEmptyNote`). */
@Composable
fun EmptyNote(message: String, modifier: Modifier = Modifier) {
    val dash = LocalDensity.current.run { floatArrayOf(6.dp.toPx(), 4.dp.toPx()) }
    Text(
        message,
        style = HWType.Body,
        color = HWColors.InkTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = HWColors.SeparatorStrong,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(dash)),
                )
            }
            .padding(horizontal = 18.dp, vertical = 26.dp),
    )
}

