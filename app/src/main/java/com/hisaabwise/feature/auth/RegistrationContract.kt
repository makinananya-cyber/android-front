package com.hisaabwise.feature.auth

import androidx.annotation.StringRes
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.components.ErrorCopy
import com.hisaabwise.core.model.Country
import com.hisaabwise.core.model.Currency
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.PasswordStrength
import com.hisaabwise.core.model.PhoneNumber
import com.hisaabwise.core.model.RegistrationRequest
import com.hisaabwise.core.model.SecurityAnswer
import com.hisaabwise.core.model.SecurityQuestion
import com.hisaabwise.core.model.TypedAmount
import com.hisaabwise.core.model.Validation
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState
import com.hisaabwise.core.network.ApiError
import java.time.LocalDate
import java.time.Period
import java.util.TimeZone

/** Which of the design's three in-place steps the user is on. */
enum class RegistrationStep(val number: Int) {
    One(1), Two(2), Three(3);

    val next: RegistrationStep? get() = entries.getOrNull(ordinal + 1)
    val previous: RegistrationStep? get() = entries.getOrNull(ordinal - 1)

    @get:StringRes val titleFirst: Int get() = when (this) {
        One -> R.string.registration_one_title_first
        Two -> R.string.registration_two_title_first
        Three -> R.string.registration_three_title_first
    }
    @get:StringRes val titleSecond: Int get() = when (this) {
        One -> R.string.registration_one_title_second
        Two -> R.string.registration_two_title_second
        Three -> R.string.registration_three_title_second
    }
    @get:StringRes val subtitle: Int get() = when (this) {
        One -> R.string.registration_one_subtitle
        Two -> R.string.registration_two_subtitle
        Three -> R.string.registration_three_subtitle
    }
    /** "Step 2 of 3", as three whole sentences so a translation keeps its word order. */
    @get:StringRes val label: Int get() = when (this) {
        One -> R.string.registration_one_stepLabel
        Two -> R.string.registration_two_stepLabel
        Three -> R.string.registration_three_stepLabel
    }
    @get:StringRes val backLabel: Int get() = when (this) {
        One, Two -> R.string.registration_back_toDetails
        Three -> R.string.registration_back_toMoney
    }
}

enum class RegistrationField {
    Name, Email, Phone, DateOfBirth, Password, ConfirmPassword, Terms,
    Salary, FirstQuestion, FirstAnswer, SecondQuestion, SecondAnswer,
    Goal,
}

/** Port of iOS `RegistrationFailure`. */
sealed interface RegistrationFailure {
    data object NameMissing : RegistrationFailure
    data object EmailInvalid : RegistrationFailure
    /** Only ever arrives from the submit; sends the user back to step 1. */
    data object EmailTaken : RegistrationFailure
    data class PhoneInvalid(val country: String) : RegistrationFailure
    data object DateOfBirthMissing : RegistrationFailure
    data class TooYoung(val minimumAge: Int) : RegistrationFailure
    data object PasswordTooShort : RegistrationFailure
    data object PasswordsDoNotMatch : RegistrationFailure
    data object TermsNotAccepted : RegistrationFailure
    data object SalaryMissing : RegistrationFailure
    data object FirstQuestionMissing : RegistrationFailure
    data object FirstAnswerMissing : RegistrationFailure
    data object SecondQuestionMissing : RegistrationFailure
    data object SecondQuestionRepeated : RegistrationFailure
    data object SecondAnswerMissing : RegistrationFailure
    /** Only reachable through Submit — Skip sends the suggestion. */
    data object GoalMissing : RegistrationFailure
    data object Unreachable : RegistrationFailure
    data class Refused(val code: ErrorCode) : RegistrationFailure

    val field: RegistrationField?
        get() = when (this) {
            NameMissing -> RegistrationField.Name
            EmailInvalid, EmailTaken -> RegistrationField.Email
            is PhoneInvalid -> RegistrationField.Phone
            DateOfBirthMissing, is TooYoung -> RegistrationField.DateOfBirth
            PasswordTooShort -> RegistrationField.Password
            PasswordsDoNotMatch -> RegistrationField.ConfirmPassword
            TermsNotAccepted -> RegistrationField.Terms
            SalaryMissing -> RegistrationField.Salary
            FirstQuestionMissing -> RegistrationField.FirstQuestion
            FirstAnswerMissing -> RegistrationField.FirstAnswer
            SecondQuestionMissing, SecondQuestionRepeated -> RegistrationField.SecondQuestion
            SecondAnswerMissing -> RegistrationField.SecondAnswer
            GoalMissing -> RegistrationField.Goal
            Unreachable, is Refused -> null
        }

