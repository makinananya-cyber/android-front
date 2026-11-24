package com.hisaabwise.feature.account.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.components.HWHeroCard
import com.hisaabwise.core.designsystem.components.HWStepBar
import com.hisaabwise.feature.account.Refusal

/** The navy profile card: initials tile, name, email and the server's summary chip. Port of `HWProfileHeader`. */
@Composable
fun AccountProfileHeader(initials: String, name: String, email: String, summary: String, modifier: Modifier = Modifier) {
    HWHeroCard(modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(
                Modifier
                    .heightIn(min = 60.dp)
                    .widthIn(min = 60.dp)
                    .clip(HWRadius.Large)
                    .background(Brush.linearGradient(listOf(HWColors.AccentSoft, HWColors.TintSecondary)))
                    .padding(4.dp)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, style = HWType.Subheading.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.BrandBackground)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, style = HWType.Subheading.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.BrandInk)
                Text(email, style = HWType.Caption, color = HWColors.BrandInkSecondary)
                Text(
                    summary,
                    style = HWType.Micro.copy(fontWeight = FontWeight.Bold),
                    color = HWColors.BrandInkAccent,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(HWRadius.Small)
                        .background(HWColors.BrandSurface)
                        .border(1.dp, HWColors.BrandSeparator, HWRadius.Small)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** The blue tray grouping the settings rows, with its eyebrow caption. Port of `HWSettingsTray`. */
@Composable
fun AccountSettingsTray(caption: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Card)
            .background(Brush.verticalGradient(listOf(HWColors.AccentSoft, HWColors.TintSecondary)))
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            caption.uppercase(),
            style = HWType.Eyebrow,
            color = HWColors.AccentMuted,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp).semantics { heading() },
        )
        content()
    }
}

/**
 * One settings row: icon tile, name, subtitle, trailing value and an optional disclosure chevron.
 * Read as one element by TalkBack. Port of `HWRowLabel` / `HWRow`.
 */
@Composable
fun AccountRow(
    icon: ImageVector,
    name: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    valueDescription: String? = null,
    showsDisclosure: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .shadow(4.dp, HWRadius.Large, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Large)
            .background(HWColors.Surface)
            .then(clickable)
            .semantics(mergeDescendants = true) {
                if (valueDescription != null) stateDescription = valueDescription
            }
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(HWRadius.Small).background(HWColors.Tint),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = HWColors.Accent) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = HWType.BodyLarge.copy(fontWeight = FontWeight.Bold), color = HWColors.Ink)
            if (subtitle != null) Text(subtitle, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
        }
        if (value != null) {
            Text(
                value,
                style = HWType.Body.copy(fontWeight = FontWeight.Bold),
                color = HWColors.Accent,
                textAlign = TextAlign.End,
                modifier = if (valueDescription != null) Modifier.clearAndSetSemantics {} else Modifier,
            )
        }
        if (showsDisclosure) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = HWColors.AccentMuted)
    }
}

/** The white card holding the personal lines. Port of `HWRowCard`. */
@Composable
fun AccountRowCard(caption: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Card)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(6.dp),
    ) {
        if (caption != null) {
            Text(
                caption.uppercase(),
                style = HWType.Eyebrow,
                color = HWColors.AccentMuted,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp).semantics { heading() },
            )
        }
        content()
    }
}

/** A label on the leading side and a value (or inline field) on the trailing side. Port of `HWInfoRow`. */
@Composable
fun AccountInfoRow(label: String, isSeparated: Boolean = true, value: @Composable () -> Unit) {
    Column {
        if (isSeparated) HorizontalDivider(color = HWColors.Separator)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(label, style = HWType.Caption.copy(fontWeight = FontWeight.Bold), color = HWColors.InkSecondary)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { value() }
        }
    }
}

/** A read-only value; [isLocked] draws the padlock beside the email. Port of `HWInfoValue`. */
@Composable
fun AccountInfoValue(text: String, isLocked: Boolean = false, unit: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text,
            style = HWType.Body.copy(fontWeight = FontWeight.Bold),
            color = if (isLocked) HWColors.InkSecondary else HWColors.Ink,
            textAlign = TextAlign.End,
        )
        if (unit != null) Text(unit, style = HWType.Micro.copy(fontWeight = FontWeight.Bold), color = HWColors.InkTertiary)
        if (isLocked) Icon(Icons.Outlined.Lock, null, tint = HWColors.InkTertiary, modifier = Modifier.size(16.dp))
    }
}

