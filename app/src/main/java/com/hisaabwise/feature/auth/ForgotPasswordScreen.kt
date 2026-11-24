package com.hisaabwise.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWMark
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.feature.auth.components.AuthDateField
import com.hisaabwise.feature.auth.components.AuthFormFailure
import com.hisaabwise.feature.auth.components.AuthNote
import com.hisaabwise.feature.auth.components.AuthPage
import com.hisaabwise.feature.auth.components.authEnters

@Composable
fun ForgotPasswordRoute(onBack: () -> Unit, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { ForgotPasswordViewModel(ApiRecovery(container.api)) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effects.collect {
            when (it) {
                ForgotPasswordEffect.Finished -> onDone()
                ForgotPasswordEffect.GoBack -> onBack()
            }
        }
    }
    ForgotPasswordScreen(state, vm::dispatch)
}

/**
 * Port of iOS `ForgotPasswordView`. The answer boxes are one loop over the questions the server
 * sent, each with its own FocusRequester, so the second box always takes focus.
 */
@Composable
fun ForgotPasswordScreen(state: ForgotPasswordState, onIntent: (ForgotPasswordIntent) -> Unit) {
    AuthPage(onBack = { onIntent(ForgotPasswordIntent.Back) }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            HWMark(30.dp, Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally).authEnters(0))

            if (state.didReset) {
                Column(Modifier.authEnters(1), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Heading(stringResource(R.string.recovery_done_heading), stringResource(R.string.recovery_done_blurb))
                    HWButton(
                        stringResource(R.string.recovery_done_action),
                        onClick = { onIntent(ForgotPasswordIntent.Done) },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.recovery_step, (state.stage.ordinal + 1).toString(), RecoveryStage.entries.size.toString()).uppercase(),
                    style = HWType.Eyebrow,
                    color = HWColors.BrandInkAccent,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().authEnters(1),
                )
                Column(Modifier.authEnters(2), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (state.stage) {
                        RecoveryStage.Email -> EmailStage(state, onIntent)
                        RecoveryStage.Identity -> IdentityStage(state, onIntent)
                        RecoveryStage.NewPassword -> NewPasswordStage(state, onIntent)
                    }
                }
                AnimatedVisibility(state.formFailure != null, enter = fadeIn(), exit = fadeOut()) {
                    state.formFailure?.let { AuthFormFailure(stringResource(it.copy)) }
                }
                AnimatedVisibility(state.suggestsSupport, enter = fadeIn(), exit = fadeOut()) {
                    AuthNote(stringResource(R.string.recovery_support))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.EmailStage(state: ForgotPasswordState, onIntent: (ForgotPasswordIntent) -> Unit) {
    val focus = LocalFocusManager.current
    Heading(stringResource(R.string.recovery_email_heading), stringResource(R.string.recovery_email_blurb))
    HWTextField(
        value = state.email,
        onValueChange = { onIntent(ForgotPasswordIntent.EmailChanged(it)) },
        label = stringResource(R.string.recovery_email_label),
        error = state.failure(RecoveryField.Email)?.let { stringResource(it.copy) },
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
        onImeAction = {
            focus.clearFocus()
            onIntent(ForgotPasswordIntent.LookUpQuestions)
        },
        leading = { Icon(Icons.Outlined.Email, null, tint = HWColors.BrandInkSecondary) },
    )
    Action(stringResource(R.string.recovery_email_action), state.isWorking) { onIntent(ForgotPasswordIntent.LookUpQuestions) }
}

@Composable
private fun ColumnScope.IdentityStage(state: ForgotPasswordState, onIntent: (ForgotPasswordIntent) -> Unit) {
    val focus = LocalFocusManager.current
    val requesters = remember(state.questions.size) { List(state.questions.size) { FocusRequester() } }
    Heading(stringResource(R.string.recovery_identity_heading), stringResource(R.string.recovery_identity_blurb))
    val answersError = state.failure(RecoveryField.Answers)?.let { stringResource(it.copy) }
    state.questions.forEachIndexed { index, question ->
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            // The question is server content in the reader's language.
            Text(question.text, style = HWType.Caption, color = HWColors.BrandInkSecondary)
            HWTextField(
                value = state.answers.getOrElse(index) { "" },
                onValueChange = { onIntent(ForgotPasswordIntent.AnswerChanged(index, it)) },
                modifier = Modifier.focusRequester(requesters[index]),
                label = stringResource(R.string.recovery_identity_answer_label),
                // Under the first box only: the server does not say which answer missed.
                error = if (index == 0) answersError else null,
                imeAction = if (index < state.questions.lastIndex) ImeAction.Next else ImeAction.Done,
                onImeAction = {
                    if (index < state.questions.lastIndex) requesters[index + 1].requestFocus() else focus.clearFocus()
                },
                leading = { Icon(Icons.Outlined.Shield, null, tint = HWColors.BrandInkSecondary) },
            )
        }
    }
    // The same span registration offers, so recovery never refuses the date the account was made with.
    AuthDateField(
        date = state.dateOfBirth,
        onDateChange = { onIntent(ForgotPasswordIntent.DateOfBirthChosen(it)) },
        range = RegistrationState.dateOfBirthRange(),
        label = stringResource(R.string.recovery_identity_dob_label),
        placeholder = stringResource(R.string.recovery_identity_dob_placeholder),
        error = state.failure(RecoveryField.DateOfBirth)?.let { stringResource(it.copy) },
    )
    Action(stringResource(R.string.recovery_identity_action), state.isWorking) { onIntent(ForgotPasswordIntent.VerifyIdentity) }
}

@Composable
private fun ColumnScope.NewPasswordStage(state: ForgotPasswordState, onIntent: (ForgotPasswordIntent) -> Unit) {
    val focus = LocalFocusManager.current
    val confirm = remember { FocusRequester() }
    Heading(stringResource(R.string.recovery_password_heading), stringResource(R.string.recovery_password_blurb))
    HWTextField(
        value = state.newPassword,
        onValueChange = { onIntent(ForgotPasswordIntent.NewPasswordChanged(it)) },
        label = stringResource(R.string.recovery_password_new_label),
        error = state.failure(RecoveryField.NewPassword)?.let { stringResource(it.copy) },
        isSecure = true,
        onImeAction = { confirm.requestFocus() },
        leading = { Icon(Icons.Outlined.Lock, null, tint = HWColors.BrandInkSecondary) },
    )
    HWTextField(
        value = state.confirmPassword,
        onValueChange = { onIntent(ForgotPasswordIntent.ConfirmPasswordChanged(it)) },
        modifier = Modifier.focusRequester(confirm),
        label = stringResource(R.string.recovery_password_confirm_label),
        error = state.failure(RecoveryField.ConfirmPassword)?.let { stringResource(it.copy) },
        isSecure = true,
        imeAction = ImeAction.Done,
        onImeAction = { focus.clearFocus() },
        leading = { Icon(Icons.Outlined.Shield, null, tint = HWColors.BrandInkSecondary) },
    )
    Action(stringResource(R.string.recovery_password_action), state.isWorking) { onIntent(ForgotPasswordIntent.ResetPassword) }
}

@Composable
private fun Heading(title: String, blurb: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = HWType.Heading, color = HWColors.BrandInk, modifier = Modifier.semantics { heading() })
        Text(blurb, style = HWType.Body, color = HWColors.BrandInkSecondary)
    }
}

@Composable
private fun Action(text: String, isWorking: Boolean, onClick: () -> Unit) {
    val focus = LocalFocusManager.current
    HWButton(
        text,
        onClick = {
            focus.clearFocus()
            onClick()
        },
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        // A busy control is not an unavailable one: the spinner keeps the button's shape.
        isLoading = isWorking,
    )
}
