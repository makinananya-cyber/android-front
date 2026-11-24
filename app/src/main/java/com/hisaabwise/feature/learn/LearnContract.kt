package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/** What just happened, for the toast. The words are the screen's. */
enum class LearnNotice { LessonLocked }

data class LearnState(
    val content: LoadState<LearnMap> = LoadState.Loading,
    /** The open guide sheet, as an **id**, so a reload re-renders it from the new payload. */
    val openGuideUnitId: String? = null,
    val notice: LearnNotice? = null,
) : UiState {
    /** The open unit read out of the **current** payload; null if a reload dropped it. */
    val openGuideUnit: LearnMap.MapUnit?
        get() = openGuideUnitId?.let { id -> content.valueOrNull?.unit(id) }
}

sealed interface LearnIntent : UiIntent {
    data object Load : LearnIntent
    data class OpenGuide(val unitId: String) : LearnIntent
    data object CloseGuide : LearnIntent
    /** A node or guide row was pressed: start it, or say why not. */
    data class PressLesson(val lessonId: String, val isLocked: Boolean) : LearnIntent
    data object DismissNotice : LearnIntent
    /** A write's own response re-renders the map (the completion's `screen`). */
    data class ApplyScreen(val screen: LearnPayload) : LearnIntent
}

sealed interface LearnEffect : UiEffect {
    data class OpenLesson(val lessonId: String) : LearnEffect
}
