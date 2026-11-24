package com.hisaabwise.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
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
import com.hisaabwise.core.designsystem.components.HWBackBar
import com.hisaabwise.core.designsystem.components.HWBanner
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWCard
import com.hisaabwise.core.designsystem.components.HWPickerSheet
import com.hisaabwise.core.designsystem.components.HWStrengthMeter
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.core.designsystem.components.PageHorizontalPadding
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.core.model.AppLanguage
import com.hisaabwise.core.model.PasswordStrength
import com.hisaabwise.feature.account.components.AccountDialTrigger
import com.hisaabwise.feature.account.components.AccountFieldNote
import com.hisaabwise.feature.account.components.AccountInfoNote
import com.hisaabwise.feature.account.components.AccountInfoRow
import com.hisaabwise.feature.account.components.AccountInfoValue
import com.hisaabwise.feature.account.components.AccountInlineField
import com.hisaabwise.feature.account.components.AccountRefusalNote
import com.hisaabwise.feature.account.components.AccountRowCard
import com.hisaabwise.feature.account.components.AccountStepHeader

/** The shared chrome of a pushed Account page: the ground, the back bar and the title. */
@Composable
private fun DetailPage(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    SurfaceGround {
        Column(Modifier.fillMaxSize()) {
            HWBackBar(title, onBack, backDescription = stringResource(R.string.account_back), trailing = trailing)
            content()
        }
    }
}

/** A page whose payload has gone (a write failed offline underneath it): say so. */
@Composable
private fun DetailUnavailable() {
    Box(Modifier.padding(horizontal = PageHorizontalPadding, vertical = 14.dp)) {
        HWBanner(stringResource(R.string.account_detail_unavailable), isError = true)
    }
}

// region Personal information

@Composable
fun AccountPersonalScreen(state: AccountState, onIntent: (AccountIntent) -> Unit, onBack: () -> Unit) {
    val intents by rememberUpdatedState(onIntent)
    // Catch up on every appearance (invariant 2: the salary has one owner) and leave edit mode on the way out.
    LaunchedEffect(Unit) { intents(AccountIntent.RefreshQuietly) }
    DisposableEffect(Unit) { onDispose { intents(AccountIntent.CancelEditingPersonal) } }
    LaunchedEffect(state.isEditingPersonal) { if (state.isEditingPersonal) intents(AccountIntent.LoadDialCodes) }

    var choosingDialCode by rememberSaveable { mutableStateOf(false) }

    DetailPage(
        title = stringResource(R.string.account_personal_title),
        onBack = onBack,
        trailing = {
            if (state.screen != null) {
                EditToggle(
                    isOn = state.isEditingPersonal,
                    isWriting = state.isWriting,
                    onClick = { onIntent(if (state.isEditingPersonal) AccountIntent.SavePersonal else AccountIntent.BeginEditingPersonal) },
                )
            }
        },
    ) {
        val details = state.screen?.personal
        if (details == null) {
            DetailUnavailable()
            return@DetailPage
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = PageHorizontalPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PersonalCard(state, details, onIntent, onChangeDialCode = { choosingDialCode = true })

            if (!details.isEmailVerified) HWBanner(stringResource(R.string.account_personal_email_unverified))

            if (state.isEditingPersonal) {
                HWButton(
                    stringResource(R.string.account_personal_save),
                    onClick = { onIntent(AccountIntent.SavePersonal) },
                    icon = Icons.Filled.Check,
                    isLoading = state.isWriting,
                )
            }
            Box(Modifier.navigationBarsPadding())
        }
    }

    if (choosingDialCode) {
        HWPickerSheet(
            title = stringResource(R.string.account_personal_dialSheet_title),
            options = state.countries,
            label = { it.name },
            detail = { "${it.code} ${it.dialCode}" },
            isSelected = { it.code == state.personal.country },
            onSelect = {
                onIntent(AccountIntent.ChooseDialCode(it.code))
                choosingDialCode = false
            },
            onDismiss = { choosingDialCode = false },
            searchPlaceholder = stringResource(R.string.account_personal_dialSheet_search),
        )
    }
}

