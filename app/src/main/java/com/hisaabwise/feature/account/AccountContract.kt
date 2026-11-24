package com.hisaabwise.feature.account

import com.hisaabwise.core.model.AppLanguage
import com.hisaabwise.core.model.Country
import com.hisaabwise.core.model.Currency
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.PasswordStrength
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState
import com.hisaabwise.core.network.ApiError

/**
 * One contract for Account's root and its four detail pages — one view model serves them all,
 * as on iOS (`AccountViewModel`).
 */
data class AccountState(
    val content: LoadState<AccountScreen> = LoadState.Loading,
    val isEditingPersonal: Boolean = false,
    val personal: PersonalDraft = PersonalDraft(),
    val password: PasswordDraft = PasswordDraft(),
    val notice: AccountNotice? = null,
    val refusal: Refusal? = null,
    val isWriting: Boolean = false,
    val isDeletingAccount: Boolean = false,
    /** The 160 currencies, once fetched (on demand). */
    val currencies: List<Currency> = emptyList(),
    /** The 251 countries, once fetched (on demand, for the dial-code sheet). */
    val countries: List<Country> = emptyList(),
) : UiState {
    val screen: AccountScreen? get() = content.valueOrNull
    val securityQuestions get() = screen?.password?.questions.orEmpty()
    val storedLanguage: AppLanguage? get() = AppLanguage.fromTag(screen?.language)
    val passwordStrength: PasswordStrength get() = PasswordStrength.of(password.newPassword)

    fun refusal(about: Refusal.Subject): Refusal.Reason? = refusal?.takeIf { it.subject == about }?.reason
}

/** The three editable personal fields as they currently read. Port of iOS `PersonalDraft`. */
data class PersonalDraft(
    val displayName: String = "",
    /** As typed, in major units. Filled from `Money.minor`, never from `Money.display` (D16). */
    val salary: String = "",
    val country: String = "",
    val dialCode: String = "",
    val national: String = "",
    val failures: Set<Failure> = emptySet(),
) {
    enum class Failure { NameMissing, SalaryMissing, PhoneInvalid }
}

enum class PasswordStep {
    CurrentPassword, SecurityQuestions, NewPassword;

    val previous: PasswordStep? get() = entries.getOrNull(ordinal - 1)
}

/** What has been typed into the password flow. Port of iOS `PasswordDraft`. */
data class PasswordDraft(
    val step: PasswordStep = PasswordStep.CurrentPassword,
    val current: String = "",
    /** One per question, in the payload's order. */
    val answers: List<String> = emptyList(),
    val newPassword: String = "",
    val confirmation: String = "",
    val failure: Failure? = null,
    /** How many times the answers have been refused; support is offered after three. */
    val attempts: Int = 0,
) {
    val shouldOfferSupport: Boolean get() = attempts >= 3

    enum class Failure(val step: PasswordStep) {
        CurrentPasswordMissing(PasswordStep.CurrentPassword),
        AnswerMissing(PasswordStep.SecurityQuestions),
        NewPasswordTooShort(PasswordStep.NewPassword),
        ConfirmationMismatch(PasswordStep.NewPassword),
        CurrentPasswordRejected(PasswordStep.CurrentPassword),
        AnswersRejected(PasswordStep.SecurityQuestions);

        companion object {
            /** A server code as a field error, or `null` for a code that is not about a field. */
            fun of(code: ErrorCode?): Failure? = when (code) {
                ErrorCode.InvalidCredentials -> CurrentPasswordRejected
                ErrorCode.SecurityAnswersInvalid -> AnswersRejected
                else -> null
            }
        }
    }

    companion object {
        const val MINIMUM_LENGTH = 8
    }
}

enum class AccountNotice { PersonalUpdated, CurrencyChanged, LanguageChanged, PasswordChanged }

/** Why one thing did not happen — a notice beside the control that was tried, not a screen state. */
data class Refusal(val subject: Subject, val reason: Reason) {
    enum class Subject { Currency, Language, DeleteAccount }
    enum class Reason {
        NeedsConnection, Refused;

        companion object {
            fun of(error: Throwable): Reason = if (error == ApiError.Offline) NeedsConnection else Refused
        }
    }
}

sealed interface AccountIntent : UiIntent {
    data object Load : AccountIntent
    data object RefreshQuietly : AccountIntent

    // Personal
    data object BeginEditingPersonal : AccountIntent
    data object CancelEditingPersonal : AccountIntent
    data class EditDisplayName(val text: String) : AccountIntent
    data class EditSalary(val text: String) : AccountIntent
    data class EditPhoneDigits(val text: String) : AccountIntent
    data class ChooseDialCode(val country: String) : AccountIntent
    data object SavePersonal : AccountIntent
    data object LoadDialCodes : AccountIntent

    // Preferences
    data object LoadCurrencies : AccountIntent
    data class SelectCurrency(val code: String) : AccountIntent
    data class SelectLanguage(val language: AppLanguage) : AccountIntent

    // Password
    data object BeginPasswordChange : AccountIntent
    data class EditCurrentPassword(val text: String) : AccountIntent
    data class EditAnswer(val index: Int, val text: String) : AccountIntent
    data class EditNewPassword(val text: String) : AccountIntent
    data class EditConfirmation(val text: String) : AccountIntent
    data object AdvancePasswordChange : AccountIntent
    data object RetreatPasswordChange : AccountIntent

    // Root
    data object DeleteAccount : AccountIntent
    data object SignOut : AccountIntent
    data object ContactSupport : AccountIntent
    data object DismissNotice : AccountIntent
}

sealed interface AccountEffect : UiEffect {
    /** A detail page's change landed: close the page (language, currency, password). */
    data object CloseDetail : AccountEffect
    data class OpenMail(val address: String) : AccountEffect
}
