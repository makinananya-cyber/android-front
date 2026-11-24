package com.hisaabwise.feature.auth

import com.hisaabwise.core.model.Acknowledgement
import com.hisaabwise.core.model.SecurityQuestionList
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.attempt
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.Endpoint

/** The three anonymous recovery calls, behind a seam so the view model can be driven in tests. */
interface RecoveryApi {
    suspend fun questions(request: RecoveryQuestionsRequest): SecurityQuestionList
    suspend fun verify(request: RecoveryVerifyRequest): RecoveryTicket
    suspend fun reset(request: RecoveryResetRequest)
}

class ApiRecovery(private val api: ApiClient) : RecoveryApi {
    override suspend fun questions(request: RecoveryQuestionsRequest): SecurityQuestionList =
        api.post(Endpoint.FORGOT_PASSWORD_QUESTIONS, request, auth = ApiClient.Auth.Anonymous)

    override suspend fun verify(request: RecoveryVerifyRequest): RecoveryTicket =
        api.post(Endpoint.FORGOT_PASSWORD_VERIFY, request, auth = ApiClient.Auth.Anonymous)

    /** The token pair it answers with is deliberately discarded: a recovery signs every device out. */
    override suspend fun reset(request: RecoveryResetRequest) {
        api.post<RecoveryResetRequest, Acknowledgement>(Endpoint.RESET_PASSWORD, request, auth = ApiClient.Auth.Anonymous)
    }
}

/**
 * Recovery by security question (port of iOS `ForgotPasswordViewModel`). An unknown address still
 * gets two (decoy) questions from the server, so reaching stage two proves nothing here.
 */
class ForgotPasswordViewModel(private val api: RecoveryApi) :
    MviViewModel<ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordEffect>(ForgotPasswordState()) {

    /** The single-use ticket. A credential: never in State, never rendered, never persisted. */
    private var ticket: String? = null

    override suspend fun handle(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> edit(RecoveryField.Email) { copy(email = intent.value) }
            is ForgotPasswordIntent.AnswerChanged -> {
                if (intent.index !in current.answers.indices) return
                edit(RecoveryField.Answers) { copy(answers = answers.toMutableList().also { it[intent.index] = intent.value }) }
            }
            is ForgotPasswordIntent.DateOfBirthChosen -> edit(RecoveryField.DateOfBirth) { copy(dateOfBirth = intent.value) }
            is ForgotPasswordIntent.NewPasswordChanged -> edit(RecoveryField.NewPassword) { copy(newPassword = intent.value) }
            is ForgotPasswordIntent.ConfirmPasswordChanged -> edit(RecoveryField.ConfirmPassword) { copy(confirmPassword = intent.value) }
            ForgotPasswordIntent.LookUpQuestions -> lookUpQuestions()
            ForgotPasswordIntent.VerifyIdentity -> verifyIdentity()
            ForgotPasswordIntent.ResetPassword -> resetPassword()
            ForgotPasswordIntent.Done -> emit(ForgotPasswordEffect.Finished)
            ForgotPasswordIntent.Back -> emit(ForgotPasswordEffect.GoBack)
        }
    }

    private fun edit(field: RecoveryField, change: ForgotPasswordState.() -> ForgotPasswordState) {
        reduce { change().copy(failures = failures.filterNot { it.field == field }) }
    }

    private suspend fun lookUpQuestions() {
        if (current.isWorking) return
        val failures = current.validateEmail()
        reduce { copy(failures = failures) }
        if (failures.isNotEmpty()) return

        reduce { copy(isWorking = true) }
        attempt { api.questions(RecoveryQuestionsRequest(current.email.trim())) }
            .onSuccess { list ->
                reduce { copy(questions = list.questions, answers = List(list.questions.size) { "" }, stage = RecoveryStage.Identity) }
            }
            .onFailure { error -> reduce { copy(failures = listOf(RecoveryFailure.of(error))) } }
        reduce { copy(isWorking = false) }
    }

    private suspend fun verifyIdentity() {
        if (current.isWorking) return
        val failures = current.validateIdentity()
        reduce { copy(failures = failures) }
        if (failures.isNotEmpty()) return
        val state = current
        val dob = state.dateOfBirth ?: return

        reduce { copy(isWorking = true) }
        val request = RecoveryVerifyRequest(
            email = state.email.trim(),
            dateOfBirth = RegistrationState.isoDay(dob),
            answers = state.questions.zip(state.answers) { q, a -> RecoverySubmittedAnswer(q.id, a.trim()) },
        )
        attempt { api.verify(request) }
            .onSuccess { response ->
                ticket = response.ticket
                reduce { copy(stage = RecoveryStage.NewPassword) }
            }
            .onFailure { error ->
                val failure = RecoveryFailure.of(error)
                // Only a refusal counts towards offering support.
                reduce { copy(failures = listOf(failure), refusals = if (failure == RecoveryFailure.IdentityRefused) refusals + 1 else refusals) }
            }
        reduce { copy(isWorking = false) }
    }

    private suspend fun resetPassword() {
        if (current.isWorking) return
        val failures = current.validateNewPassword()
        reduce { copy(failures = failures) }
        if (failures.isNotEmpty()) return

        val ticket = ticket
        if (ticket == null) {
            // The verify step was never completed: go back and earn one.
            reduce { copy(failures = listOf(RecoveryFailure.TicketExpired), stage = RecoveryStage.Identity) }
            return
        }

        reduce { copy(isWorking = true) }
        attempt { api.reset(RecoveryResetRequest(ticket, current.newPassword)) }
            .onSuccess {
                this.ticket = null
                reduce { copy(didReset = true) }
            }
            .onFailure { error ->
                val failure = RecoveryFailure.of(error)
                if (failure == RecoveryFailure.TicketExpired) {
                    this.ticket = null
                    reduce { copy(stage = RecoveryStage.Identity) }
                }
                reduce { copy(failures = listOf(failure)) }
            }
        reduce { copy(isWorking = false) }
    }
}
