package com.hisaabwise.feature.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.components.HWComboField
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.core.model.PhoneNumber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Port of iOS `HWPhoneField`: a dial-code trigger (code chip, dial code, chevron) beside a digits
 * box. Every edit is funnelled through [PhoneNumber.nationalDigits], so a paste or a hardware
 * keyboard cannot put letters in the box and non-ASCII digits are converted.
 */
@Composable
fun AuthPhoneField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    countryCode: String,
    dialCode: String,
    onChangeCountry: () -> Unit,
    label: String,
    dialLabel: String,
    error: String?,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    val dialValue = stringResource(R.string.component_phone_accessibilityValue, dialCode, countryCode)
    val hint = stringResource(R.string.component_combo_hint)
    HWTextField(
        value = digits,
        onValueChange = { onDigitsChange(PhoneNumber.nationalDigits(it)) },
        modifier = modifier,
        label = label,
        error = error,
        keyboardType = KeyboardType.Phone,
        imeAction = imeAction,
        onImeAction = onImeAction,
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .heightIn(min = MinTouchTarget)
                        .clip(HWRadius.Small)
                        .clickable(role = Role.Button, onClickLabel = hint, onClick = onChangeCountry)
                        // One control: "Change country code, +971, AE".
                        .clearAndSetSemantics {
                            contentDescription = dialLabel
                            stateDescription = dialValue
                            role = Role.Button
                            onClick(hint) { onChangeCountry(); true }
                        }
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    CodeChip(countryCode)
                    Text(dialCode, style = HWType.BodyLarge, color = HWColors.BrandInk)
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = HWColors.BrandInkSecondary)
                }
                Spacer(Modifier.width(8.dp))
                // `.sep` — a hairline between the two halves. Decoration.
                Box(Modifier.width(1.dp).height(28.dp).background(HWColors.BrandSeparator))
            }
        },
    )
}

/** `HWCodeChip` — a small rounded tile holding an ISO code. */
@Composable
fun CodeChip(code: String) {
    Box(
        Modifier
            .clip(HWRadius.Hairline)
            .background(HWColors.BrandSeparator)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(code, style = HWType.Micro.copy(fontWeight = FontWeight.Bold), color = HWColors.BrandInkAccent)
    }
}

/**
 * Port of iOS `HWDateField`: a field-shaped button that opens a Material 3 date picker in a dialog,
 * constrained to [range]. The chosen day is shown in the app's locale; the wire value is ISO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDateField(
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    range: ClosedRange<LocalDate>,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }

    HWComboField(
        value = date?.format(formatter),
        placeholder = placeholder,
        onClick = { choosing = true },
        modifier = modifier,
        label = label,
        error = error,
    )

    if (choosing) {
        val selectable = remember(range) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcDay(utcTimeMillis) in range
                override fun isSelectableYear(year: Int): Boolean = year in range.start.year..range.endInclusive.year
            }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.let(::utcMillis),
            initialDisplayedMonthMillis = utcMillis(date ?: range.endInclusive),
            yearRange = range.start.year..range.endInclusive.year,
            selectableDates = selectable,
        )
        DatePickerDialog(
            onDismissRequest = { choosing = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // As on iOS, Done with nothing tapped takes the day the picker opened on.
                        val chosen = state.selectedDateMillis?.let(::utcDay) ?: range.endInclusive
                        onDateChange(chosen)
                        choosing = false
                    },
                ) { Text(stringResource(R.string.component_date_done)) }
            },
            dismissButton = {
                TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.component_sheet_close)) }
            },
        ) {
            DatePicker(state = state, title = {
                Text(placeholder, style = HWType.Caption, modifier = Modifier.padding(start = 24.dp, top = 16.dp))
            })
        }
    }
}

private fun utcDay(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
private fun utcMillis(day: LocalDate): Long = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