@Composable
private fun PersonalCard(
    state: AccountState,
    details: AccountScreen.Personal,
    onIntent: (AccountIntent) -> Unit,
    onChangeDialCode: () -> Unit,
) {
    val editing = state.isEditingPersonal
    val draft = state.personal
    val nameError = stringResource(R.string.account_personal_error_name)
    val salaryError = stringResource(R.string.account_personal_error_salary)
    val phoneError = stringResource(R.string.account_personal_error_phone)

    AccountRowCard(stringResource(R.string.account_personal_card)) {
        val usernameLabel = stringResource(R.string.account_personal_username)
        AccountInfoRow(usernameLabel, isSeparated = false) {
            if (editing) {
                AccountInlineField(
                    usernameLabel, draft.displayName, { onIntent(AccountIntent.EditDisplayName(it)) },
                    isInvalid = PersonalDraft.Failure.NameMissing in draft.failures, invalidMessage = nameError,
                )
            } else {
                AccountInfoValue(details.displayName)
            }
        }
        if (PersonalDraft.Failure.NameMissing in draft.failures) AccountFieldNote(nameError)

        val salaryLabel = stringResource(R.string.account_personal_salary)
        AccountInfoRow(salaryLabel) {
            if (editing) {
                AccountInlineField(
                    salaryLabel, draft.salary,
                    { text -> onIntent(AccountIntent.EditSalary(text.filter { it.isDigit() || it == '.' || it == ',' })) },
                    symbol = details.salaryCurrency.symbol,
                    keyboardType = KeyboardType.Decimal,
                    isInvalid = PersonalDraft.Failure.SalaryMissing in draft.failures, invalidMessage = salaryError,
                )
            } else {
                // The server's display string — never formatted here.
                AccountInfoValue(details.salary.display, unit = stringResource(R.string.account_personal_salary_unit))
            }
        }
        if (PersonalDraft.Failure.SalaryMissing in draft.failures) AccountFieldNote(salaryError)

        AccountInfoRow(stringResource(R.string.account_personal_email)) {
            AccountInfoValue(details.email, isLocked = true)
        }
        AccountInfoNote(stringResource(R.string.account_personal_email_note))

        val phoneLabel = stringResource(R.string.account_personal_phone)
        AccountInfoRow(phoneLabel) {
            if (editing) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccountDialTrigger(draft.country, draft.dialCode, stringResource(R.string.account_personal_phone_dial), onChangeDialCode)
                    AccountInlineField(
                        phoneLabel, draft.national, { onIntent(AccountIntent.EditPhoneDigits(it)) },
                        keyboardType = KeyboardType.Phone,
                        isInvalid = PersonalDraft.Failure.PhoneInvalid in draft.failures, invalidMessage = phoneError,
                    )
                }
            } else {
                AccountInfoValue(details.phone?.display ?: stringResource(R.string.account_personal_phone_none))
            }
        }
        if (PersonalDraft.Failure.PhoneInvalid in draft.failures) AccountFieldNote(phoneError)
    }
}

/** The Edit / Done pill in the back bar. Done commits. */
@Composable
private fun EditToggle(isOn: Boolean, isWriting: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (isOn) R.string.account_personal_done else R.string.account_personal_edit)
    Row(
        Modifier
            .heightIn(min = MinTouchTarget)
            .clip(HWRadius.Pill)
            .background(if (isOn) HWColors.AccentDeep else HWColors.Surface)
            .border(1.dp, HWColors.SeparatorStrong, HWRadius.Pill)
            .clickable(enabled = !isWriting, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val ink = if (isOn) HWColors.Surface else HWColors.AccentDeep
        if (isWriting) CircularProgressIndicator(Modifier.widthIn(max = 16.dp).heightIn(max = 16.dp), color = ink, strokeWidth = 2.dp)
        else Icon(if (isOn) Icons.Filled.Check else Icons.Filled.Edit, null, tint = ink, modifier = Modifier.widthIn(max = 16.dp).heightIn(max = 16.dp))
        Text(label, style = HWType.Caption.copy(fontWeight = FontWeight.Bold), color = ink)
    }
}

