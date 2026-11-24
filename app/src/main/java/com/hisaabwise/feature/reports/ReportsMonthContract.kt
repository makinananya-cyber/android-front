package com.hisaabwise.feature.reports

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/**
 * One closed month. [isolated] is the donut slice in focus; [expanded] is the one open accordion group
 * (one at a time). Both are presentation state and are cleared when a reload no longer contains them.
 */
data class ReportsMonthState(
    val monthKey: String,
    val content: LoadState<ReportsMonthPayload> = LoadState.Loading,
    val isolated: String? = null,
    val expanded: String? = null,
) : UiState {
    val isolatedCategory: ReportsMonthPayload.Category?
        get() = isolated?.let { id -> content.valueOrNull?.spending?.categories?.firstOrNull { it.id == id } }

    fun isExpanded(groupId: String) = expanded == groupId
}

sealed interface ReportsMonthIntent : UiIntent {
    data object Load : ReportsMonthIntent
    /** Toggles: isolating the slice already isolated (or `null`) clears it. */
    data class Isolate(val id: String?) : ReportsMonthIntent
    data class Toggle(val groupId: String) : ReportsMonthIntent
    data object Back : ReportsMonthIntent
}

sealed interface ReportsMonthEffect : UiEffect {
    data object Back : ReportsMonthEffect
}
