package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

data class LessonCompletionState(val content: LoadState<LessonCompletion> = LoadState.Loading) : UiState

sealed interface LessonCompletionIntent : UiIntent {
    /** Submit (or resubmit, with the same idempotency key) the finished lesson. */
    data object Load : LessonCompletionIntent
}

sealed interface LessonCompletionEffect : UiEffect {
    /** The submission landed: haptic and announcement. */
    data class Landed(val completion: LessonCompletion) : LessonCompletionEffect
}
