package com.hisaabwise.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.TimeZone

private fun deviceTimeZone(): String = TimeZone.getDefault().id

@Serializable
data class SessionTokensWire(val accessToken: String, val refreshToken: String)

@Serializable
data class SignInRequest(val email: String, val password: String, val timeZone: String = deviceTimeZone())

@Serializable
data class RefreshRequest(val refreshToken: String, val timeZone: String = deviceTimeZone())

@Serializable
data class SignOutRequest(val refreshToken: String)

/** `{}` — any 2xx body is accepted. */
@Serializable
class Acknowledgement

@Serializable
data class SessionUser(val email: String, val displayName: String, val emailVerified: Boolean)

/** How the user arrived in the app shell: drives the first-run guided tour. */
enum class AuthEntry { Registered, SignedIn }

@Serializable
data class PhoneNumber(val country: String, val dialCode: String, val national: String, val e164: String = dialCode + national) {
    companion object {
        const val MAX_NATIONAL_DIGITS = 15

        /** ASCII digits only, at most 15 — port of iOS `PhoneNumber.nationalDigits`. */
        fun nationalDigits(text: String): String =
            text.mapNotNull { ch -> Character.digit(ch, 10).takeIf { it in 0..9 }?.digitToChar() }
                .joinToString("").take(MAX_NATIONAL_DIGITS)

        fun of(country: String, dialCode: String, national: String) = PhoneNumber(country, dialCode, national, dialCode + national)
    }
}

@Serializable
data class SecurityAnswer(@SerialName("questionId") val questionId: String, val answer: String)

@Serializable
data class RegistrationRequest(
    val name: String,
    val email: String,
    val dateOfBirth: String,
    val phone: PhoneNumber? = null,
    val password: String,
    val displayCurrency: String,
    val salary: MoneyAmount,
    val savingsGoal: MoneyAmount,
    val goalWasSkipped: Boolean,
    val securityAnswers: List<SecurityAnswer>,
    val acceptedTerms: Boolean,
    val timeZone: String = deviceTimeZone(),
    val language: String,
)