// endregion

// region Language and currency

/** One option in a settings list. [searchText] is what the search box matches. */
private data class AccountOption(val id: String, val name: String, val chip: String, val meta: String?, val searchText: String)

/**
 * The picker list behind Language and Currency: blurb, a search box above 12 options, rows with a
 * chip, name and meta, and a tick on the current choice. Opens scrolled to the current choice.
 */
@Composable
private fun AccountOptionList(
    blurb: String,
    searchPlaceholder: String,
    options: List<AccountOption>,
    selection: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(query, options) {
        val needle = query.trim()
        if (needle.isEmpty()) options else options.filter { it.searchText.contains(needle, ignoreCase = true) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(options.size) {
        val index = options.indexOfFirst { it.id == selection }
        if (index > 0 && query.isEmpty()) listState.scrollToItem(index)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = PageHorizontalPadding)) {
        Text(blurb, style = HWType.Body, color = HWColors.InkSecondary, modifier = Modifier.padding(bottom = 14.dp))
        if (options.size > 12) {
            HWTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = searchPlaceholder,
                imeAction = ImeAction.Search,
                leading = { Icon(Icons.Filled.Search, null, tint = HWColors.InkSecondary) },
                trailing = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.component_picker_clearSearch), tint = HWColors.InkSecondary)
                    }
                },
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        if (visible.isEmpty() && options.isNotEmpty()) {
            Text(stringResource(R.string.component_picker_empty), style = HWType.Body, color = HWColors.InkSecondary, modifier = Modifier.padding(vertical = 24.dp))
        }
        if (options.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = HWColors.Accent) }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(HWRadius.Card)
                .background(HWColors.Surface)
                .alpha(if (enabled) 1f else 0.6f),
        ) {
            items(visible, key = { it.id }) { option ->
                val isSelected = option.id == selection
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(enabled = enabled, role = Role.RadioButton) { onSelect(option.id) }
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                            if (option.meta != null) stateDescription = option.meta
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.widthIn(min = 44.dp).clip(HWRadius.Small).background(HWColors.Tint).padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(option.chip, style = HWType.Caption.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Accent) }
                    Text(option.name, style = HWType.BodyLarge, color = HWColors.Ink, modifier = Modifier.weight(1f))
                    if (option.meta != null) Text(option.meta, style = HWType.Caption, color = HWColors.InkSecondary)
                    if (isSelected) Icon(Icons.Filled.Check, null, tint = HWColors.Accent)
                }
                HorizontalDivider(color = HWColors.Separator)
            }
            item { Box(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) }
        }
    }
}

@Composable
fun AccountLanguageScreen(state: AccountState, onIntent: (AccountIntent) -> Unit, onBack: () -> Unit) {
    DetailPage(stringResource(R.string.account_language_title), onBack) {
        if (state.screen == null) {
            DetailUnavailable()
            return@DetailPage
        }
        AccountRefusalNote(state.refusal(Refusal.Subject.Language), Modifier.padding(horizontal = PageHorizontalPadding).padding(bottom = 14.dp))
        // The shipped languages, by endonym — never the 87-language reference list.
        val options = remember {
            AppLanguage.shipped.map { AccountOption(it.tag, it.endonym, it.tag.uppercase(), null, "${it.endonym} ${it.tag}") }
        }
        AccountOptionList(
            blurb = stringResource(R.string.account_language_blurb),
            searchPlaceholder = stringResource(R.string.account_language_search),
            options = options,
            // The payload's language, not the optimistic manager's.
            selection = state.storedLanguage?.tag,
            enabled = !state.isWriting,
        ) { tag -> AppLanguage.fromTag(tag)?.let { onIntent(AccountIntent.SelectLanguage(it)) } }
    }
}

