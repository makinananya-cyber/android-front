package com.hisaabwise.core.model

/**
 * What a screen's content is doing. Mirrors the iOS `LoadState`:
 * `loading | empty | offline | failed(code) | loaded(value)`.
 *
 * The only place an [com.hisaabwise.core.network.ApiError] becomes a LoadState is
 * [com.hisaabwise.core.mvi.loadInto] — screens never map errors themselves.
 */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data object Empty : LoadState<Nothing>
    data object Offline : LoadState<Nothing>
    data class Failed(val code: ErrorCode) : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>

    val valueOrNull: T? get() = (this as? Loaded<T>)?.value
    val isOffline: Boolean get() = this is Offline
}

/** Opaque server error code, from `{"error":{"code":"..."}}`. Unknown codes are kept, not rejected. */
@JvmInline
@kotlinx.serialization.Serializable
value class ErrorCode(val raw: String) {
    override fun toString() = raw

    companion object {
        val Unknown = ErrorCode("UNKNOWN")
        val MalformedResponse = ErrorCode("MALFORMED_RESPONSE")
        val Unauthenticated = ErrorCode("UNAUTHENTICATED")
        val RateLimited = ErrorCode("RATE_LIMITED")
        val MonthClosed = ErrorCode("MONTH_CLOSED")
        val AccountPendingDeletion = ErrorCode("ACCOUNT_PENDING_DELETION")
        val InvalidCredentials = ErrorCode("INVALID_CREDENTIALS")
        val SecurityAnswersInvalid = ErrorCode("SECURITY_ANSWERS_INVALID")
        val EmailTaken = ErrorCode("EMAIL_TAKEN")
        val AccountLocked = ErrorCode("ACCOUNT_LOCKED")
        val ResetTicketInvalid = ErrorCode("RESET_TICKET_INVALID")
        val ValidationFailed = ErrorCode("VALIDATION_FAILED")
        val UnderAge = ErrorCode("UNDER_AGE")
        val TermsNotAccepted = ErrorCode("TERMS_NOT_ACCEPTED")
        val Forbidden = ErrorCode("FORBIDDEN")
    }
}
