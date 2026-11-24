package com.hisaabwise.feature.reports

import androidx.lifecycle.viewModelScope
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.Endpoint
import com.hisaabwise.core.session.ScreenRepaint
import kotlinx.coroutines.launch

/** Port of iOS `ReportsViewModel`. An archive with no closed months is still `Loaded`; the page shows the empty note. */
class ReportsViewModel(
    private val api: ApiClient,
    repaint: ScreenRepaint,
) : MviViewModel<ReportsState, ReportsIntent, ReportsEffect>(ReportsState()) {

    init {
        viewModelScope.launch { repaint.requests.collect { dispatch(ReportsIntent.Load) } }
    }

    override suspend fun handle(intent: ReportsIntent) {
        when (intent) {
            ReportsIntent.Load -> {
                reduce { copy(content = LoadState.Loading) }
                val loaded = loadState { api.get<ReportsScreenPayload>(Endpoint.SCREEN_REPORTS) }
                reduce { copy(content = loaded) }
            }
            is ReportsIntent.OpenMonth -> emit(ReportsEffect.OpenMonth(intent.monthKey))
        }
    }
}
