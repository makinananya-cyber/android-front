package com.hisaabwise.feature.learn

import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/** A finished run, ready to submit. The key is fixed for this completion and reused across retries. */
data class CompletionRequest(val lessonId: String, val results: List<QuestionResult>, val idempotencyKey: String)

data class LessonPlayerState(
    /** Taken once when the lesson opened; a reload behind the player never restarts the run. */
    val material: LearnMap.Material,
    val run: LessonRun,
    /** The combo just landed, for the badge and the announcement. */
    val comboMilestone: Int? = null,
    /** Set once the last step is behind the reader; the completion screen then covers the player. */
    val completion: CompletionRequest? = null,
) : UiState {
    fun resolved(text: String): String = material.currencyToken.resolve(text)

    val typedAnswer: String get() = (run.answer as? LessonRun.Answer.Typed)?.text.orEmpty()
}

sealed interface LessonPlayerIntent : UiIntent {
    data class Choose(val option: Int) : LessonPlayerIntent
    data class Type(val text: String) : LessonPlayerIntent
    /** The one button: Continue / Check / Got it. */
    data object Primary : LessonPlayerIntent
    /** Try again from the out-of-hearts dialog. */
    data object Restart : LessonPlayerIntent
    data object DismissCombo : LessonPlayerIntent
}

sealed interface LessonPlayerEffect : UiEffect {
    /** A question was graded — haptic. */
    data class Graded(val isCorrect: Boolean) : LessonPlayerEffect
}
