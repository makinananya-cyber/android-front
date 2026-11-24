package com.hisaabwise.core.network

import com.hisaabwise.core.model.ErrorCode

/**
 * Every failure the API client reports. Port of iOS `APIError`.
 * - [Offline]: the request never got an HTTP answer. Never a reason to sign anyone out.
 * - [Server]: a non-2xx answer, with the code from the `{"error":{"code"}}` envelope.
 * - [MalformedResponse]: a 2xx body that would not decode.
 * - [Unauthenticated]: the session is over (refresh refused or no refresh token).
 */
sealed class ApiError(message: String) : Exception(message) {
    data object Offline : ApiError("offline")
    data class Server(val status: Int, val code: ErrorCode) : ApiError("server $status $code")
    data object MalformedResponse : ApiError("malformed response")
    data object Unauthenticated : ApiError("unauthenticated")

    val errorCode: ErrorCode?
        get() = when (this) {
            Offline -> null
            is Server -> code
            MalformedResponse -> ErrorCode.MalformedResponse
            Unauthenticated -> ErrorCode.Unauthenticated
        }

    val isUnauthorized: Boolean get() = this is Server && status == 401
}
