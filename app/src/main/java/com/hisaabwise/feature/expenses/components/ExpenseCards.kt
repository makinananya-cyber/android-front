package com.hisaabwise.feature.expenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.components.HWBudgetBar
import com.hisaabwise.core.designsystem.components.HWHeroCard
import com.hisaabwise.core.designsystem.components.HWStatTile

/** `.summary` — the navy card: caption, the big total, three split tiles, then [footer]. Port of `HWSpendSummary`. */
@Composable
fun SpendSummaryCard(
    caption: String,
    total: String,
    splits: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    HWHeroCard(modifier) {
        Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.BrandInkSecondary, modifier = Modifier.semantics { heading() })
        Text(
            total,
            style = HWType.Display,
            color = HWColors.BrandInk,
            modifier = Modifier
                .padding(top = 6.dp)
                .clearAndSetSemantics { contentDescription = caption; stateDescription = total },
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            splits.forEach { (label, value) ->
                HWStatTile(value = value, label = label, onHero = true, modifier = Modifier.weight(1f))
            }
        }
        footer()
    }
}

/**
 * `.budget` — caption and "₹1,150 of ₹19,770" over the bar and its percentage, under a rule.
 * One accessibility element carrying the whole fact (and the over state in words).
 */
@Composable
fun WantsBudgetBlock(
    caption: String,
    amount: String,
    percentageLabel: String,
    fill: Double,
    isOver: Boolean,
    accessibilityDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clearAndSetSemantics { contentDescription = caption; stateDescription = accessibilityDescription },
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(HWColors.BrandSeparator))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(caption.uppercase(), style = HWType.Eyebrow, color = HWColors.BrandInkSecondary, modifier = Modifier.weight(1f))
            Text(
                amount,
                style = HWType.Caption,
                color = if (isOver) HWColors.BrandDanger else HWColors.BrandInkSecondary,
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HWBudgetBar(fill = fill, isOver = isOver, onHero = true, height = 14.dp, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(11.dp))
            Text(
                percentageLabel,
                style = HWType.Caption.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isOver) HWColors.BrandDanger else HWColors.BrandInkAccent,
            )
        }
    }
}

/** The rounded icon tile on rows and the hero. Incoming money is drawn inverted. */
@Composable
fun CategoryTile(icon: ImageVector, isIncoming: Boolean, size: Int = 40, iconSize: Int = 22, large: Boolean = false) {
    Box(
        Modifier
            .size(size.dp)
            .clip(if (large) HWRadius.Large else HWRadius.Medium)
            .background(if (isIncoming) HWColors.AccentDeep else HWColors.Tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (isIncoming) HWColors.AccentSoft else HWColors.Accent, modifier = Modifier.size(iconSize.dp))
    }
}

/** `.cat` — one category row. Read as name, with the total as its value and the hint as the action. */
@Composable
fun CategoryRow(
    name: String,
    hint: String,
    total: String,
    icon: ImageVector,
    isIncoming: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .shadow(4.dp, HWRadius.Large, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Large)
            .background(HWColors.Surface)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = total
                role = Role.Button
                onClick(label = hint) { onClick(); true }
            }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTile(icon, isIncoming)
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = HWType.BodyLarge.copy(fontWeight = FontWeight.Bold), color = HWColors.Ink)
            Spacer(Modifier.height(2.dp))
            Text(hint, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
        }
        Spacer(Modifier.size(8.dp))
        Text(
            total,
            style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = if (isIncoming) HWColors.Accent else HWColors.Ink,
            softWrap = false,
            maxLines = 1,
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = HWColors.AccentMuted, modifier = Modifier.size(20.dp))
    }
}

/** `.bubble` — the translucent sky→venus panel the category rows sit in. */
@Composable
fun CategoryPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Card)
            .background(Brush.verticalGradient(listOf(HWColors.Tint.copy(alpha = 0.62f), HWColors.TintSecondary.copy(alpha = 0.34f))))
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** The category page's header card: tile, name and hint, total and month. One heading element. */
@Composable
fun CategoryHero(
    name: String,
    hint: String,
    total: String,
    monthLabel: String,
    icon: ImageVector,
    isIncoming: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.10f), spotColor = HWColors.Ink.copy(alpha = 0.10f))
            .clip(HWRadius.Card)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .clearAndSetSemantics {
                contentDescription = "$name, $hint"
                stateDescription = "$total, $monthLabel"
                heading()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTile(icon, isIncoming, size = 52, iconSize = 28, large = true)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = HWType.Subheading, color = HWColors.Ink)
            Spacer(Modifier.height(3.dp))
            Text(hint, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(total, style = HWType.Subheading, color = if (isIncoming) HWColors.Accent else HWColors.Ink, softWrap = false, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(monthLabel.uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted)
        }
    }
}
