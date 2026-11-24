package com.hisaabwise.feature.auth

import androidx.annotation.StringRes
import com.hisaabwise.R
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.SecurityQuestion
import com.hisaabwise.core.model.Validation
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState
import com.hisaabwise.core.network.ApiError
import java.time.LocalDate

/** Recovery's three stages: who are you → prove it → choose a new password. */
enum class RecoveryStage { Email, Identity, NewPassword }

enum class RecoveryField { Email, Answers, DateOfBirth, NewPassword, ConfirmPassword }

/** Port of iOS `RecoveryFailure`. One refusal case for all three factors: it never says which missed. */
sealed interface RecoveryFailure {
    data object EmailMissing : RecoveryFailure
    data object AnswerMissing : RecoveryFailure
    data object DateOfBirthMissing : RecoveryFailure
    data object PasswordMissing : RecoveryFailure
    data object PasswordTooShort : RecoveryFailure
    data object PasswordsDiffer : RecoveryFailure
    data object IdentityRefused : RecoveryFailure
    data object Locked : RecoveryFailure
    data object TicketExpired : RecoveryFailure
    data object Unreachable : RecoveryFailure
    data class Refused(val code: ErrorCode) : RecoveryFailure

    val field: RecoveryField?
        get() = when (this) {
            EmailMissing -> RecoveryField.Email
            AnswerMissing -> RecoveryField.Answers
            DateOfBirthMissing -> RecoveryField.DateOfBirth
            PasswordMissing, PasswordTooShort -> RecoveryField.NewPassword
            PasswordsDiffer -> RecoveryField.ConfirmPassword
            IdentityRefused, Locked, TicketExpired, Unreachable, is Refused -> null
        }

    @get:StringRes
    val copy: Int
        get() = when (this) {
            EmailMissing -> R.string.recovery_error_emailMissing
            AnswerMissing -> R.string.recovery_error_answerMissing
            DateOfBirthMissing -> R.string.recovery_error_dateOfBirthMissing
            PasswordMissing -> R.string.recovery_error_passwordMissing
            PasswordTooShort -> R.string.recovery_error_passwordTooShort
            PasswordsDiffer -> R.string.recovery_error_passwordsDiffer
            IdentityRefused -> R.string.recovery_error_identityRefused
            Locked -> R.string.recovery_error_locked
            TicketExpired -> R.string.recovery_error_ticketExpired
            Unreachable -> R.string.state_offline
            is Refused -> R.string.state_failed_generic
        }

    companion object {
        fun of(error: Throwable): RecoveryFailure = when (error) {
            ApiError.Offline -> Unreachable
            ApiError.MalformedResponse -> Refused(ErrorCode.MalformedResponse)
            // Anonymous routes: a 401 is a refusal, the safe reading that says nothing extra.
            ApiError.Unauthenticated -> IdentityRefused
            is ApiError.Server -> when (error.code) {
                ErrorCode.SecurityAnswersInvalid -> IdentityRefused
                ErrorCode.AccountLocked -> Locked
                ErrorCode.ResetTicketInvalid -> TicketExpired
                else -> if (AuthErrors.isFault(error.status)) Refused(error.code) else IdentityRefused
            }
            else -> Refused(ErrorCode.Unknown)
        }
    }
}

data class ForgotPasswordState(
    val stage: RecoveryStage = RecoveryStage.Email,
    val email: String = "",
    /** The two questions this account was set up with (server content), one answer each. */
    val questions: List<SecurityQuestion> = emptyList(),
    val answers: List<String> = emptyList(),
    val dateOfBirth: LocalDate? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val failures: List<RecoveryFailure> = emptyList(),
    val isWorking: Boolean = false,
    /** The terminal state: the password is reset and the reader goes to sign in. */
    val didReset: Boolean = false,
    val refusals: Int = 0,
) : UiState {
    fun failure(field: RecoveryField): RecoveryFailure? = failures.firstOrNull { it.field == field }
    val formFailure: RecoveryFailure? get() = failures.firstOrNull { it.field == null }
    val suggestsSupport: Boolean get() = refusals >= 3

    fun validateEmail(): List<RecoveryFailure> = if (email.isBlank()) listOf(RecoveryFailure.EmailMissing) else emptyList()

    /** Every answer, not just the first. */
    fun validateIdentity(): List<RecoveryFailure> = buildList {
        if (answers.isEmpty() || answers.any { it.isBlank() }) add(RecoveryFailure.AnswerMissing)
        if (dateOfBirth == null) add(RecoveryFailure.DateOfBirthMissing)
    }

    fun validateNewPassword(): List<RecoveryFailure> = when {
        newPassword.isEmpty() -> listOf(RecoveryFailure.PasswordMissing)
        newPassword.length < Validation.MIN_PASSWORD_LENGTH -> listOf(RecoveryFailure.PasswordTooShort)
        confirmPassword != newPassword -> listOf(RecoveryFailure.PasswordsDiffer)
        else -> emptyList()
    }
}

sealed interface ForgotPasswordIntent : UiIntent {
    data class EmailChanged(val value: String) : ForgotPasswordIntent
    data class AnswerChanged(val index: Int, val value: String) : ForgotPasswordIntent
    data class DateOfBirthChosen(val value: LocalDate) : ForgotPasswordIntent
    data class NewPasswordChanged(val value: String) : ForgotPasswordIntent
    data class ConfirmPasswordChanged(val value: String) : ForgotPasswordIntent
    data object LookUpQuestions : ForgotPasswordIntent
    data object VerifyIdentity : ForgotPasswordIntent
    data object ResetPassword : ForgotPasswordIntent
    data object Done : ForgotPasswordIntent
    data object Back : ForgotPasswordIntent
}

sealed interface ForgotPasswordEffect : UiEffect {
    data object Finished : ForgotPasswordEffect
    data object GoBack : ForgotPasswordEffect
}
