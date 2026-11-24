package com.hisaabwise.core.mvi

import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.network.ApiError
import kotlinx.coroutines.CancellationException

/**
 * Runs a fetch and turns its outcome into a [LoadState]. The **only** place an [ApiError]
 * becomes a LoadState — port of the iOS `BaseViewModel.load()`.
 *
 * Cancellation is rethrown, never shown.
 */
suspend fun <T> loadState(isEmpty: (T) -> Boolean = { false }, fetch: suspend () -> T): LoadState<T> =
    try {
        val value = fetch()
        if (isEmpty(value)) LoadState.Empty else LoadState.Loaded(value)
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiError) {
        when (e) {
            ApiError.Offline -> LoadState.Offline
            else -> LoadState.Failed(e.errorCode ?: ErrorCode.Unknown)
        }
    } catch (e: Exception) {
        LoadState.Failed(ErrorCode.Unknown)
    }

/** Runs a write; returns the value or the [ApiError] it failed with. */
suspend fun <T> attempt(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiError) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ApiError.MalformedResponse)
    }
