package com.hisaabwise.feature.account

import androidx.lifecycle.viewModelScope
import com.hisaabwise.core.i18n.LanguageManager
import com.hisaabwise.core.model.AppLanguage
import com.hisaabwise.core.model.AuthoringCurrency
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.CountryList
import com.hisaabwise.core.model.CurrencyList
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PhoneNumber
import com.hisaabwise.core.model.SecurityAnswer
import com.hisaabwise.core.model.TypedAmount
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.attempt
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.ApiError
import com.hisaabwise.core.network.ContentLoader
import com.hisaabwise.core.network.Endpoint
import com.hisaabwise.core.session.ScreenRepaint
import com.hisaabwise.core.session.SessionCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Account: one read, four writes, and the two settings that make every other screen stale.
 * Port of iOS `AccountViewModel` — one instance serves the root and its four detail pages.
 *
 * - The salary is never a number here: `Money.minor` out, typed text back, through [TypedAmount].
 * - Security answers are never compared here; the server verifies (invariant 5).
 * - Currency and language are requests; what comes back is the truth, then every screen repaints.
 */
class AccountViewModel(
    private val api: ApiClient,
    private val content: ContentLoader,
    private val language: LanguageManager,
    private val session: SessionCoordinator,
    private val repaint: ScreenRepaint,
    private val contactEmail: String,
) : MviViewModel<AccountState, AccountIntent, AccountEffect>(AccountState()) {

    /** The currency the salary is typed in, from the latest payload — kept across a failed write. */
    private var authoring: AuthoringCurrency? = null

    private enum class WriteKind { Details, Password, Currency }

    init {
        dispatch(AccountIntent.Load)
        viewModelScope.launch {
            // Another screen (or this one) changed a preference: re-read. Quietly when a payload is
            // already on screen, so an open page is not blanked to a spinner.
            repaint.requests.collect {
                if (current.content is LoadState.Loaded) refreshQuietly() else load()
            }
        }
    }

    override suspend fun handle(intent: AccountIntent) {
        when (intent) {
            AccountIntent.Load -> load()
            AccountIntent.RefreshQuietly -> refreshQuietly()

            AccountIntent.BeginEditingPersonal -> beginEditingPersonal()
            AccountIntent.CancelEditingPersonal -> reduce { copy(isEditingPersonal = false, personal = PersonalDraft()) }
            is AccountIntent.EditDisplayName -> reduce {
                copy(personal = personal.copy(displayName = intent.text, failures = personal.failures - PersonalDraft.Failure.NameMissing))
            }
            is AccountIntent.EditSalary -> reduce {
                copy(personal = personal.copy(salary = intent.text, failures = personal.failures - PersonalDraft.Failure.SalaryMissing))
            }
            is AccountIntent.EditPhoneDigits -> reduce {
                copy(
                    personal = personal.copy(
                        national = PhoneNumber.nationalDigits(intent.text),
                        failures = personal.failures - PersonalDraft.Failure.PhoneInvalid,
                    ),
                )
            }
            is AccountIntent.ChooseDialCode -> {
                val country = current.countries.firstOrNull { it.code == intent.country } ?: return
                reduce {
                    copy(
                        personal = personal.copy(
                            country = country.code,
                            dialCode = country.dialCode,
                            failures = personal.failures - PersonalDraft.Failure.PhoneInvalid,
                        ),
                    )
                }
            }
            AccountIntent.SavePersonal -> savePersonalDetails()
            AccountIntent.LoadDialCodes -> loadDialCodes()

            AccountIntent.LoadCurrencies -> loadCurrencies()
            is AccountIntent.SelectCurrency -> selectCurrency(intent.code)
            is AccountIntent.SelectLanguage -> selectLanguage(intent.language)

            AccountIntent.BeginPasswordChange -> reduce {
                copy(password = PasswordDraft(answers = List(securityQuestions.size) { "" }))
            }
            is AccountIntent.EditCurrentPassword -> reduce { copy(password = password.copy(current = intent.text, failure = null)) }
            is AccountIntent.EditAnswer -> reduce {
                val answers = password.answers.toMutableList()
                if (intent.index in answers.indices) answers[intent.index] = intent.text
                copy(password = password.copy(answers = answers, failure = null))
            }
            is AccountIntent.EditNewPassword -> reduce { copy(password = password.copy(newPassword = intent.text, failure = null)) }
            is AccountIntent.EditConfirmation -> reduce { copy(password = password.copy(confirmation = intent.text, failure = null)) }
            AccountIntent.AdvancePasswordChange -> advancePasswordChange()
            AccountIntent.RetreatPasswordChange -> {
                val previous = current.password.step.previous ?: return
                reduce { copy(password = password.copy(step = previous, failure = null)) }
            }

            AccountIntent.DeleteAccount -> deleteAccount()
            AccountIntent.SignOut -> session.signOut()
            AccountIntent.ContactSupport -> emit(AccountEffect.OpenMail(contactEmail))
            AccountIntent.DismissNotice -> reduce { copy(notice = null) }
        }
    }

    // region Read

    private suspend fun fetch(): AccountScreen {
        val screen = api.get<AccountScreen>(Endpoint.SCREEN_ACCOUNT)
        authoring = screen.personal.salaryCurrency
        return screen
    }

    private suspend fun load() {
        reduce { copy(content = LoadState.Loading) }
        val loaded = loadState { fetch() }
        reduce { copy(content = loaded) }
    }

    /** Re-reads without blanking; a failure is silent because a good payload is already on screen. */
    private suspend fun refreshQuietly() {
        val screen = try {
            fetch()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return
        }
        reduce { copy(content = LoadState.Loaded(screen)) }
    }

    private suspend fun loadCurrencies() {
        if (current.currencies.isNotEmpty()) return
        val list = runCatching { content.load<CurrencyList>(ContentResource.Currencies) }
            .getOrNull()?.currencies.orEmpty()
        reduce { copy(currencies = list) }
    }

    private suspend fun loadDialCodes() {
        if (current.countries.isNotEmpty()) return
        val list = runCatching { content.load<CountryList>(ContentResource.Countries) }
            .getOrNull()?.countries.orEmpty()
        reduce { copy(countries = list) }
    }

    // endregion

    // region Personal

    private fun beginEditingPersonal() {
        val details = current.screen?.personal ?: return
        reduce {
            copy(
                isEditingPersonal = true,
                personal = PersonalDraft(
                    displayName = details.displayName,
                    salary = TypedAmount.major(details.salary.minor, details.salary.exponent),
                    country = details.phone?.country.orEmpty(),
                    dialCode = details.phone?.dialCode.orEmpty(),
                    national = details.phone?.national.orEmpty(),
                ),
            )
        }
    }

    private suspend fun savePersonalDetails() {
        val request = validatedPersonalDetails() ?: return
        val saved = write(WriteKind.Details, AccountNotice.PersonalUpdated) {
            api.put<PersonalDetailsUpdate, AccountScreen>(Endpoint.ME, request)
        }
        if (saved) reduce { copy(isEditingPersonal = false, personal = PersonalDraft()) }
    }

    /** Local input validation: every bad field at once, as the design marks them. */
    private fun validatedPersonalDetails(): PersonalDetailsUpdate? {
        val currency = authoring ?: return null
        val draft = current.personal
        val failures = mutableSetOf<PersonalDraft.Failure>()

        val name = draft.displayName.trim()
        if (name.isEmpty()) failures += PersonalDraft.Failure.NameMissing

        val minor = TypedAmount.minor(draft.salary, currency.exponent)
        if (minor == null) failures += PersonalDraft.Failure.SalaryMissing

        // Blank is allowed (phone is optional); a partial number is not.
        val digits = PhoneNumber.nationalDigits(draft.national)
        val hasCountry = draft.country.isNotEmpty() && draft.dialCode.isNotEmpty()
        if (digits.isNotEmpty() && (digits.length !in 6..15 || !hasCountry)) failures += PersonalDraft.Failure.PhoneInvalid

        reduce { copy(personal = personal.copy(failures = failures)) }
        if (failures.isNotEmpty() || minor == null) return null

        return PersonalDetailsUpdate(
            displayName = name,
            salary = MoneyAmount(minor, currency.code),
            phone = if (digits.isEmpty()) null else PhoneNumber.of(draft.country, draft.dialCode, digits),
        )
    }

    // endregion

    // region Preferences

    private suspend fun selectCurrency(code: String) {
        val currentCode = current.screen?.currency ?: return
        if (code == currentCode) {
            emit(AccountEffect.CloseDetail)
            return
        }
        val changed = write(WriteKind.Currency, AccountNotice.CurrencyChanged) {
            api.put<DisplayCurrencyUpdate, AccountScreen>(Endpoint.CURRENCY, DisplayCurrencyUpdate(code))
        }
        if (changed) {
            repaint.repaintEveryScreen()
            emit(AccountEffect.CloseDetail)
        }
    }

    private suspend fun selectLanguage(selected: AppLanguage) {
        if (current.isWriting) return
        reduce { copy(isWriting = true, refusal = null) }
        try {
            language.select(selected, api)
        } catch (e: CancellationException) {
            reduce { copy(isWriting = false) }
            throw e
        } catch (e: Exception) {
            reduce { copy(isWriting = false, refusal = Refusal(Refusal.Subject.Language, Refusal.Reason.of(e))) }
            return
        }
        // The switch landed on the server; re-read quietly so a failed follow-up GET never reports
        // an error for a change that worked.
        refreshQuietly()
        reduce { copy(isWriting = false, notice = AccountNotice.LanguageChanged) }
        repaint.repaintEveryScreen()
        emit(AccountEffect.CloseDetail)
    }

    // endregion

    // region Password

    private suspend fun advancePasswordChange() {
        val draft = current.password
        when (draft.step) {
            PasswordStep.CurrentPassword -> {
                if (draft.current.isEmpty()) {
                    reduce { copy(password = password.copy(failure = PasswordDraft.Failure.CurrentPasswordMissing)) }
                    return
                }
                if (!confirmPasswordStep(sendingAnswers = false)) return
                reduce { copy(password = password.copy(step = PasswordStep.SecurityQuestions)) }
            }
            PasswordStep.SecurityQuestions -> {
                if (draft.answers.any { it.isBlank() }) {
                    reduce { copy(password = password.copy(failure = PasswordDraft.Failure.AnswerMissing)) }
                    return
                }
                if (!confirmPasswordStep(sendingAnswers = true)) return
                reduce { copy(password = password.copy(step = PasswordStep.NewPassword)) }
            }
            PasswordStep.NewPassword -> submitPasswordChange()
        }
    }

    private fun answers(): List<SecurityAnswer> =
        current.securityQuestions.zip(current.password.answers) { question, answer ->
            // The id, never the question's text (D12).
            SecurityAnswer(question.id, answer.trim())
        }

    /**
     * Advisory `POST /v1/me/password/check`. `true` means "carry on". Only INVALID_CREDENTIALS and
     * SECURITY_ANSWERS_INVALID block; offline or anything else lets the reader through, because the
     * final submit is authoritative.
     */
    private suspend fun confirmPasswordStep(sendingAnswers: Boolean): Boolean {
        if (current.isWriting) return false
        reduce { copy(isWriting = true) }
        val body = PasswordCheck(current.password.current, if (sendingAnswers) answers() else null)
        val result = try {
            attempt { api.post<PasswordCheck, PasswordCheckAccepted>(Endpoint.PASSWORD_CHECK, body) }
        } finally {
            reduce { copy(isWriting = false) }
        }
        val error = result.exceptionOrNull() ?: run {
            reduce { copy(password = password.copy(failure = null)) }
            return true
        }
        val rejection = (error as? ApiError.Server)?.let { PasswordDraft.Failure.of(it.code) } ?: return true
        reduce {
            copy(
                password = password.copy(
                    failure = rejection,
                    step = rejection.step,
                    // Counted here as well as on submit: with the check step, answers are refused at
                    // the check, so the support note would otherwise never appear.
                    attempts = password.attempts + if (rejection == PasswordDraft.Failure.AnswersRejected) 1 else 0,
                ),
            )
        }
        return false
    }

    private suspend fun submitPasswordChange() {
        val draft = current.password
        if (draft.newPassword.length < PasswordDraft.MINIMUM_LENGTH) {
            reduce { copy(password = password.copy(failure = PasswordDraft.Failure.NewPasswordTooShort)) }
            return
        }
        if (draft.confirmation != draft.newPassword) {
            reduce { copy(password = password.copy(failure = PasswordDraft.Failure.ConfirmationMismatch)) }
            return
        }
        if (current.securityQuestions.size != draft.answers.size) return

        val request = PasswordChange(draft.current, answers(), draft.newPassword)
        val changed = write(WriteKind.Password, AccountNotice.PasswordChanged) {
            api.post<PasswordChange, AccountScreen>(Endpoint.PASSWORD, request)
        }
        if (changed) {
            reduce { copy(password = PasswordDraft(answers = List(securityQuestions.size) { "" })) }
            emit(AccountEffect.CloseDetail)
        }
    }

    // endregion

    // region Delete

    private suspend fun deleteAccount() {
        if (current.isDeletingAccount) return
        reduce { copy(isDeletingAccount = true, refusal = null) }
        val result = try {
            attempt { api.delete<AccountDeletion>(Endpoint.DELETE_ACCOUNT) }
        } finally {
            reduce { copy(isDeletingAccount = false) }
        }
        result
            .onSuccess { session.signOut() }
            .onFailure { e -> reduce { copy(refusal = Refusal(Refusal.Subject.DeleteAccount, Refusal.Reason.of(e))) } }
    }

    // endregion

    // region One write

    /** Sends a write and re-renders from the screen payload it answers with. */
    private suspend fun write(kind: WriteKind, landed: AccountNotice, send: suspend () -> AccountScreen): Boolean {
        if (current.isWriting) return false
        reduce { copy(isWriting = true, refusal = null) }
        val result = try {
            attempt { send() }
        } finally {
            reduce { copy(isWriting = false) }
        }
        result.onSuccess { screen ->
            authoring = screen.personal.salaryCurrency
            reduce { copy(content = LoadState.Loaded(screen), notice = landed) }
            return true
        }
        apply(result.exceptionOrNull() ?: ApiError.MalformedResponse, kind)
        return false
    }

    private fun apply(error: Throwable, kind: WriteKind) {
        if (kind == WriteKind.Currency) {
            reduce { copy(refusal = Refusal(Refusal.Subject.Currency, Refusal.Reason.of(error))) }
            return
        }
        val apiError = error as? ApiError ?: run {
            reduce { copy(content = LoadState.Failed(com.hisaabwise.core.model.ErrorCode.Unknown)) }
            return
        }
        if (kind == WriteKind.Password) {
            val rejection = PasswordDraft.Failure.of(apiError.errorCode)
            if (rejection != null) {
                reduce {
                    copy(
                        password = password.copy(
                            failure = rejection,
                            step = rejection.step,
                            attempts = password.attempts + if (rejection == PasswordDraft.Failure.AnswersRejected) 1 else 0,
                        ),
                    )
                }
                return
            }
        }
        reduce {
            copy(
                content = if (apiError == ApiError.Offline) LoadState.Offline
                else LoadState.Failed(apiError.errorCode ?: com.hisaabwise.core.model.ErrorCode.Unknown),
            )
        }
    }

    // endregion
}