    /** The step the user has to be on to fix it. */
    val step: RegistrationStep
        get() = when (this.field) {
            RegistrationField.Name, RegistrationField.Email, RegistrationField.Phone, RegistrationField.DateOfBirth,
            RegistrationField.Password, RegistrationField.ConfirmPassword, RegistrationField.Terms -> RegistrationStep.One
            RegistrationField.Salary, RegistrationField.FirstQuestion, RegistrationField.FirstAnswer,
            RegistrationField.SecondQuestion, RegistrationField.SecondAnswer -> RegistrationStep.Two
            RegistrationField.Goal, null -> RegistrationStep.Three
        }

    /** The only place a RegistrationFailure becomes words. */
    @get:StringRes
    val copy: Int
        get() = when (this) {
            NameMissing -> R.string.registration_error_nameMissing
            EmailInvalid -> R.string.registration_error_emailInvalid
            EmailTaken -> R.string.registration_error_emailTaken
            is PhoneInvalid -> R.string.registration_error_phoneInvalid
            DateOfBirthMissing -> R.string.registration_error_dateOfBirthMissing
            is TooYoung -> R.string.registration_error_tooYoung
            PasswordTooShort -> R.string.registration_error_passwordTooShort
            PasswordsDoNotMatch -> R.string.registration_error_passwordsDoNotMatch
            TermsNotAccepted -> R.string.registration_error_termsNotAccepted
            SalaryMissing -> R.string.registration_error_salaryMissing
            FirstQuestionMissing -> R.string.registration_error_firstQuestionMissing
            FirstAnswerMissing -> R.string.registration_error_firstAnswerMissing
            SecondQuestionMissing -> R.string.registration_error_secondQuestionMissing
            SecondQuestionRepeated -> R.string.registration_error_secondQuestionRepeated
            SecondAnswerMissing -> R.string.registration_error_secondAnswerMissing
            GoalMissing -> R.string.registration_error_goalMissing
            Unreachable -> R.string.state_offline
            is Refused -> ErrorCopy.message(code)
        }

    companion object {
        fun of(error: Throwable): RegistrationFailure = when (error) {
            ApiError.Offline -> Unreachable
            ApiError.MalformedResponse -> Refused(ErrorCode.MalformedResponse)
            ApiError.Unauthenticated -> Refused(ErrorCode.Unauthenticated)
            // The collision is the only per-field failure registration can receive.
            is ApiError.Server -> if (error.code == ErrorCode.EmailTaken) EmailTaken else Refused(error.code)
            else -> Refused(ErrorCode.Unknown)
        }
    }
}

/** How the typed goal compares with the 20% suggestion. */
enum class GoalComparison { Same, Above, Below }

/**
 * Everything the three steps collect, held until the one atomic `POST /v1/auth/register`
 * (port of iOS `RegistrationViewModel`'s stored properties). The rules are pure members so they
 * can be asserted without a view model.
 */
