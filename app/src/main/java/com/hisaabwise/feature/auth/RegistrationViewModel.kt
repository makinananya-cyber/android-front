package com.hisaabwise.feature.auth

import androidx.lifecycle.viewModelScope
import com.hisaabwise.app.LegalLinks
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.CountryList
import com.hisaabwise.core.model.CurrencyList
import com.hisaabwise.core.model.RegistrationRequest
import com.hisaabwise.core.model.SecurityQuestionList
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.attempt
import com.hisaabwise.core.network.ApiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** The three reference lists, in parallel (port of the iOS `ContentLoader` calls). */
interface RegistrationLists {
    suspend fun countries(): CountryList
    suspend fun currencies(): CurrencyList
    suspend fun questions(): SecurityQuestionList
}

/**
 * Registration: three steps in the UI, one request at the end (port of iOS `RegistrationViewModel`).
 * Nothing is sent until Submit or Skip, so there is no half-built account and no
 * email-availability oracle; an `EMAIL_TAKEN` collision sends the user back to step 1.
 */
class RegistrationViewModel(
    private val lists: RegistrationLists,
    private val register: suspend (RegistrationRequest) -> Unit,
    private val legal: LegalLinks,
    private val language: () -> String,
) : MviViewModel<RegistrationState, RegistrationIntent, RegistrationEffect>(RegistrationState()) {

    /** Single-flight: a second caller awaits the load in flight rather than returning early. */
    private var listsInFlight: Deferred<Unit>? = null

    init {
        dispatch(RegistrationIntent.LoadReferenceLists)
    }

    override suspend fun handle(intent: RegistrationIntent) {
        when (intent) {
            RegistrationIntent.LoadReferenceLists -> loadReferenceLists()

            is RegistrationIntent.NameChanged -> edit(RegistrationField.Name) { copy(name = intent.value) }
            is RegistrationIntent.EmailChanged -> edit(RegistrationField.Email) { copy(email = intent.value) }
            is RegistrationIntent.CountryChosen -> {
                // An unknown id is ignored rather than clearing the choice.
                val match = current.countries.firstOrNull { it.code == intent.code } ?: return
                edit(RegistrationField.Phone) { copy(country = match) }
            }
            is RegistrationIntent.PhoneChanged -> edit(RegistrationField.Phone) { copy(phoneDigits = intent.value) }
            is RegistrationIntent.DateOfBirthChosen -> edit(RegistrationField.DateOfBirth) { copy(dateOfBirth = intent.value) }
            // "Those do not match" is about both boxes, so editing either clears it.
            is RegistrationIntent.PasswordChanged -> edit(RegistrationField.Password, RegistrationField.ConfirmPassword) { copy(password = intent.value) }
            is RegistrationIntent.ConfirmPasswordChanged -> edit(RegistrationField.ConfirmPassword) { copy(confirmPassword = intent.value) }
            is RegistrationIntent.TermsChanged -> edit(RegistrationField.Terms) { copy(acceptedTerms = intent.value) }
            RegistrationIntent.OpenPrivacy -> emit(RegistrationEffect.OpenUrl(legal.privacy))
            RegistrationIntent.OpenTerms -> emit(RegistrationEffect.OpenUrl(legal.terms))

            is RegistrationIntent.CurrencyChosen -> {
                val match = current.currencies.firstOrNull { it.code == intent.code } ?: return
                edit(RegistrationField.Salary) { copy(currency = match) }
            }
            is RegistrationIntent.SalaryChanged -> edit(RegistrationField.Salary) { copy(salaryText = intent.value) }
            is RegistrationIntent.FirstQuestionChosen -> {
                val match = current.questions.firstOrNull { it.id == intent.id } ?: return
                edit(RegistrationField.FirstQuestion) { copy(firstQuestion = match) }
            }
            // Each answer clears its own failure, never the question's.
            is RegistrationIntent.FirstAnswerChanged -> edit(RegistrationField.FirstAnswer) { copy(firstAnswer = intent.value) }
            is RegistrationIntent.SecondQuestionChosen -> {
                val match = current.questions.firstOrNull { it.id == intent.id } ?: return
                edit(RegistrationField.SecondQuestion) { copy(secondQuestion = match) }
            }
            is RegistrationIntent.SecondAnswerChanged -> edit(RegistrationField.SecondAnswer) { copy(secondAnswer = intent.value) }

            // Typing is what makes the goal the user's own rather than the suggestion.
            is RegistrationIntent.GoalChanged -> edit(RegistrationField.Goal) { copy(goalText = intent.value, goalIsSuggestion = false) }
            RegistrationIntent.UseSuggestedGoal -> {
                val suggested = current.suggestedGoalText ?: return
                edit(RegistrationField.Goal) { copy(goalText = suggested, goalIsSuggestion = true) }
            }

            RegistrationIntent.Advance -> advance()
            RegistrationIntent.GoBack -> {
                val previous = current.step.previous ?: return
                reduce { copy(failures = emptyList(), step = previous) }
                emit(RegistrationEffect.Announce(previous.label))
            }
            RegistrationIntent.Submit -> {
                reduce { copy(goalWasSkipped = false) }
                submit(current.goalMinor)
            }
            RegistrationIntent.SkipGoal -> {
                // Skip is not "no goal": it accepts the 20% suggestion and records that it was not chosen.
                reduce { copy(goalWasSkipped = true) }
                submit(current.suggestedGoalMinor)
            }
            RegistrationIntent.SignInInstead -> emit(RegistrationEffect.GoToSignIn)
            RegistrationIntent.Leave -> emit(RegistrationEffect.GoBack)
        }
    }

    private fun edit(vararg cleared: RegistrationField, change: RegistrationState.() -> RegistrationState) {
        reduce { change().copy(failures = failures.filterNot { it.field in cleared }) }
    }

    private suspend fun loadReferenceLists() {
        listsInFlight?.let { it.await(); return }
        val task = viewModelScope.async { fetchReferenceLists() }
        listsInFlight = task
        try {
            task.await()
        } finally {
            listsInFlight = null
        }
    }

    private suspend fun fetchReferenceLists() {
        reduce { copy(isLoadingReferenceLists = true, referenceFailure = null) }
        try {
            // All three or none: a form with countries and no currencies is a trap.
            val (countries, currencies, questions) = coroutineScope {
                val c = async { lists.countries().countries }
                val m = async { lists.currencies().currencies }
                val q = async { lists.questions().questions }
                Triple(c.await(), m.await(), q.await())
            }
            reduce {
                copy(
                    countries = countries,
                    currencies = currencies,
                    questions = questions,
                    isLoadingReferenceLists = false,
                    // Defaults only over an empty choice, so going back never resets a pick.
                    country = country ?: (countries.firstOrNull { it.code == RegistrationState.DEFAULT_COUNTRY_CODE } ?: countries.firstOrNull()),
                    currency = currency ?: (currencies.firstOrNull { it.code == RegistrationState.DEFAULT_CURRENCY_CODE } ?: currencies.firstOrNull()),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiError) {
            reduce { copy(isLoadingReferenceLists = false, referenceFailure = RegistrationFailure.of(e)) }
        } catch (e: Exception) {
            reduce { copy(isLoadingReferenceLists = false, referenceFailure = RegistrationFailure.of(e)) }
        }
    }

    /** Validates the current step and advances. The only way forward. */
    private fun advance() {
        val state = current
        val failures = state.validate(state.step, state.goalMinor)
        reduce { copy(failures = failures) }
        announceFirst(failures)
        val next = state.step.next
        if (failures.isNotEmpty() || next == null) return
        reduce {
            // Pre-fill step 3 with the suggestion, re-derived each time unless the user typed their own.
            val goal = if (next == RegistrationStep.Three && goalIsSuggestion) suggestedGoalText ?: goalText else goalText
            copy(step = next, goalText = goal)
        }
        emit(RegistrationEffect.Announce(next.label))
    }

    private suspend fun submit(goalMinor: Long?) {
        if (current.isSubmitting) return
        val failures = current.validateAll(goalMinor)
        if (failures.isNotEmpty()) {
            // Back to the earliest step with something wrong, which is where it can be fixed.
            val step = failures.minOf { it.step }
            reduce { copy(failures = failures, step = step) }
            announceFirst(failures)
            return
        }
        val request = current.request(goalMinor, language()) ?: return
        reduce { copy(failures = emptyList(), isSubmitting = true) }
        // Not cancellable: success swaps the pre-auth stack away while the coordinator finishes.
        val result = withContext(NonCancellable) { attempt { register(request) } }
        result.onFailure { error ->
            val failure = RegistrationFailure.of(error)
            reduce { copy(failures = listOf(failure), step = if (failure.field != null) failure.step else step) }
            announceFirst(listOf(failure))
        }
        reduce { copy(isSubmitting = false) }
    }

    private fun announceFirst(failures: List<RegistrationFailure>) {
        failures.firstOrNull()?.let { emit(RegistrationEffect.Announce(it.copy)) }
    }
}
