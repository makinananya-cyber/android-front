package com.hisaabwise.feature.reports

import androidx.lifecycle.viewModelScope
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.Endpoint
import com.hisaabwise.core.session.ScreenRepaint
import kotlinx.coroutines.launch

/** Port of iOS `ReportsMonthViewModel`. The month re-reads itself from the server by its key. */
class ReportsMonthViewModel(
    monthKey: String,
    private val api: ApiClient,
    repaint: ScreenRepaint,
) : MviViewModel<ReportsMonthState, ReportsMonthIntent, ReportsMonthEffect>(ReportsMonthState(monthKey)) {

    init {
        dispatch(ReportsMonthIntent.Load)
        viewModelScope.launch { repaint.requests.collect { dispatch(ReportsMonthIntent.Load) } }
    }

    override suspend fun handle(intent: ReportsMonthIntent) {
        when (intent) {
            ReportsMonthIntent.Load -> {
                reduce { copy(content = LoadState.Loading) }
                val loaded = loadState { api.get<ReportsMonthPayload>(Endpoint.screenReportsMonth(current.monthKey)) }
                reduce {
                    val screen = loaded.valueOrNull
                    copy(
                        content = loaded,
                        isolated = isolated.takeIf { id -> screen == null || screen.spending.categories.any { it.id == id } },
                        expanded = expanded.takeIf { id -> screen == null || screen.groups.any { it.id == id } },
                    )
                }
            }
            is ReportsMonthIntent.Isolate -> reduce { copy(isolated = if (isolated == intent.id) null else intent.id) }
            is ReportsMonthIntent.Toggle -> reduce { copy(expanded = if (expanded == intent.groupId) null else intent.groupId) }
            ReportsMonthIntent.Back -> emit(ReportsMonthEffect.Back)
        }
    }
}