data class RegistrationState(
    // Step 1
    val name: String = "",
    val email: String = "",
    val country: Country? = null,
    val phoneDigits: String = "",
    val dateOfBirth: LocalDate? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val acceptedTerms: Boolean = false,
    // Step 2
    val currency: Currency? = null,
    val salaryText: String = "",
    val firstQuestion: SecurityQuestion? = null,
    val firstAnswer: String = "",
    val secondQuestion: SecurityQuestion? = null,
    val secondAnswer: String = "",
    // Step 3
    val goalText: String = "",
    /** The box still holds the app's suggestion, so it re-derives when the salary changes. */
    val goalIsSuggestion: Boolean = true,
    val goalWasSkipped: Boolean = false,
    // Reference lists (fetched, never compiled in)
    val countries: List<Country> = emptyList(),
    val currencies: List<Currency> = emptyList(),
    val questions: List<SecurityQuestion> = emptyList(),
    val isLoadingReferenceLists: Boolean = false,
    /** Why the lists are missing — separate from [failures], which are about what was typed. */
    val referenceFailure: RegistrationFailure? = null,
    // Form state
    val step: RegistrationStep = RegistrationStep.One,
    val failures: List<RegistrationFailure> = emptyList(),
    val isSubmitting: Boolean = false,
) : UiState {

    val hasReferenceLists: Boolean get() = currencies.isNotEmpty() && questions.isNotEmpty() && countries.isNotEmpty()

    fun failure(field: RegistrationField): RegistrationFailure? = failures.firstOrNull { it.field == field }
    val formFailure: RegistrationFailure? get() = failures.firstOrNull { it.field == null }

    val passwordStrength: PasswordStrength get() = PasswordStrength.of(password)

    /** Every question except the one the other slot holds. */
    fun questionsExcluding(other: SecurityQuestion?): List<SecurityQuestion> =
        if (other == null) questions else questions.filter { it != other }

    /** The national significant number: digits with any trunk prefix (leading zeros) removed. */
    val phoneSignificantDigits: String get() = PhoneNumber.nationalDigits(phoneDigits).trimStart('0')

    // Salary and goal. Two minor digits: the reference list carries no exponent (as on iOS).
    val salaryMinor: Long? get() = TypedAmount.minor(salaryText, EXPONENT)
    val suggestedGoalMinor: Long? get() = salaryMinor?.let { Math.round(it * SUGGESTED_GOAL_SHARE) }
    val goalMinor: Long? get() = TypedAmount.minor(goalText, EXPONENT)
    val suggestedGoalText: String? get() = suggestedGoalMinor?.let { TypedAmount.major(it, EXPONENT) }

    val goalAgainstSuggestion: GoalComparison?
        get() {
            val goal = goalMinor ?: return null
            val suggested = suggestedGoalMinor ?: return null
            return when {
                goal == suggested -> GoalComparison.Same
                goal > suggested -> GoalComparison.Above
                else -> GoalComparison.Below
            }
        }

    /** Offer "Use the suggested amount" only when the box holds something other than the suggestion. */
    val suggestsUsingTheSuggestion: Boolean get() = goalMinor != null && goalAgainstSuggestion != GoalComparison.Same

    /** The live "% of salary": a ratio between two typed numbers, not money. Latin digits, no grouping. */
    val goalShareText: String?
        get() {
            val goal = goalMinor ?: return null
            val salary = salaryMinor?.takeIf { it > 0 } ?: return null
            return Math.round(goal.toDouble() / salary.toDouble() * 100).toString()
        }

    // Validation

    fun validate(step: RegistrationStep, goalMinor: Long?, today: LocalDate = LocalDate.now()): List<RegistrationFailure> =
        when (step) {
            RegistrationStep.One -> validateStepOne(today)
            RegistrationStep.Two -> validateStepTwo()
            RegistrationStep.Three -> if (goalMinor == null) listOf(RegistrationFailure.GoalMissing) else emptyList()
        }

    private fun validateStepOne(today: LocalDate): List<RegistrationFailure> = buildList {
        if (name.isBlank()) add(RegistrationFailure.NameMissing)
        if (!Validation.isEmail(email)) add(RegistrationFailure.EmailInvalid)
        // Optional at launch — but a number that is given has to be one, counted on what will be sent.
        if (phoneDigits.isNotEmpty() && phoneSignificantDigits.length !in PHONE_DIGIT_RANGE) {
            add(RegistrationFailure.PhoneInvalid(country?.name.orEmpty()))
        }
        val dob = dateOfBirth
        if (dob == null) add(RegistrationFailure.DateOfBirthMissing)
        else if (age(dob, today) < MINIMUM_AGE) add(RegistrationFailure.TooYoung(MINIMUM_AGE))
        if (password.length < Validation.MIN_PASSWORD_LENGTH) add(RegistrationFailure.PasswordTooShort)
        if (confirmPassword != password) add(RegistrationFailure.PasswordsDoNotMatch)
        if (!acceptedTerms) add(RegistrationFailure.TermsNotAccepted)
    }

    private fun validateStepTwo(): List<RegistrationFailure> = buildList {
        if (currency == null || salaryMinor == null) add(RegistrationFailure.SalaryMissing)
        when {
            firstQuestion == null -> add(RegistrationFailure.FirstQuestionMissing)
            firstAnswer.isBlank() -> add(RegistrationFailure.FirstAnswerMissing)
        }
        when {
            secondQuestion == null -> add(RegistrationFailure.SecondQuestionMissing)
            secondQuestion == firstQuestion -> add(RegistrationFailure.SecondQuestionRepeated)
            secondAnswer.isBlank() -> add(RegistrationFailure.SecondAnswerMissing)
        }
    }

    /** Every step's rules against the figure actually being sent. */
    fun validateAll(goalMinor: Long?, today: LocalDate = LocalDate.now()): List<RegistrationFailure> =
        RegistrationStep.entries.flatMap { validate(it, goalMinor, today) }

    /** The body, built once from what all three steps collected. */
    fun request(goalMinor: Long?, language: String, timeZone: String = TimeZone.getDefault().id): RegistrationRequest? {
        val currency = currency ?: return null
        val salary = salaryMinor ?: return null
        val goal = goalMinor ?: return null
        val dob = dateOfBirth ?: return null
        val first = firstQuestion ?: return null
        val second = secondQuestion ?: return null
        return RegistrationRequest(
            name = name.trim(),
            email = email.trim(),
            dateOfBirth = isoDay(dob),
            phone = if (phoneDigits.isEmpty()) null else country?.let { PhoneNumber.of(it.code, it.dialCode, phoneSignificantDigits) },
            password = password,
            displayCurrency = currency.code,
            salary = MoneyAmount(salary, currency.code),
            savingsGoal = MoneyAmount(goal, currency.code),
            goalWasSkipped = goalWasSkipped,
            securityAnswers = listOf(
                SecurityAnswer(first.id, firstAnswer.trim()),
                SecurityAnswer(second.id, secondAnswer.trim()),
            ),
            acceptedTerms = acceptedTerms,
            timeZone = timeZone,
            language = language,
        )
    }

    companion object {
        /** 16+, matching the server's MINIMUM_AGE_YEARS; someone turning 16 today is old enough. */
        const val MINIMUM_AGE = 16
        const val MAXIMUM_AGE = 120
        val PHONE_DIGIT_RANGE = 6..15
        const val SUGGESTED_GOAL_SHARE = 0.20
        const val DEFAULT_COUNTRY_CODE = "AE"
        const val DEFAULT_CURRENCY_CODE = "AED"
        private const val EXPONENT = 2

        /** `YYYY-MM-DD`, the calendar day chosen. */
        fun isoDay(day: LocalDate): String = day.toString()

        /** What the picker offers: no one younger than 16 or older than 120. */
        fun dateOfBirthRange(today: LocalDate = LocalDate.now()): ClosedRange<LocalDate> =
            today.minusYears(MAXIMUM_AGE.toLong())..today.minusYears(MINIMUM_AGE.toLong())

        fun age(birthday: LocalDate, today: LocalDate = LocalDate.now()): Int = Period.between(birthday, today).years
    }
}

