package com.hisaabwise.feature.auth

import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/**
 * Landing makes no request, so it has no LoadState: its only state is which strapline is showing
 * (port of iOS `LandingViewModel`). It holds an index, not the words; the screen owns the copy.
 */
data class LandingState(val strapline: Int = 0, val straplineCount: Int = STRAPLINE_COUNT) : UiState {
    companion object {
        const val STRAPLINE_COUNT = 4
        /** The design's `setInterval(…, 3800)`. */
        const val ROTATION_MILLIS = 3800L
    }
}

sealed interface LandingIntent : UiIntent {
    /** The next strapline, wrapping. Sent by the screen's clock, never under Reduce Motion. */
    data object Advance : LandingIntent
    data object GetStarted : LandingIntent
}

sealed interface LandingEffect : UiEffect {
    data object GoToSignIn : LandingEffect
}

/** Pure rotation rule, shared by the view model and its test. */
internal fun LandingState.advanced(): LandingState =
    copy(strapline = (strapline + 1) % straplineCount.coerceAtLeast(1))