@Composable
fun AccountCurrencyScreen(state: AccountState, onIntent: (AccountIntent) -> Unit, onBack: () -> Unit) {
    val intents by rememberUpdatedState(onIntent)
    LaunchedEffect(Unit) { intents(AccountIntent.LoadCurrencies) }
    DetailPage(stringResource(R.string.account_currency_title), onBack) {
        if (state.screen == null) {
            DetailUnavailable()
            return@DetailPage
        }
        AccountRefusalNote(state.refusal(Refusal.Subject.Currency), Modifier.padding(horizontal = PageHorizontalPadding).padding(bottom = 14.dp))
        val options = remember(state.currencies) {
            state.currencies.map { AccountOption(it.code, it.name, it.symbol, it.code, "${it.name} ${it.code} ${it.symbol}") }
        }
        AccountOptionList(
            blurb = stringResource(R.string.account_currency_blurb),
            searchPlaceholder = stringResource(R.string.account_currency_search),
            options = options,
            selection = state.screen?.currency,
            // Disabled only while a change is in flight — never after a refusal, so it can be retried.
            enabled = !state.isWriting,
        ) { code -> onIntent(AccountIntent.SelectCurrency(code)) }
    }
}

// endregion

// region Password

@Composable
fun AccountPasswordScreen(state: AccountState, onIntent: (AccountIntent) -> Unit, onBack: () -> Unit) {
    val intents by rememberUpdatedState(onIntent)
    // Start at step one, and clear both passwords from memory on the way out.
    LaunchedEffect(Unit) { intents(AccountIntent.BeginPasswordChange) }
    DisposableEffect(Unit) { onDispose { intents(AccountIntent.BeginPasswordChange) } }

    DetailPage(stringResource(R.string.account_password_title), onBack) {
        if (state.screen == null) {
            DetailUnavailable()
            return@DetailPage
        }
        val draft = state.password
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = PageHorizontalPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AccountStepHeader(
                stepIndex = draft.step.ordinal,
                stepCount = PasswordStep.entries.size,
                label = stringResource(stepLabel(draft.step)),
                backLabel = stringResource(R.string.account_password_back),
                onBack = if (draft.step == PasswordStep.CurrentPassword) null else ({ onIntent(AccountIntent.RetreatPasswordChange) }),
            )
            HWCard {
                when (draft.step) {
                    PasswordStep.CurrentPassword -> CurrentPasswordStep(draft, onIntent)
                    PasswordStep.SecurityQuestions -> QuestionsStep(state, onIntent)
                    PasswordStep.NewPassword -> NewPasswordStep(state, onIntent)
                }
            }
            HWButton(
                stringResource(actionTitle(draft.step)),
                onClick = { onIntent(AccountIntent.AdvancePasswordChange) },
                icon = if (draft.step == PasswordStep.NewPassword) Icons.Filled.Check else null,
                isLoading = state.isWriting,
            )
            Box(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun StepHeading(title: Int, blurb: Int) {
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = HWType.Subheading, color = HWColors.Ink)
        Text(stringResource(blurb), style = HWType.Body, color = HWColors.InkSecondary)
    }
}

@Composable
private fun CurrentPasswordStep(draft: PasswordDraft, onIntent: (AccountIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepHeading(R.string.account_password_current_heading, R.string.account_password_current_blurb)
        HWTextField(
            value = draft.current,
            onValueChange = { onIntent(AccountIntent.EditCurrentPassword(it)) },
            label = stringResource(R.string.account_password_current_field),
            error = message(draft.failure, PasswordStep.CurrentPassword)?.let { stringResource(it) },
            isSecure = true,
            imeAction = ImeAction.Done,
            onImeAction = { onIntent(AccountIntent.AdvancePasswordChange) },
        )
    }
}

@Composable
private fun QuestionsStep(state: AccountState, onIntent: (AccountIntent) -> Unit) {
    val draft = state.password
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepHeading(R.string.account_password_questions_heading, R.string.account_password_questions_blurb)
        state.securityQuestions.forEachIndexed { index, question ->
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                // The question is server content; the field's label is app copy.
                Text(question.text, style = HWType.Caption.copy(fontWeight = FontWeight.Bold), color = HWColors.Ink)
                HWTextField(
                    value = draft.answers.getOrElse(index) { "" },
                    onValueChange = { onIntent(AccountIntent.EditAnswer(index, it)) },
                    label = stringResource(R.string.account_password_questions_answer),
                    // Under the first box only: the server does not say which answer missed.
                    error = if (index == 0) message(draft.failure, PasswordStep.SecurityQuestions)?.let { stringResource(it) } else null,
                    imeAction = if (index == state.securityQuestions.lastIndex) ImeAction.Done else ImeAction.Next,
                )
            }
        }
        if (draft.shouldOfferSupport) {
            Text(stringResource(R.string.account_password_questions_support), style = HWType.Micro.copy(fontWeight = FontWeight.Normal), color = HWColors.InkTertiary)
        }
    }
}

