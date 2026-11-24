package com.hisaabwise.feature.auth

import kotlinx.serialization.Serializable

// Wire payloads for the recovery routes (port of the private structs in iOS `ForgotPasswordViewModel`).
// Sign-in and registration bodies live in core/model/Session.kt because SessionCoordinator sends them.

@Serializable
data class RecoveryQuestionsRequest(val email: String)

@Serializable
data class RecoverySubmittedAnswer(val questionId: String, val answer: String)

@Serializable
data class RecoveryVerifyRequest(val email: String, val dateOfBirth: String, val answers: List<RecoverySubmittedAnswer>)

/** The single-use ticket the identity check issues. Never rendered, never persisted. */
@Serializable
data class RecoveryTicket(val ticket: String, val expiresInSeconds: Int? = null)

@Serializable
data class RecoveryResetRequest(val ticket: String, val newPassword: String)

/**
 * Shared reading of an error from the anonymous auth routes: 5xx, 429 and 400 are faults the user
 * cannot cause; every other status is a refusal, so a hole can never turn a form into an
 * account-existence oracle. Port of the iOS `isFault(status:)` that sign-in and recovery share.
 */
internal object AuthErrors {
    fun isFault(status: Int): Boolean = status >= 500 || status == 429 || status == 400
}
