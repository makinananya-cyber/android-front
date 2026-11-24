package com.hisaabwise.core.designsystem.components

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.model.PasswordStrength

@Composable
private fun fieldColors(): FieldColors {
    val brand = LocalAppearance.current == Appearance.Brand
    return if (brand) FieldColors(HWColors.BrandSurface, HWColors.BrandSeparatorStrong, HWColors.BrandInkAccent, HWColors.BrandInk, HWColors.BrandInkSecondary, HWColors.BrandInkSecondary, HWColors.BrandDanger)
    else FieldColors(HWColors.Surface, HWColors.SeparatorStrong, HWColors.Accent, HWColors.Ink, HWColors.InkTertiary, HWColors.InkSecondary, HWColors.Danger)
}

internal data class FieldColors(
    val container: Color, val border: Color, val focused: Color, val text: Color,
    val placeholder: Color, val label: Color, val error: Color,
)

/** Label above the field, error below it. */
@Composable
fun HWFieldFrame(
    label: String?,
    error: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = fieldColors()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, style = HWType.Caption, color = colors.label)
        content()
        if (error != null) Text(error, style = HWType.Caption, color = colors.error, modifier = Modifier.semantics { this.error(error) })
    }
}

/**
 * The text field. [isSecure] adds a reveal toggle. Errors are shown inline under the field and
 * announced to TalkBack.
 */
@Composable
fun HWTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    error: String? = null,
    isSecure: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    textStyle: TextStyle = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal),
) {
    val colors = fieldColors()
    var focused by remember { mutableStateOf(false) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val borderColor = when {
        error != null -> colors.error
        focused -> colors.focused
        else -> colors.border
    }
    HWFieldFrame(label, error, modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = if (enabled) colors.text else colors.placeholder),
            cursorBrush = SolidColor(colors.focused),
            visualTransformation = if (isSecure && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isSecure) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
                autoCorrectEnabled = !isSecure && keyboardType == KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics { if (label != null) contentDescription = label },
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(HWRadius.Medium)
                        .background(colors.container)
                        .border(if (focused) 2.dp else 1.dp, borderColor, HWRadius.Medium)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) Text(placeholder, style = textStyle, color = colors.placeholder)
                        inner()
                    }
                    if (isSecure) {
                        IconButton(onClick = { revealed = !revealed }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = stringResource(R.string.component_field_revealPassword),
                                tint = colors.label,
                            )
                        }
                    }
                    trailing?.invoke()
                }
            },
        )
    }
}

/**
 * An amount field: the currency code beside a decimal keypad field. The typed text is kept as
 * typed; the view model turns it into minor units with [com.hisaabwise.core.model.TypedAmount].
 * [prominent] draws the large figure used in the Edit-goal sheet.
 */
@Composable
fun HWMoneyField(
    text: String,
    onTextChange: (String) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "0",
    error: String? = null,
    prominent: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
) {
    val colors = fieldColors()
    val a11yValue = if (text.isEmpty()) stringResource(R.string.component_money_accessibilityEmpty, currencyCode)
    else stringResource(R.string.component_money_accessibilityValue, text, currencyCode)
    HWTextField(
        value = text,
        onValueChange = { new -> onTextChange(new.filter { it.isDigit() || it == '.' || it == ',' }) },
        modifier = modifier.semantics { stateDescription = a11yValue },
        label = label,
        placeholder = placeholder,
        error = error,
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction,
        onImeAction = onImeAction,
        textStyle = if (prominent) HWType.Title else HWType.Subheading,
        leading = {
            Text(currencyCode, style = if (prominent) HWType.Heading else HWType.BodyLarge, color = colors.label)
        },
    )
}

/** A field-shaped button that opens a picker (currency, country, security question…). */
@Composable
fun HWComboField(
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    error: String? = null,
) {
    val colors = fieldColors()
    val hint = stringResource(R.string.component_combo_hint)
    HWFieldFrame(label, error, modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(HWRadius.Medium)
                .background(colors.container)
                .border(1.dp, if (error != null) colors.error else colors.border, HWRadius.Medium)
                .clickable(role = Role.Button, onClickLabel = hint, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value ?: placeholder,
                style = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal),
                color = if (value == null) colors.placeholder else colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = colors.label)
        }
    }
}

/** Checkbox with a label slot (Terms consent, Keep me signed in). */
@Composable
fun HWCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val brand = LocalAppearance.current == Appearance.Brand
    val tick = if (brand) HWColors.BrandInk else HWColors.Accent
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .semantics { stateDescription = if (checked) "✓" else "" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(HWRadius.Small)
                .background(if (checked) tick else Color.Transparent)
                .border(1.5.dp, tick, HWRadius.Small),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, null, tint = if (brand) HWColors.BrandBackground else HWColors.Surface, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        label()
    }
}

/** "Step 2 of 3" bar for the registration and password wizards. */
@Composable
fun HWStepBar(step: Int, count: Int, modifier: Modifier = Modifier) {
    val brand = LocalAppearance.current == Appearance.Brand
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 5.dp)
                    .clip(HWRadius.Pill)
                    .background(
                        when {
                            i <= step -> if (brand) HWColors.BrandInkAccent else HWColors.Accent
                            else -> if (brand) HWColors.BrandSeparator else HWColors.EmptyTrack
                        },
                    ),
            )
        }
    }
}

/** Four-segment password strength meter (advisory only). */
@Composable
fun HWStrengthMeter(strength: PasswordStrength, label: String?, modifier: Modifier = Modifier) {
    val brand = LocalAppearance.current == Appearance.Brand
    val colour = when (strength) {
        PasswordStrength.None -> HWColors.EmptyTrack
        PasswordStrength.Weak -> HWColors.MeterNothing
        PasswordStrength.Fair -> HWColors.MeterLow
        PasswordStrength.Good -> HWColors.MeterHigh
        PasswordStrength.Strong -> HWColors.MeterReached
    }
    val name = stringResource(R.string.component_strength_label)
    val none = stringResource(R.string.component_strength_none)
    Column(modifier.semantics { contentDescription = name; stateDescription = label ?: none }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).heightIn(min = 4.dp).clip(HWRadius.Pill)
                        .background(if (i < strength.score) colour else if (brand) HWColors.BrandSeparator else HWColors.EmptyTrack),
                )
            }
        }
        if (label != null) Text(label, style = HWType.Micro.copy(fontSize = 12.sp), color = if (brand) HWColors.BrandInkSecondary else HWColors.InkSecondary)
    }
}