@Composable
private fun NewPasswordStep(state: AccountState, onIntent: (AccountIntent) -> Unit) {
    val draft = state.password
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StepHeading(R.string.account_password_new_heading, R.string.account_password_new_blurb)
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            HWTextField(
                value = draft.newPassword,
                onValueChange = { onIntent(AccountIntent.EditNewPassword(it)) },
                label = stringResource(R.string.account_password_new_field),
                // The mismatch message belongs to the confirmation box.
                error = message(draft.failure?.takeIf { it != PasswordDraft.Failure.ConfirmationMismatch }, PasswordStep.NewPassword)?.let { stringResource(it) },
                isSecure = true,
            )
            HWStrengthMeter(state.passwordStrength, strengthLabel(state.passwordStrength)?.let { stringResource(it) })
        }
        HWTextField(
            value = draft.confirmation,
            onValueChange = { onIntent(AccountIntent.EditConfirmation(it)) },
            label = stringResource(R.string.account_password_new_confirm),
            error = if (draft.failure == PasswordDraft.Failure.ConfirmationMismatch) stringResource(R.string.account_password_error_mismatch) else null,
            isSecure = true,
            imeAction = ImeAction.Done,
            onImeAction = { onIntent(AccountIntent.AdvancePasswordChange) },
        )
    }
}

private fun stepLabel(step: PasswordStep): Int = when (step) {
    PasswordStep.CurrentPassword -> R.string.account_password_step_one
    PasswordStep.SecurityQuestions -> R.string.account_password_step_two
    PasswordStep.NewPassword -> R.string.account_password_step_three
}

private fun actionTitle(step: PasswordStep): Int = when (step) {
    PasswordStep.CurrentPassword -> R.string.account_password_continue
    PasswordStep.SecurityQuestions -> R.string.account_password_check
    PasswordStep.NewPassword -> R.string.account_password_submit
}

/** A failure as the message under the box it is about, or `null` on a step it is not about. */
internal fun message(failure: PasswordDraft.Failure?, step: PasswordStep): Int? {
    if (failure == null || failure.step != step) return null
    return when (failure) {
        PasswordDraft.Failure.CurrentPasswordMissing -> R.string.account_password_error_currentMissing
        PasswordDraft.Failure.AnswerMissing -> R.string.account_password_error_answerMissing
        PasswordDraft.Failure.NewPasswordTooShort -> R.string.account_password_error_tooShort
        PasswordDraft.Failure.ConfirmationMismatch -> R.string.account_password_error_mismatch
        PasswordDraft.Failure.CurrentPasswordRejected -> R.string.account_password_error_currentRejected
        PasswordDraft.Failure.AnswersRejected -> R.string.account_password_error_answersRejected
    }
}

private fun strengthLabel(strength: PasswordStrength): Int? = when (strength) {
    PasswordStrength.None -> null
    PasswordStrength.Weak -> R.string.registration_password_strength_weak
    PasswordStrength.Fair -> R.string.registration_password_strength_fair
    PasswordStrength.Good -> R.string.registration_password_strength_good
    PasswordStrength.Strong -> R.string.registration_password_strength_strong
}

// endregion
