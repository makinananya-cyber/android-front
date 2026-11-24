package com.hisaabwise.feature.auth.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWComboField
import com.hisaabwise.core.designsystem.components.HWMoneyField
import com.hisaabwise.core.designsystem.components.HWPickerSheet
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.core.model.SecurityQuestion
import com.hisaabwise.feature.auth.RegistrationField
import com.hisaabwise.feature.auth.RegistrationIntent
import com.hisaabwise.feature.auth.RegistrationState
import com.hisaabwise.feature.auth.components.AuthNote
import com.hisaabwise.feature.auth.components.CtaGlow
import com.hisaabwise.feature.auth.components.authEnters

private enum class MoneyPicker { Currency, FirstQuestion, SecondQuestion }

/**
 * Step 2 — "money & security" (port of iOS `RegistrationMoneyStep`). Each question slot's list
 * omits the other slot's question. Both answer boxes take focus: tapping either focuses it, and
 * Next on the first answer moves straight to the second (the iOS build could not focus the second).
 */
@Composable
fun RegistrationMoneyStep(state: RegistrationState, onIntent: (RegistrationIntent) -> Unit) {
    var picker by rememberSaveable { mutableStateOf<MoneyPicker?>(null) }
    val focus = LocalFocusManager.current
    val firstAnswer = remember { FocusRequester() }
    val secondAnswer = remember { FocusRequester() }

    @Composable
    fun errorFor(field: RegistrationField): String? = state.failure(field)?.let { stringResource(it.copy) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCap(stringResource(R.string.registration_two_money_section), Modifier.authEnters(3))
        HWComboField(
            value = state.currency?.let { stringResource(R.string.registration_two_currency_value, it.name, it.code) },
            placeholder = stringResource(R.string.registration_two_currency_placeholder),
            onClick = { picker = MoneyPicker.Currency },
            modifier = Modifier.authEnters(3),
            label = stringResource(R.string.registration_two_currency_label),
        )
        AuthNote(stringResource(R.string.registration_two_currency_hint), Modifier.authEnters(3), centred = false, alpha = 0.7f)
        HWMoneyField(
            text = state.salaryText,
            onTextChange = { onIntent(RegistrationIntent.SalaryChanged(it)) },
            currencyCode = state.currency?.code.orEmpty(),
            modifier = Modifier.authEnters(4),
            label = stringResource(R.string.registration_two_salary_label),
            error = errorFor(RegistrationField.Salary),
            imeAction = ImeAction.Done,
            onImeAction = { focus.clearFocus() },
        )

        SectionCap(stringResource(R.string.registration_two_security_section), Modifier.authEnters(4))
        AuthNote(stringResource(R.string.registration_two_security_hint), Modifier.authEnters(4), centred = false, alpha = 0.7f)

        Column(Modifier.authEnters(5), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QuestionCombo(
                label = stringResource(R.string.registration_two_question_one_label),
                question = state.firstQuestion,
                error = errorFor(RegistrationField.FirstQuestion),
                onClick = { picker = MoneyPicker.FirstQuestion },
            )
            HWTextField(
                value = state.firstAnswer,
                onValueChange = { onIntent(RegistrationIntent.FirstAnswerChanged(it)) },
                modifier = Modifier.focusRequester(firstAnswer),
                label = stringResource(R.string.registration_two_answer_one_label),
                error = errorFor(RegistrationField.FirstAnswer),
                imeAction = ImeAction.Next,
                onImeAction = { secondAnswer.requestFocus() },
                leading = { Icon(Icons.AutoMirrored.Outlined.List, null, tint = HWColors.BrandInkSecondary) },
            )
            QuestionCombo(
                label = stringResource(R.string.registration_two_question_two_label),
                question = state.secondQuestion,
                error = errorFor(RegistrationField.SecondQuestion),
                onClick = { picker = MoneyPicker.SecondQuestion },
            )
            HWTextField(
                value = state.secondAnswer,
                onValueChange = { onIntent(RegistrationIntent.SecondAnswerChanged(it)) },
                modifier = Modifier.focusRequester(secondAnswer),
                label = stringResource(R.string.registration_two_answer_two_label),
                error = errorFor(RegistrationField.SecondAnswer),
                imeAction = ImeAction.Done,
                onImeAction = { focus.clearFocus() },
                leading = { Icon(Icons.AutoMirrored.Outlined.List, null, tint = HWColors.BrandInkSecondary) },
            )
        }

        Box(Modifier.authEnters(5)) {
            CtaGlow {
                HWButton(
                    stringResource(R.string.registration_two_action),
                    onClick = {
                        focus.clearFocus()
                        onIntent(RegistrationIntent.Advance)
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
        }
    }

    when (picker) {
        MoneyPicker.Currency -> HWPickerSheet(
            title = stringResource(R.string.registration_two_currency_title),
            options = state.currencies,
            label = { it.name },
            detail = { "${it.code}  ${it.symbol}" },
            isSelected = { it.code == state.currency?.code },
            onSelect = {
                onIntent(RegistrationIntent.CurrencyChosen(it.code))
                picker = null
            },
            onDismiss = { picker = null },
            searchPlaceholder = stringResource(R.string.registration_two_currency_search),
        )
        MoneyPicker.FirstQuestion -> QuestionPicker(
            title = stringResource(R.string.registration_two_question_one_title),
            options = state.questionsExcluding(state.secondQuestion),
            selected = state.firstQuestion,
            onSelect = {
                onIntent(RegistrationIntent.FirstQuestionChosen(it.id))
                picker = null
            },
            onDismiss = { picker = null },
        )
        MoneyPicker.SecondQuestion -> QuestionPicker(
            title = stringResource(R.string.registration_two_question_two_title),
            options = state.questionsExcluding(state.firstQuestion),
            selected = state.secondQuestion,
            onSelect = {
                onIntent(RegistrationIntent.SecondQuestionChosen(it.id))
                picker = null
            },
            onDismiss = { picker = null },
        )
        null -> Unit
    }
}

@Composable
private fun QuestionCombo(label: String, question: SecurityQuestion?, error: String?, onClick: () -> Unit) {
    HWComboField(
        value = question?.text,
        placeholder = stringResource(R.string.registration_two_question_placeholder),
        onClick = onClick,
        label = label,
        error = error,
    )
}

@Composable
private fun QuestionPicker(
    title: String,
    options: List<SecurityQuestion>,
    selected: SecurityQuestion?,
    onSelect: (SecurityQuestion) -> Unit,
    onDismiss: () -> Unit,
) {
    HWPickerSheet(
        title = title,
        options = options,
        label = { it.text },
        isSelected = { it.id == selected?.id },
        onSelect = onSelect,
        onDismiss = onDismiss,
        searchPlaceholder = stringResource(R.string.registration_two_question_search),
    )
}

/** `.section-cap` — the uppercase rule above a group of fields. */
@Composable
internal fun SectionCap(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = HWType.Eyebrow,
        color = HWColors.BrandInkAccent,
        modifier = modifier.fillMaxWidth().semantics { heading() },
    )
}
