package com.hisaabwise.feature.expenses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.MinTouchTarget

/** `.entry` — one logged entry with its server date label and a quiet remove control. */
@Composable
fun EntryRow(
    label: String,
    dateLabel: String,
    amount: String,
    icon: ImageVector,
    isIncoming: Boolean,
    removeLabel: String,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .shadow(4.dp, HWRadius.Large, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Large)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Large)
            .padding(start = 13.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryTile(icon, isIncoming, size = 36, iconSize = 20)
        Spacer(Modifier.size(12.dp))
        Row(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = "$label, $dateLabel"; stateDescription = amount },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold), color = HWColors.Ink)
                Spacer(Modifier.height(2.dp))
                Text(dateLabel, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
            }
            Spacer(Modifier.size(8.dp))
            Text(
                amount,
                style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isIncoming) HWColors.Accent else HWColors.Ink,
                textAlign = TextAlign.End,
            )
        }
        // Unavailable rather than absent while a write is in flight, so the row never moves under a finger.
        IconButton(onClick = { onRemove?.invoke() }, enabled = onRemove != null, modifier = Modifier.size(MinTouchTarget)) {
            Icon(Icons.Filled.Close, removeLabel, tint = HWColors.InkTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

/** A dashed box marking a space where rows could be ("Nothing logged yet this month"). */
@Composable
fun EmptyNote(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        style = HWType.Body,
        color = HWColors.InkTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder()
            .padding(horizontal = 18.dp, vertical = 26.dp),
    )
}

private fun Modifier.dashedBorder(): Modifier = drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = HWColors.SeparatorStrong,
        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(18.dp.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
    )
}

/** A dashed full-width button ("Add another bill"). */
@Composable
fun DashedButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(HWRadius.Large)
            .dashedBorder()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = HWColors.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, style = HWType.BodyLarge.copy(fontWeight = FontWeight.Bold), color = HWColors.Accent)
    }
}

@Composable
private fun BillGlyph(icon: ImageVector) {
    Box(Modifier.size(34.dp).clip(HWRadius.Small).background(HWColors.Tint), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = HWColors.Accent, modifier = Modifier.size(18.dp))
    }
}

/** `.line` — a bill at rest. */
@Composable
fun BillLine(name: String, amount: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Medium)
            .background(HWColors.BackgroundSecondary)
            .clearAndSetSemantics { contentDescription = name; stateDescription = amount }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BillGlyph(icon)
        Spacer(Modifier.size(9.dp))
        Text(name, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold), color = HWColors.Ink, modifier = Modifier.weight(1f))
        Text(amount, style = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Ink, textAlign = TextAlign.End)
    }
}

/** `.editing .line` — a bill being edited: name, symbol, amount, and a destructive remove. */
@Composable
fun BillLineEditor(
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    symbol: String,
    icon: ImageVector,
    nameLabel: String,
    amountLabel: String,
    removeLabel: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nameFocused by remember { mutableStateOf(false) }
    var amountFocused by remember { mutableStateOf(false) }
    val focused = nameFocused || amountFocused
    val amountValue = if (amount.isEmpty()) stringResource(R.string.component_money_accessibilityEmpty, symbol)
    else stringResource(R.string.component_money_accessibilityValue, amount, symbol)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Medium)
            .background(HWColors.Surface)
            .border(1.5.dp, if (focused) HWColors.AccentMuted else HWColors.SeparatorStrong, HWRadius.Medium)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BillGlyph(icon)
        Spacer(Modifier.size(9.dp))
        Box(Modifier.weight(1f)) {
            if (name.isEmpty()) Text(nameLabel, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold), color = HWColors.InkTertiary)
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = HWType.Body.copy(fontWeight = FontWeight.SemiBold, color = HWColors.Ink),
                cursorBrush = SolidColor(HWColors.Accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, autoCorrectEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { nameFocused = it.isFocused }
                    .semantics { contentDescription = nameLabel },
            )
        }
        Spacer(Modifier.size(6.dp))
        Text(symbol, style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Accent, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.size(4.dp))
        BasicTextField(
            value = amount,
            onValueChange = { new -> onAmountChange(new.filter { it.isDigit() || it == '.' || it == ',' }) },
            singleLine = true,
            textStyle = HWType.BodyLarge.copy(fontWeight = FontWeight.ExtraBold, color = HWColors.Ink, textAlign = TextAlign.End),
            cursorBrush = SolidColor(HWColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .widthIn(min = 74.dp, max = 120.dp)
                .onFocusChanged { amountFocused = it.isFocused }
                .semantics { contentDescription = amountLabel; stateDescription = amountValue },
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(MinTouchTarget)) {
            Box(Modifier.size(30.dp).clip(HWRadius.Small).background(HWColors.Danger.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, removeLabel, tint = HWColors.Danger, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** `.fixed-val` — the fixed amount at rest, with its padlock. */
@Composable
fun FixedAmount(amount: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Large)
            .background(Brush.linearGradient(listOf(HWColors.AccentSoft, HWColors.Tint)))
            .border(1.dp, HWColors.Separator, HWRadius.Large)
            .clearAndSetSemantics { contentDescription = label; stateDescription = amount }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(amount, style = HWType.Heading, color = HWColors.Ink, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Lock, null, tint = HWColors.Accent.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
    }
}

/** `.editbtn` / `.editbtn.on` — Edit turns edit mode on, Done commits. */
@Composable
fun EditToggle(isOn: Boolean, isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val text = stringResource(if (isOn) R.string.expenses_done else R.string.expenses_edit)
    Row(
        modifier
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Pill)
            .background(if (isOn) HWColors.AccentDeep else HWColors.Surface)
            .border(1.dp, if (isOn) HWColors.AccentDeep else HWColors.SeparatorStrong, HWRadius.Pill)
            .clickable(enabled = !isLoading, role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (isOn) HWColors.Surface else HWColors.AccentDeep
        if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = tint, strokeWidth = 2.dp)
        else Icon(if (isOn) Icons.Filled.Check else Icons.Outlined.Edit, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(text, style = HWType.Caption.copy(fontWeight = FontWeight.Bold), color = tint)
    }
}
