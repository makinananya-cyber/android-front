package com.hisaabwise.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.designsystem.components.HWCheckbox
import com.hisaabwise.core.designsystem.components.HWMark
import com.hisaabwise.core.designsystem.components.HWTextField
import com.hisaabwise.feature.auth.components.AuthFormFailure
import com.hisaabwise.feature.auth.components.AuthHairline
import com.hisaabwise.feature.auth.components.AuthHead
import com.hisaabwise.feature.auth.components.AuthLink
import com.hisaabwise.feature.auth.components.AuthNote
import com.hisaabwise.feature.auth.components.AuthPage
import com.hisaabwise.feature.auth.components.CtaGlow
import com.hisaabwise.feature.auth.components.FlexSpace
import com.hisaabwise.feature.auth.components.authEnters

@Composable
fun SignInRoute(onBack: () -> Unit, onForgotPassword: () -> Unit, onRegister: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { SignInViewModel(container.session::signIn) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effects.collect {
            when (it) {
                SignInEffect.GoToForgotPassword -> onForgotPassword()
                SignInEffect.GoToRegister -> onRegister()
                SignInEffect.GoBack -> onBack()
            }
        }
    }
    SignInScreen(state, vm::dispatch)
}

/** Port of iOS `SignInView`. The identifier is the email; the failure copy is the same whatever the reason. */
@Composable
fun SignInScreen(state: SignInState, onIntent: (SignInIntent) -> Unit) {
    val focus = LocalFocusManager.current
    val password = remember { FocusRequester() }

    AuthPage(onBack = { onIntent(SignInIntent.Back) }) {
        HWMark(34.dp, Modifier.authEnters(0))
        FlexSpace(20.dp)
        AuthHead(
            stringResource(R.string.signin_title_first),
            stringResource(R.string.signin_title_second),
            stringResource(R.string.signin_subtitle),
            Modifier.authEnters(1),
        )
        FlexSpace(22.dp)

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            HWTextField(
                value = state.email,
                onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
                modifier = Modifier.authEnters(2),
                label = stringResource(R.string.signin_email_label),
                error = state.failure(SignInField.Email)?.let { stringResource(it.copy) },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                onImeAction = { password.requestFocus() },
                leading = { Icon(Icons.Outlined.Email, null, tint = HWColors.BrandInkSecondary) },
            )
            HWTextField(
                value = state.password,
                onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
                modifier = Modifier.authEnters(3).focusRequester(password),
                label = stringResource(R.string.signin_password_label),
                error = state.failure(SignInField.Password)?.let { stringResource(it.copy) },
                isSecure = true,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focus.clearFocus()
                    onIntent(SignInIntent.Submit)
                },
                leading = { Icon(Icons.Outlined.Lock, null, tint = HWColors.BrandInkSecondary) },
            )

            // `.row` — the checkbox and the reset link on one line.
            Row(Modifier.fillMaxWidth().authEnters(3), verticalAlignment = Alignment.CenterVertically) {
                HWCheckbox(
                    checked = state.keepMeSignedIn,
                    onCheckedChange = { onIntent(SignInIntent.KeepSignedInChanged(it)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.signin_keepSignedIn), style = HWType.Body, color = HWColors.BrandInk)
                }
                Spacer(Modifier.width(12.dp))
                AuthLink(stringResource(R.string.signin_forgot)) { onIntent(SignInIntent.ForgotPassword) }
            }

            Box(Modifier.authEnters(4)) {
                CtaGlow {
                    HWButton(
                        stringResource(R.string.signin_action),
                        onClick = {
                            focus.clearFocus()
                            onIntent(SignInIntent.Submit)
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        isLoading = state.isSigningIn,
                    )
                }
            }

            AnimatedVisibility(state.formFailure != null, enter = fadeIn(), exit = fadeOut()) {
                state.formFailure?.let { AuthFormFailure(stringResource(it.copy)) }
            }
            // Three refusals in, offer a way out. The server's backoff is the authority.
            AnimatedVisibility(state.suggestsSupport, enter = fadeIn(), exit = fadeOut()) {
                AuthNote(stringResource(R.string.signin_error_support))
            }
        }

        FlexSpace(24.dp)

        Column(
            Modifier.fillMaxWidth().authEnters(5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AuthHairline()
            AuthNote(stringResource(R.string.signin_register_prompt), style = HWType.Body)
            HWButton(
                stringResource(R.string.signin_register_action),
                onClick = { onIntent(SignInIntent.Register) },
                style = HWButtonStyle.Ghost,
            )
        }
    }
}
