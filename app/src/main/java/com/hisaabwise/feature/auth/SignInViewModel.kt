package com.hisaabwise.feature.auth

import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.attempt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Sign in: validate locally, then ask [signIn] (SessionCoordinator.signIn). Success flips
 * `isSignedIn` and RootScreen swaps to the shell, so this screen never navigates on success.
 */
class SignInViewModel(
    private val signIn: suspend (email: String, password: String, keepMeSignedIn: Boolean) -> Unit,
) : MviViewModel<SignInState, SignInIntent, SignInEffect>(SignInState()) {

    override suspend fun handle(intent: SignInIntent) {
        when (intent) {
            // Editing a box clears the failure under it.
            is SignInIntent.EmailChanged -> reduce { copy(email = intent.value, failures = failures.filterNot { it.field == SignInField.Email }) }
            is SignInIntent.PasswordChanged -> reduce { copy(password = intent.value, failures = failures.filterNot { it.field == SignInField.Password }) }
            is SignInIntent.KeepSignedInChanged -> reduce { copy(keepMeSignedIn = intent.value) }
            SignInIntent.Submit -> submit()
            SignInIntent.ForgotPassword -> emit(SignInEffect.GoToForgotPassword)
            SignInIntent.Register -> emit(SignInEffect.GoToRegister)
            SignInIntent.Back -> emit(SignInEffect.GoBack)
        }
    }

    private suspend fun submit() {
        if (current.isSigningIn) return
        val failures = current.validate()
        reduce { copy(failures = failures) }
        if (failures.isNotEmpty()) return

        reduce { copy(isSigningIn = true) }
        val form = current
        // Not cancellable: on success the root swaps the pre-auth stack away mid-call, and the
        // coordinator's follow-up `/v1/me` must still complete (iOS runs this in an unstructured Task).
        val result = withContext(NonCancellable) {
            attempt { signIn(form.email, form.password, form.keepMeSignedIn) }
        }
        result.onFailure { error ->
            val failure = SignInFailure.of(error)
            reduce {
                copy(
                    failures = listOf(failure),
                    // Only a refusal counts: a tunnel is not a wrong password.
                    refusals = if (failure == SignInFailure.CredentialsRefused) refusals + 1 else refusals,
                )
            }
        }
        reduce { copy(isSigningIn = false) }
    }
}
