package com.hisaabwise.feature.home

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.Tip
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/**
 * Home's presentation state. Everything money-shaped is inside [content], from the server; the rest is
 * which slice is isolated, which tip is showing, and the goal sheet.
 */
data class HomeState(
    val content: LoadState<HomeScreenPayload> = LoadState.Loading,
    /** The isolated slice's id (an id, so it survives a refresh that reorders categories). */
    val isolated: String? = null,
    /** A tip the user asked for with **Show me another**; null while it is the day's own. In memory only. */
    val substitutedTip: Tip? = null,
    /** "Not now" on the goal nudge — for this run of the app only. */
    val hasDismissedGoalNudge: Boolean = false,
    /** True while `PUT /v1/me/goal` is in flight. */
    val isRaisingGoal: Boolean = false,
    val isEditingGoal: Boolean = false,
    /** The goal being typed, in major units, as typed. */
    val goalDraft: String = "",
) : UiState

sealed interface HomeIntent : UiIntent {
    /** The single request. Also the retry. */
    data object Load : HomeIntent
    data class Isolate(val id: String?) : HomeIntent
    data object ShowAnotherTip : HomeIntent
    data object RaiseGoal : HomeIntent
    data object DismissGoalNudge : HomeIntent
    data object BeginEditingGoal : HomeIntent
    data class EditGoalAmount(val text: String) : HomeIntent
    data object CloseEditGoal : HomeIntent
    data object SaveGoal : HomeIntent
    data object AddExpense : HomeIntent
    data object ContinueLearning : HomeIntent
    data class OpenArticle(val teaser: HomeScreenPayload.ArticleTeaser) : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object GoToExpenses : HomeEffect
    data object GoToLearn : HomeEffect
    data class OpenArticle(val teaser: HomeScreenPayload.ArticleTeaser) : HomeEffect
}
