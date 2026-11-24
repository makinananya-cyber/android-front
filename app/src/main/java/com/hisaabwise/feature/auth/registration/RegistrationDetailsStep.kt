package com.hisaabwise.feature.auth.registration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWCheckbox
import com.hisaabwise.core.designsystem.components.HWPickerSheet
import com.hisaabwise.core.designsystem.components.HWStrengthMeter
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.core.model.PasswordStrength
import com.hisaabwise.feature.auth.RegistrationField
import com.hisaabwise.feature.auth.RegistrationIntent
import com.hisaabwise.feature.auth.RegistrationState
import com.hisaabwise.feature.auth.components.AuthDateField
import com.hisaabwise.feature.auth.components.AuthLink
import com.hisaabwise.feature.auth.components.AuthPhoneField
import com.hisaabwise.feature.auth.components.CtaGlow
import com.hisaabwise.feature.auth.components.authEnters

/** Step 1 — "about you" (port of iOS `RegistrationDetailsStep`). Nothing here talks to the server. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationDetailsStep(state: RegistrationState, onIntent: (RegistrationIntent) -> Unit) {
    var choosingCountry by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val email = remember { FocusRequester() }
    val phone = remember { FocusRequester() }
    val password = remember { FocusRequester() }
    val confirm = remember { FocusRequester() }

    @Composable
    fun errorFor(field: RegistrationField): String? = state.failure(field)?.let { stringResource(it.copy) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HWTextField(
            value = state.name,
            onValueChange = { onIntent(RegistrationIntent.NameChanged(it)) },
            modifier = Modifier.authEnters(3),
            label = stringResource(R.string.registration_name_label),
            error = errorFor(RegistrationField.Name),
            onImeAction = { email.requestFocus() },
            leading = { Icon(Icons.Outlined.Person, null, tint = HWColors.BrandInkSecondary) },
        )
        HWTextField(
            value = state.email,
            onValueChange = { onIntent(RegistrationIntent.EmailChanged(it)) },
            modifier = Modifier.authEnters(3).focusRequester(email),
            label = stringResource(R.string.registration_email_label),
            error = errorFor(RegistrationField.Email),
            keyboardType = KeyboardType.Email,
            onImeAction = { phone.requestFocus() },
            leading = { Icon(Icons.Outlined.Email, null, tint = HWColors.BrandInkSecondary) },
        )
        // Optional at launch; a number that is given must be 6–15 significant digits.
        AuthPhoneField(
            digits = state.phoneDigits,
            onDigitsChange = { onIntent(RegistrationIntent.PhoneChanged(it)) },
            countryCode = state.country?.code.orEmpty(),
            dialCode = state.country?.dialCode.orEmpty(),
            onChangeCountry = { choosingCountry = true },
            label = stringResource(R.string.registration_phone_label),
            dialLabel = stringResource(R.string.registration_phone_dial_accessibilityLabel),
            error = errorFor(RegistrationField.Phone),
            modifier = Modifier.authEnters(4).focusRequester(phone),
            onImeAction = { focus.clearFocus() },
        )
        AuthDateField(
            date = state.dateOfBirth,
            onDateChange = { onIntent(RegistrationIntent.DateOfBirthChosen(it)) },
            range = RegistrationState.dateOfBirthRange(),
            label = stringResource(R.string.registration_dateOfBirth_label),
            placeholder = stringResource(R.string.registration_dateOfBirth_placeholder),
            error = errorFor(RegistrationField.DateOfBirth),
            modifier = Modifier.authEnters(4),
        )
        Column(Modifier.authEnters(5), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            HWTextField(
                value = state.password,
                onValueChange = { onIntent(RegistrationIntent.PasswordChanged(it)) },
                modifier = Modifier.focusRequester(password),
                label = stringResource(R.string.registration_password_label),
                error = errorFor(RegistrationField.Password),
                isSecure = true,
                onImeAction = { confirm.requestFocus() },
                leading = { Icon(Icons.Outlined.Lock, null, tint = HWColors.BrandInkSecondary) },
            )
            // The meter appears the moment there is something to measure.
            AnimatedVisibility(state.passwordStrength != PasswordStrength.None) {
                HWStrengthMeter(state.passwordStrength, strengthLabel(state.passwordStrength)?.let { stringResource(it) })
            }
        }
        HWTextField(
            value = state.confirmPassword,
            onValueChange = { onIntent(RegistrationIntent.ConfirmPasswordChanged(it)) },
            modifier = Modifier.authEnters(5).focusRequester(confirm),
            label = stringResource(R.string.registration_confirmPassword_label),
            error = errorFor(RegistrationField.ConfirmPassword),
            isSecure = true,
            imeAction = ImeAction.Done,
            onImeAction = { focus.clearFocus() },
            leading = { Icon(Icons.Outlined.Shield, null, tint = HWColors.BrandInkSecondary) },
        )

        // The consent: the checkbox carries the lead-in; each document is its own link element.
        Column(Modifier.authEnters(5), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(verticalArrangement = Arrangement.Center) {
                val centred = Modifier.align(Alignment.CenterVertically)
                HWCheckbox(
                    checked = state.acceptedTerms,
                    onCheckedChange = { onIntent(RegistrationIntent.TermsChanged(it)) },
                    modifier = centred,
                ) {
                    Text(stringResource(R.string.registration_terms), style = HWType.Body, color = HWColors.BrandInk)
                }
                AuthLink(stringResource(R.string.registration_terms_privacy), centred) { onIntent(RegistrationIntent.OpenPrivacy) }
                Text(stringResource(R.string.auth_terms_and), style = HWType.Body, color = HWColors.BrandInk, modifier = centred)
                AuthLink(stringResource(R.string.auth_terms_terms), centred) { onIntent(RegistrationIntent.OpenTerms) }
            }
            errorFor(RegistrationField.Terms)?.let { message ->
                Text(message, style = HWType.Caption, color = HWColors.BrandDanger, modifier = Modifier.semantics { error(message) })
            }
        }

        Box(Modifier.authEnters(5)) {
            CtaGlow {
                HWButton(
                    stringResource(R.string.registration_one_action),
                    onClick = {
                        focus.clearFocus()
                        onIntent(RegistrationIntent.Advance)
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
        }
    }

    if (choosingCountry) {
        HWPickerSheet(
            title = stringResource(R.string.registration_country_title),
            options = state.countries,
            label = { it.name },
            // Searchable by name, ISO code, or dial code ("971", "AE", "Emirates").
            detail = { "${it.code}  ${it.dialCode}" },
            isSelected = { it.code == state.country?.code },
            onSelect = {
                onIntent(RegistrationIntent.CountryChosen(it.code))
                choosingCountry = false
            },
            onDismiss = { choosingCountry = false },
            searchPlaceholder = stringResource(R.string.registration_country_search),
        )
    }
}

internal fun strengthLabel(strength: PasswordStrength): Int? = when (strength) {
    PasswordStrength.None -> null
    PasswordStrength.Weak -> R.string.registration_password_strength_weak
    PasswordStrength.Fair -> R.string.registration_password_strength_fair
    PasswordStrength.Good -> R.string.registration_password_strength_good
    PasswordStrength.Strong -> R.string.registration_password_strength_strong
}
