package com.hisaabwise.feature.reports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.components.BarSegment
import com.hisaabwise.core.designsystem.components.HWHeroCard
import com.hisaabwise.core.designsystem.components.HWSplitBar
import com.hisaabwise.feature.reports.GroupIcon

/** iOS `ExpensesView.symbol(_:)` in Material icons. */
fun GroupIcon.vector(): ImageVector = when (this) {
    GroupIcon.Groceries -> Icons.Outlined.ShoppingBasket
    GroupIcon.Transport -> Icons.Outlined.DirectionsBus
    GroupIcon.Entertainment -> Icons.Outlined.Movie
    GroupIcon.Other -> Icons.AutoMirrored.Outlined.HelpOutline
    GroupIcon.Income -> Icons.Outlined.VerticalAlignBottom
    GroupIcon.Utilities, GroupIcon.Bolt -> Icons.Outlined.Bolt
    GroupIcon.Rent -> Icons.Outlined.Home
    GroupIcon.Drop -> Icons.Outlined.WaterDrop
    GroupIcon.Signal -> Icons.Outlined.PhoneIphone
    GroupIcon.Tag -> Icons.Outlined.LocalOffer
}

/** `.card-top`: the eyebrow caption and an optional subtitle beside it, read as one heading. */
@Composable
fun CardTop(caption: String, sub: String?, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.Top,
    ) {
        Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted, modifier = Modifier.weight(1f))
        if (sub != null) {
            Spacer(Modifier.width(10.dp))
            Text(sub, style = HWType.Caption, color = HWColors.InkSecondary, textAlign = TextAlign.End)
        }
    }
}

/** The navy totals card: caption, the big total, three figure chips, then [footer]. */
@Composable
fun SpendSummary(
    caption: String,
    total: String,
    splits: List<FigureSplit>,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    HWHeroCard(modifier) {
        Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.BrandInkSecondary, modifier = Modifier.clearAndSetSemantics {})
        Spacer(Modifier.height(6.dp))
        Text(
            total,
            style = HWType.Display,
            color = HWColors.BrandInk,
            modifier = Modifier.clearAndSetSemantics { contentDescription = caption; stateDescription = total },
        )
        Spacer(Modifier.height(16.dp))
        FigureChips(splits)
        footer()
    }
}

/** The ⓘ sentence under the totals that explains the month's split. */
@Composable
fun SummaryNote(note: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 15.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(HWColors.BrandSeparator))
        Row(Modifier.fillMaxWidth().padding(top = 13.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, null, tint = HWColors.BrandInkAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Text(note, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.BrandInkSecondary, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * The wants allowance: used amount "of X allowed", a 9 dp track (red when over) and the percentage.
 * TalkBack reads [accessibilityDescription], which says the over state in words.
 */
@Composable
fun AllowanceBar(
    amount: String,
    allowance: String,
    percentageLabel: String,
    fill: Double,
    isOver: Boolean,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val animated by animateFloatAsState(
        fill.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = if (reduceMotion) tween(0) else tween(HWMotion.LONG, easing = HWMotion.EaseOut),
        label = "allowance",
    )
    Column(
        modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = amount; stateDescription = accessibilityDescription },
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(amount, style = HWType.Heading, color = HWColors.Ink)
            Spacer(Modifier.width(7.dp))
            Text(allowance, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary, modifier = Modifier.padding(bottom = 3.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isAccessibilityFontSize()) {
                Box(Modifier.weight(1f).height(9.dp).clip(HWRadius.Pill).background(HWColors.BackgroundSecondary)) {
                    val stops = if (isOver) listOf(HWColors.Danger, HWColors.MeterNothing) else listOf(HWColors.Accent, HWColors.AccentMuted)
                    Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(HWRadius.Pill).background(Brush.horizontalGradient(stops)))
                }
                Spacer(Modifier.width(11.dp))
            }
            Text(
                percentageLabel,
                style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isOver) HWColors.Danger else HWColors.Accent,
            )
        }
    }
}

/** One part of the month's income split. */
data class SplitRow(val name: String, val amount: String, val target: String?, val share: Double, val color: Color)

