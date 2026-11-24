package com.hisaabwise.feature.auth

import com.hisaabwise.core.model.Country
import com.hisaabwise.core.model.Currency
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.SecurityQuestion
import com.hisaabwise.core.network.ApiError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AuthRulesTest {

    // Landing

    @Test
    fun straplineRotationWraps() {
        var state = LandingState()
        repeat(4) { state = state.advanced() }
        assertEquals(0, state.strapline)
        assertEquals(1, LandingState().advanced().strapline)
    }

    // Sign in

    @Test
    fun signInValidatesEmptinessAndEightCharacters() {
        assertEquals(listOf(SignInFailure.EmailMissing, SignInFailure.PasswordMissing), SignInState(email = "  ").validate())
        assertEquals(listOf(SignInFailure.PasswordTooShort), SignInState(email = "x", password = "1234567").validate())
        assertTrue(SignInState(email = "not-an-address", password = "12345678").validate().isEmpty())
    }

    @Test
    fun signInRefusalsAllReadTheSame() {
        assertEquals(SignInFailure.CredentialsRefused, SignInFailure.of(ApiError.Server(401, ErrorCode.InvalidCredentials)))
        // A status that is not a known fault is a refusal, so no code can become an address checker.
        assertEquals(SignInFailure.CredentialsRefused, SignInFailure.of(ApiError.Server(404, ErrorCode("NO_SUCH_ACCOUNT"))))
        assertEquals(SignInFailure.CredentialsRefused, SignInFailure.of(ApiError.Unauthenticated))
        assertEquals(SignInFailure.PendingDeletion, SignInFailure.of(ApiError.Server(403, ErrorCode.AccountPendingDeletion)))
        assertEquals(SignInFailure.Refused(ErrorCode.RateLimited), SignInFailure.of(ApiError.Server(429, ErrorCode.RateLimited)))
        assertEquals(SignInFailure.Refused(ErrorCode("INTERNAL")), SignInFailure.of(ApiError.Server(500, ErrorCode("INTERNAL"))))
        assertEquals(SignInFailure.Unreachable, SignInFailure.of(ApiError.Offline))
        assertEquals(SignInField.Password, SignInFailure.CredentialsRefused.field)
        assertTrue(SignInState(refusals = 3).suggestsSupport)
    }

    // Registration

    private val today = LocalDate.of(2026, 11, 25)
    private val q1 = SecurityQuestion("sq01", "Pet?")
    private val q2 = SecurityQuestion("sq02", "City?")

    private fun filled() = RegistrationState(
        name = "Neeraj", email = "n@example.ae",
        country = Country("AE", "+971", "United Arab Emirates"), phoneDigits = "0501234567",
        dateOfBirth = LocalDate.of(1990, 3, 12), password = "a-long-password", confirmPassword = "a-long-password",
        acceptedTerms = true,
        currency = Currency("AED", "UAE Dirham", "د.إ"), salaryText = "8,000",
        firstQuestion = q1, firstAnswer = " Rex ", secondQuestion = q2, secondAnswer = "Dubai",
        questions = listOf(q1, q2),
    )

    @Test
    fun suggestedGoalIsTwentyPercentAndTheShareIsLive() {
        val state = filled()
        assertEquals(800_000L, state.salaryMinor)
        assertEquals(160_000L, state.suggestedGoalMinor)
        assertEquals("1600", state.suggestedGoalText)
        assertEquals(GoalComparison.Same, state.copy(goalText = "1600").goalAgainstSuggestion)
        assertFalse(state.copy(goalText = "1600").suggestsUsingTheSuggestion)
        assertEquals(GoalComparison.Above, state.copy(goalText = "2500").goalAgainstSuggestion)
        assertEquals("31", state.copy(goalText = "2500").goalShareText)
        assertEquals(GoalComparison.Below, state.copy(goalText = "500").goalAgainstSuggestion)
        assertTrue(state.copy(goalText = "500").suggestsUsingTheSuggestion)
        assertNull(state.copy(goalText = "").goalShareText)
    }

    @Test
    fun ageIsSixteenOnTheBirthdayItself() {
        val ok = filled().copy(dateOfBirth = today.minusYears(16))
        assertTrue(ok.validate(RegistrationStep.One, null, today).isEmpty())
        val young = filled().copy(dateOfBirth = today.minusYears(16).plusDays(1))
        assertEquals(listOf(RegistrationFailure.TooYoung(16)), young.validate(RegistrationStep.One, null, today))
        val range = RegistrationState.dateOfBirthRange(today)
        assertEquals(LocalDate.of(2010, 11, 25), range.endInclusive)
        assertEquals(LocalDate.of(1906, 11, 25), range.start)
    }

    @Test
    fun phoneDropsTheTrunkPrefixAndIsOptional() {
        assertEquals("501234567", filled().phoneSignificantDigits)
        assertTrue(filled().copy(phoneDigits = "").validate(RegistrationStep.One, null, today).isEmpty())
        val short = filled().copy(phoneDigits = "0012")
        assertEquals(RegistrationField.Phone, short.validate(RegistrationStep.One, null, today).single().field)
    }

    @Test
    fun stepTwoRequiresTwoDifferentAnsweredQuestions() {
        val state = filled()
        assertTrue(state.validate(RegistrationStep.Two, null).isEmpty())
        assertEquals(listOf(RegistrationFailure.SecondQuestionRepeated), state.copy(secondQuestion = q1).validate(RegistrationStep.Two, null))
        assertEquals(listOf(RegistrationFailure.SecondAnswerMissing), state.copy(secondAnswer = " ").validate(RegistrationStep.Two, null))
        assertEquals(listOf(q2), state.questionsExcluding(q1))
        assertEquals(RegistrationField.FirstAnswer, RegistrationFailure.FirstAnswerMissing.field)
    }

    @Test
    fun theRequestCarriesTrimmedAnswersAndTheSignificantPhone() {
        val request = requireNotNull(filled().request(160_000, "en", "Asia/Dubai"))
        assertEquals("1990-03-12", request.dateOfBirth)
        assertEquals("+971501234567", request.phone?.e164)
        assertEquals("Rex", request.securityAnswers.first().answer)
        assertEquals(160_000L, request.savingsGoal.minor)
        assertNull(filled().copy(phoneDigits = "").request(1, "en")!!.phone)
        assertNull(filled().request(null, "en"))
    }

    @Test
    fun emailTakenBelongsToStepOne() {
        val failure = RegistrationFailure.of(ApiError.Server(409, ErrorCode.EmailTaken))
        assertEquals(RegistrationFailure.EmailTaken, failure)
        assertEquals(RegistrationStep.One, failure.step)
        assertEquals(RegistrationStep.Three, RegistrationFailure.of(ApiError.Offline).step)
        assertEquals(RegistrationStep.Three, RegistrationFailure.GoalMissing.step)
    }

    // Recovery

    @Test
    fun recoveryNeverSaysWhichFactorMissed() {
        assertEquals(RecoveryFailure.IdentityRefused, RecoveryFailure.of(ApiError.Server(401, ErrorCode.SecurityAnswersInvalid)))
        assertEquals(RecoveryFailure.IdentityRefused, RecoveryFailure.of(ApiError.Server(404, ErrorCode("NOPE"))))
        assertEquals(RecoveryFailure.Locked, RecoveryFailure.of(ApiError.Server(423, ErrorCode.AccountLocked)))
        assertEquals(RecoveryFailure.TicketExpired, RecoveryFailure.of(ApiError.Server(400, ErrorCode.ResetTicketInvalid)))
        assertEquals(RecoveryFailure.Refused(ErrorCode("BAD")), RecoveryFailure.of(ApiError.Server(503, ErrorCode("BAD"))))
    }

    @Test
    fun recoveryValidatesEveryAnswerAndThePasswordPair() {
        val state = ForgotPasswordState(answers = listOf("Rex", " "))
        assertEquals(listOf(RecoveryFailure.AnswerMissing, RecoveryFailure.DateOfBirthMissing), state.validateIdentity())
        assertEquals(listOf(RecoveryFailure.PasswordTooShort), ForgotPasswordState(newPassword = "short").validateNewPassword())
        assertEquals(listOf(RecoveryFailure.PasswordsDiffer), ForgotPasswordState(newPassword = "12345678", confirmPassword = "1234567x").validateNewPassword())
    }
}
