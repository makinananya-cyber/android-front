package com.hisaabwise.feature.reports

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/** The archive: closed months grouped by year, plus the savings-goal trend. */
data class ReportsState(val content: LoadState<ReportsScreenPayload> = LoadState.Loading) : UiState

sealed interface ReportsIntent : UiIntent {
    /** Runs on every appearance (iOS `.task { load() }`) and on retry. */
    data object Load : ReportsIntent
    data class OpenMonth(val monthKey: String) : ReportsIntent
}

sealed interface ReportsEffect : UiEffect {
    data class OpenMonth(val monthKey: String) : ReportsEffect
}