/** An underlined inline field used in the personal card's edit mode. Port of `HWInlineField`. */
@Composable
fun AccountInlineField(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    symbol: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isInvalid: Boolean = false,
    invalidMessage: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val underline = when {
        isInvalid -> HWColors.Danger
        focused -> HWColors.AccentMuted
        else -> HWColors.SeparatorStrong
    }
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        singleLine = true,
        textStyle = HWType.Body.copy(fontWeight = FontWeight.Bold, color = HWColors.Ink),
        cursorBrush = SolidColor(HWColors.Accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        modifier = modifier
            .widthIn(max = 190.dp)
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentDescription = label
                if (isInvalid && invalidMessage != null) error(invalidMessage)
            },
        decorationBox = { inner ->
            Row(
                Modifier
                    .heightIn(min = MinTouchTarget)
                    .drawBehind {
                        val y = size.height - 0.75.dp.toPx()
                        drawLine(underline, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx())
                    }
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (symbol != null) {
                    Text(symbol, style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Accent, modifier = Modifier.clearAndSetSemantics {})
                }
                Box(Modifier.weight(1f, fill = false).widthIn(min = 60.dp)) { inner() }
            }
        },
    )
}

/** Small tertiary note under a row (the locked-email explanation, the support pointer). */
@Composable
fun AccountInfoNote(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        style = HWType.Micro.copy(fontWeight = FontWeight.Normal),
        color = HWColors.InkTertiary,
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
    )
}

/** A field's error, in the card under the row it is about. */
@Composable
fun AccountFieldNote(message: String) {
    Text(
        message,
        style = HWType.Micro.copy(fontWeight = FontWeight.Normal),
        color = HWColors.Danger,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
    )
}

/** The dial-code chip beside the phone digits. Port of `HWDialTrigger`. */
@Composable
fun AccountDialTrigger(countryCode: String, dialCode: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Small)
            .background(HWColors.Tint)
            .border(1.dp, HWColors.Separator, HWRadius.Small)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = "$countryCode $dialCode".trim()
            }
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val style = HWType.Caption.copy(fontWeight = FontWeight.ExtraBold)
        Text(countryCode, style = style, color = HWColors.Accent)
        Text(dialCode, style = style, color = HWColors.Accent)
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = HWColors.Accent, modifier = Modifier.size(16.dp))
    }
}

/** The refusal beside a control that could not do what it was asked, or nothing. */
@Composable
fun AccountRefusalNote(reason: Refusal.Reason?, modifier: Modifier = Modifier) {
    if (reason == null) return
    val text = stringResource(
        when (reason) {
            Refusal.Reason.NeedsConnection -> R.string.account_refusal_needsConnection
            Refusal.Reason.Refused -> R.string.account_refusal_refused
        },
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Medium)
            .background(HWColors.DangerSoft)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (reason == Refusal.Reason.NeedsConnection) Icons.Outlined.WifiOff else Icons.Outlined.ErrorOutline,
            null,
            tint = HWColors.Danger,
        )
        Text(text, style = HWType.Caption, color = HWColors.Ink, modifier = Modifier.weight(1f))
    }
}

/** The step bar with its "Step 2 of 3" label and a back-a-step control from step two on. */
@Composable
fun AccountStepHeader(stepIndex: Int, stepCount: Int, label: String, backLabel: String, onBack: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Row(
                    Modifier
                        .heightIn(min = MinTouchTarget)
                        .clip(HWRadius.Small)
                        .clickable(role = Role.Button, onClick = onBack)
                        .semantics(mergeDescendants = true) { contentDescription = backLabel }
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = HWColors.Accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(backLabel, style = HWType.Caption, color = HWColors.Accent)
                }
                Spacer(Modifier.weight(1f))
            }
            Text(label, style = HWType.Caption, color = HWColors.InkSecondary)
        }
        HWStepBar(stepIndex, stepCount)
    }
}
