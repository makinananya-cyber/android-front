package com.hisaabwise.feature.home

import androidx.lifecycle.viewModelScope
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.Tip
import com.hisaabwise.core.model.TipList
import com.hisaabwise.core.model.TypedAmount
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.attempt
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.ContentLoader
import com.hisaabwise.core.network.Endpoint
import com.hisaabwise.core.session.ScreenRepaint
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * Home: one request (`GET /v1/screens/home`) and nothing derived from it. Port of iOS `HomeViewModel`.
 * The tip pool is fetched on the first **Show me another** and cycled in memory.
 */
class HomeViewModel(
    private val api: ApiClient,
    private val content: ContentLoader,
    repaint: ScreenRepaint,
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    /** The cacheable tip pool, once fetched. Not state: nothing renders it directly. */
    private var pool: List<Tip> = emptyList()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch { repaint.requests.collect { dispatch(HomeIntent.Load) } }
    }

    override suspend fun handle(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Load -> load()
            is HomeIntent.Isolate -> reduce { copy(isolated = HomeLogic.toggleIsolation(isolated, intent.id)) }
            HomeIntent.ShowAnotherTip -> showAnotherTip()
            HomeIntent.RaiseGoal -> raiseGoal()
            HomeIntent.DismissGoalNudge -> reduce { copy(hasDismissedGoalNudge = true) }
            HomeIntent.BeginEditingGoal -> {
                val goal = current.content.valueOrNull?.savings?.goal ?: return
                // Empty for a zero goal, so the caret does not sit after a "0".
                val draft = if (goal.minor == 0L) "" else TypedAmount.major(goal.minor, goal.exponent)
                reduce { copy(goalDraft = draft, isEditingGoal = true) }
            }
            is HomeIntent.EditGoalAmount -> reduce { copy(goalDraft = intent.text) }
            HomeIntent.CloseEditGoal -> reduce { copy(isEditingGoal = false) }
            HomeIntent.SaveGoal -> saveGoal()
            HomeIntent.AddExpense -> emit(HomeEffect.GoToExpenses)
            HomeIntent.ContinueLearning -> emit(HomeEffect.GoToLearn)
            is HomeIntent.OpenArticle -> emit(HomeEffect.OpenArticle(intent.teaser))
        }
    }

    private suspend fun load() {
        loadJob?.cancel()
        loadJob = currentCoroutineContext().job
        reduce { copy(content = LoadState.Loading) }
        val loaded = loadState { api.get<HomeScreenPayload>(Endpoint.SCREEN_HOME) }
        reduce {
            val screen = loaded.valueOrNull
            copy(content = loaded, isolated = if (screen != null) HomeLogic.reconcileIsolation(isolated, screen) else isolated)
        }
    }

    /** A pool that will not load leaves the day's tip on screen and reports nothing. */
    private suspend fun showAnotherTip() {
        val screen = current.content.valueOrNull ?: return
        if (pool.isEmpty()) {
            pool = attempt { content.load<TipList>(ContentResource.Tips) }.getOrNull()?.tips.orEmpty()
        }
        val showing = current.substitutedTip?.id ?: screen.tip.id
        val next = HomeLogic.nextTip(pool, showing) ?: return
        reduce { copy(substitutedTip = next) }
    }

    /** Takes the server's suggested goal and sends that same figure back. A failure is silent. */
    private suspend fun raiseGoal() {
        if (current.isRaisingGoal) return
        val suggested = current.content.valueOrNull?.savings?.goalNudge?.suggested ?: return
        putGoal(MoneyAmount(suggested.minor, suggested.currency))
    }

    /** Sets the goal by hand. Zero is allowed (clearing the goal); an empty box reads as 0. */
    private suspend fun saveGoal() {
        if (current.isRaisingGoal) return
        val goal = current.content.valueOrNull?.savings?.goal ?: return
        reduce { copy(isEditingGoal = false) }
        val minor = TypedAmount.scaled(current.goalDraft, goal.exponent) ?: 0L
        putGoal(MoneyAmount(minor, goal.currency))
    }

    private suspend fun putGoal(amount: MoneyAmount) {
        reduce { copy(isRaisingGoal = true) }
        val result = attempt { api.put<SavingsGoalUpdate, JsonObject>(Endpoint.GOAL, SavingsGoalUpdate(amount)) }
        reduce { copy(isRaisingGoal = false) }
        // The goal is on every card that mentions it, so the screen is re-read rather than patched.
        if (result.isSuccess) load()
    }
}