sealed interface RegistrationIntent : UiIntent {
    data object LoadReferenceLists : RegistrationIntent
    // Step 1
    data class NameChanged(val value: String) : RegistrationIntent
    data class EmailChanged(val value: String) : RegistrationIntent
    data class CountryChosen(val code: String) : RegistrationIntent
    data class PhoneChanged(val value: String) : RegistrationIntent
    data class DateOfBirthChosen(val value: LocalDate) : RegistrationIntent
    data class PasswordChanged(val value: String) : RegistrationIntent
    data class ConfirmPasswordChanged(val value: String) : RegistrationIntent
    data class TermsChanged(val value: Boolean) : RegistrationIntent
    data object OpenPrivacy : RegistrationIntent
    data object OpenTerms : RegistrationIntent
    // Step 2
    data class CurrencyChosen(val code: String) : RegistrationIntent
    data class SalaryChanged(val value: String) : RegistrationIntent
    data class FirstQuestionChosen(val id: String) : RegistrationIntent
    data class FirstAnswerChanged(val value: String) : RegistrationIntent
    data class SecondQuestionChosen(val id: String) : RegistrationIntent
    data class SecondAnswerChanged(val value: String) : RegistrationIntent
    // Step 3
    data class GoalChanged(val value: String) : RegistrationIntent
    data object UseSuggestedGoal : RegistrationIntent
    // Movement
    data object Advance : RegistrationIntent
    data object GoBack : RegistrationIntent
    data object Submit : RegistrationIntent
    data object SkipGoal : RegistrationIntent
    data object SignInInstead : RegistrationIntent
    data object Leave : RegistrationIntent
}

sealed interface RegistrationEffect : UiEffect {
    data class OpenUrl(val url: String) : RegistrationEffect
    /** Spoken by TalkBack: the new step, or the first failure just produced. */
    data class Announce(@StringRes val text: Int) : RegistrationEffect
    data object GoToSignIn : RegistrationEffect
    data object GoBack : RegistrationEffect
}