/** The four-segment split bar (needs · wants · saved · above goal) and its key. The bar hides at accessibility sizes. */
@Composable
fun PortionSplit(rows: List<SplitRow>, accessibilityLabel: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isAccessibilityFontSize()) {
            HWSplitBar(
                rows.map { BarSegment(it.share, it.color) },
                Modifier.clip(HWRadius.Small).clearAndSetSemantics {},
                height = 32.dp,
                gap = 2.dp,
            )
        }
        Column(
            Modifier.fillMaxWidth().semantics { contentDescription = accessibilityLabel },
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().clearAndSetSemantics {
                        contentDescription = row.name
                        stateDescription = listOfNotNull(row.amount, row.target).joinToString(", ")
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(9.dp).clip(HWRadius.Hairline).background(row.color))
                    Spacer(Modifier.width(9.dp))
                    Text(row.name, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold), color = HWColors.Ink, modifier = Modifier.weight(1f))
                    Text(row.amount, style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Ink)
                    if (row.target != null) {
                        Spacer(Modifier.width(9.dp))
                        Text(row.target, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary, textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

/** One accordion row's entry. */
data class AccordionEntry(val label: String, val dateLabel: String, val amount: String)

/**
 * One category of everything logged (iOS `HWAccordionRow`): tile, name, summary, total, chevron; the panel
 * of entries opens beneath. The screen keeps only one open at a time.
 */
@Composable
fun AccordionRow(
    name: String,
    summaryLabel: String,
    amount: String,
    icon: ImageVector,
    slot: Int,
    isIncoming: Boolean,
    isExpanded: Boolean,
    entries: List<AccordionEntry>,
    emptyNote: String,
    hint: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val slotColour = HWColors.categorySlot(slot.coerceAtLeast(1))
    Column(
        modifier
            .fillMaxWidth()
            .shadow(3.dp, HWRadius.Large, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Large)
            .background(HWColors.Surface)
            .border(1.dp, if (isExpanded) HWColors.SeparatorStrong else HWColors.Separator, HWRadius.Large),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClickLabel = hint, role = Role.Button, onClick = onToggle)
                .clearAndSetSemantics {
                    contentDescription = name
                    stateDescription = amount
                    role = Role.Button
                    onClick(label = hint) { onToggle(); true }
                }
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(HWRadius.Medium).background(if (isIncoming) HWColors.AccentDeep else slotColour.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = if (isIncoming) HWColors.AccentSoft else slotColour, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = HWType.BodyLarge.copy(fontWeight = FontWeight.Bold), color = HWColors.Ink)
                Text(summaryLabel, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                amount,
                style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isIncoming) HWColors.Accent else HWColors.Ink,
                textAlign = TextAlign.End,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = if (isExpanded) HWColors.Accent else HWColors.InkTertiary,
                modifier = Modifier.size(26.dp),
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = if (reduceMotion) fadeIn(tween(0)) else expandVertically(tween(HWMotion.SHORT, easing = HWMotion.EaseOut)) + fadeIn(),
            exit = if (reduceMotion) fadeOut(tween(0)) else shrinkVertically(tween(HWMotion.SHORT, easing = HWMotion.EaseOut)) + fadeOut(),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, top = 4.dp, bottom = 13.dp)) {
                if (entries.isEmpty()) {
                    EmptyNote(emptyNote, Modifier.padding(top = 4.dp))
                } else {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(HWColors.Separator))
                        EntryRow(entry, icon, isIncoming)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: AccordionEntry, icon: ImageVector, isIncoming: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .clearAndSetSemantics {
                contentDescription = entry.label
                stateDescription = "${entry.amount}, ${entry.dateLabel}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(HWRadius.Small).background(if (isIncoming) HWColors.AccentDeep else HWColors.Tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (isIncoming) HWColors.AccentSoft else HWColors.Accent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.label, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold), color = HWColors.Ink)
            Text(entry.dateLabel, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
        }
        Spacer(Modifier.width(11.dp))
        Text(
            entry.amount,
            style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold),
            color = if (isIncoming) HWColors.Accent else HWColors.Ink,
            textAlign = TextAlign.End,
        )
    }
}

/** One "for the record" fact. */
data class FactItem(val id: String, val label: String, val value: String, val note: String?)

/** Two-column facts grid; one column at accessibility sizes. */
@Composable
fun FactGrid(facts: List<FactItem>, modifier: Modifier = Modifier) {
    val columns = if (isAccessibilityFontSize()) 1 else 2
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        facts.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { Fact(it, Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Fact(item: FactItem, modifier: Modifier) {
    Column(
        modifier
            .clip(HWRadius.Large)
            .background(HWColors.BackgroundSecondary)
            .border(1.dp, HWColors.Separator, HWRadius.Large)
            .padding(start = 13.dp, end = 13.dp, top = 13.dp, bottom = 12.dp)
            .clearAndSetSemantics {
                contentDescription = item.label
                stateDescription = listOfNotNull(item.value, item.note).joinToString(", ")
            },
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(item.label.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted, modifier = Modifier.padding(bottom = 2.dp))
        Text(item.value, style = HWType.Subheading, color = HWColors.Ink)
        if (item.note != null) Text(item.note, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
    }
}
