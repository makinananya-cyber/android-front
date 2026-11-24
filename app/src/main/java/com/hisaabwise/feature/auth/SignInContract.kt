package com.hisaabwise.feature.auth

import androidx.annotation.StringRes
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.components.ErrorCopy
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.Validation
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState
import com.hisaabwise.core.network.ApiError

enum class SignInField { Email, Password }

/**
 * What is wrong with the sign-in form, as a value (port of iOS `SignInFailure`). Every refusal is
 * one case, so the copy never says whether an account exists.
 */
sealed interface SignInFailure {
    data object EmailMissing : SignInFailure
    data object PasswordMissing : SignInFailure
    /** Under 8 characters — passwords are 8+ everywhere. */
    data object PasswordTooShort : SignInFailure
    data object CredentialsRefused : SignInFailure
    /** `ACCOUNT_PENDING_DELETION`: deletion is permanent, so this reads as "does not exist". */
    data object PendingDeletion : SignInFailure
    data object Unreachable : SignInFailure
    data class Refused(val code: ErrorCode) : SignInFailure

    val field: SignInField?
        get() = when (this) {
            EmailMissing -> SignInField.Email
            PasswordMissing, PasswordTooShort, CredentialsRefused -> SignInField.Password
            PendingDeletion, Unreachable, is Refused -> null
        }

    /** The only place a SignInFailure becomes words. */
    @get:StringRes
    val copy: Int
        get() = when (this) {
            EmailMissing -> R.string.signin_error_emailMissing
            PasswordMissing -> R.string.signin_error_passwordMissing
            PasswordTooShort -> R.string.signin_error_passwordTooShort
            CredentialsRefused -> R.string.signin_error_refused
            PendingDeletion -> R.string.signin_pendingDeletion_title
            Unreachable -> R.string.state_offline
            is Refused -> ErrorCopy.message(code)
        }

    companion object {
        /** Everything is a refusal unless it is a known fault (5xx, 429, 400). */
        fun of(error: Throwable): SignInFailure = when (error) {
            ApiError.Offline -> Unreachable
            ApiError.MalformedResponse -> Refused(ErrorCode.MalformedResponse)
            // A 401 on the sign-in route means "wrong credentials": there is no session yet to expire.
            ApiError.Unauthenticated -> CredentialsRefused
            is ApiError.Server -> when {
                error.code == ErrorCode.AccountPendingDeletion -> PendingDeletion
                AuthErrors.isFault(error.status) -> Refused(error.code)
                else -> CredentialsRefused
            }
            else -> Refused(ErrorCode.Unknown)
        }
    }
}

data class SignInState(
    val email: String = "",
    val password: String = "",
    /** Checked by default, as the design draws it: decides Keystore vs memory for the refresh token. */
    val keepMeSignedIn: Boolean = true,
    val failures: List<SignInFailure> = emptyList(),
    val isSigningIn: Boolean = false,
    /** Server refusals this visit. A hint for the support line only; never gates submission. */
    val refusals: Int = 0,
) : UiState {
    fun failure(field: SignInField): SignInFailure? = failures.firstOrNull { it.field == field }
    val formFailure: SignInFailure? get() = failures.firstOrNull { it.field == null }
    val suggestsSupport: Boolean get() = refusals >= REFUSALS_BEFORE_SUPPORT

    /** Emptiness and the 8+ rule only: the client does not judge the shape of an email address. */
    fun validate(): List<SignInFailure> = buildList {
        if (email.isBlank()) add(SignInFailure.EmailMissing)
        when {
            password.isEmpty() -> add(SignInFailure.PasswordMissing)
            password.length < Validation.MIN_PASSWORD_LENGTH -> add(SignInFailure.PasswordTooShort)
        }
    }

    companion object {
        const val REFUSALS_BEFORE_SUPPORT = 3
    }
}

sealed interface SignInIntent : UiIntent {
    data class EmailChanged(val value: String) : SignInIntent
    data class PasswordChanged(val value: String) : SignInIntent
    data class KeepSignedInChanged(val value: Boolean) : SignInIntent
    data object Submit : SignInIntent
    data object ForgotPassword : SignInIntent
    data object Register : SignInIntent
    data object Back : SignInIntent
}

sealed interface SignInEffect : UiEffect {
    data object GoToForgotPassword : SignInEffect
    data object GoToRegister : SignInEffect
    data object GoBack : SignInEffect
}
