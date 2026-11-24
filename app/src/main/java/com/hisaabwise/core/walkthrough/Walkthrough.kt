package com.hisaabwise.core.walkthrough

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.hisaabwise.core.data.AppPreferences
import com.hisaabwise.core.model.AppTab
import com.hisaabwise.core.model.AuthEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Elements the guided tour can spotlight. Screens tag them with [Modifier.walkthroughTarget]. */
enum class WalkthroughTarget {
    SpendingCard, AddExpense, AddExpenseFirst, Savings, Tip, Duo,
    ExpensesSummary, WantsBudget, CategoryList,
    LearnPath,
    ReportsHero, ReportsMonth,
    AccountPersonal, AccountLanguage, AccountCurrency, AccountPassword,
}

enum class WalkthroughTrigger { Registered, NewDevice, Replay }

/**
 * One tour step. [slug] keys the copy: `walkthrough_<slug>_title` / `walkthrough_<slug>_body`.
 * A step with no target is drawn centred.
 */
data class WalkthroughStep(
    val slug: String,
    val screen: AppTab,
    val target: WalkthroughTarget?,
    /** If [whenPresent] is on screen, spotlight [instead] (first-run Home has a different add button). */
    val whenPresent: WalkthroughTarget? = null,
    val instead: WalkthroughTarget? = null,
) {
    companion object {
        val all = listOf(
            WalkthroughStep("welcome", AppTab.Home, null),
            WalkthroughStep("spend", AppTab.Home, WalkthroughTarget.SpendingCard),
            WalkthroughStep("add", AppTab.Home, WalkthroughTarget.AddExpense, WalkthroughTarget.AddExpenseFirst, WalkthroughTarget.AddExpenseFirst),
            WalkthroughStep("savings", AppTab.Home, WalkthroughTarget.Savings),
            WalkthroughStep("tip", AppTab.Home, WalkthroughTarget.Tip),
            WalkthroughStep("duo", AppTab.Home, WalkthroughTarget.Duo),
            WalkthroughStep("exSummary", AppTab.Expenses, WalkthroughTarget.ExpensesSummary),
            WalkthroughStep("exBudget", AppTab.Expenses, WalkthroughTarget.WantsBudget),
            WalkthroughStep("exCats", AppTab.Expenses, WalkthroughTarget.CategoryList),
            WalkthroughStep("learn", AppTab.Learn, WalkthroughTarget.LearnPath),
            WalkthroughStep("repHero", AppTab.Reports, WalkthroughTarget.ReportsHero),
            WalkthroughStep("repMonth", AppTab.Reports, WalkthroughTarget.ReportsMonth),
            WalkthroughStep("acPersonal", AppTab.Account, WalkthroughTarget.AccountPersonal),
            WalkthroughStep("acLanguage", AppTab.Account, WalkthroughTarget.AccountLanguage),
            WalkthroughStep("acCurrency", AppTab.Account, WalkthroughTarget.AccountCurrency),
            WalkthroughStep("acPassword", AppTab.Account, WalkthroughTarget.AccountPassword),
            WalkthroughStep("finish", AppTab.Account, null),
        )
    }
}

/** Tour state. Port of iOS `WalkthroughModel`. App-scoped; the shell draws the overlay. */
class WalkthroughModel(private val prefs: AppPreferences, val steps: List<WalkthroughStep> = WalkthroughStep.all) {

    data class State(
        val isPresenting: Boolean = false,
        val index: Int = 0,
        val trigger: WalkthroughTrigger? = null,
        val rects: Map<WalkthroughTarget, Rect> = emptyMap(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun current(state: State = _state.value): WalkthroughStep? =
        if (state.isPresenting) steps.getOrNull(state.index) else null

    fun startIfNeeded(entry: AuthEntry?) {
        if (_state.value.isPresenting) return
        val trigger = when (entry) {
            AuthEntry.Registered -> WalkthroughTrigger.Registered
            AuthEntry.SignedIn -> if (prefs.walkthroughSeen) null else WalkthroughTrigger.NewDevice
            null -> null
        } ?: return
        start(trigger)
    }

    fun start(trigger: WalkthroughTrigger) = _state.update { it.copy(isPresenting = true, index = 0, trigger = trigger) }

    fun next() = _state.update {
        if (!it.isPresenting) it
        else if (it.index >= steps.lastIndex) { prefs.walkthroughSeen = true; it.copy(isPresenting = false, trigger = null) }
        else it.copy(index = it.index + 1)
    }

    fun back() = _state.update { if (it.isPresenting && it.index > 0) it.copy(index = it.index - 1) else it }

    fun skip() = _state.update { prefs.walkthroughSeen = true; it.copy(isPresenting = false, trigger = null) }

    fun report(target: WalkthroughTarget, rect: Rect?) = _state.update {
        if (it.rects[target] == rect) it
        else it.copy(rects = if (rect == null) it.rects - target else it.rects + (target to rect))
    }
}

val LocalWalkthrough = staticCompositionLocalOf<WalkthroughModel?> { null }

/** Tag an element so the guided tour can spotlight it. */
fun Modifier.walkthroughTarget(target: WalkthroughTarget): Modifier = composed {
    val model = LocalWalkthrough.current
    if (model == null) this else onGloballyPositioned { model.report(target, it.boundsInRoot()) }
}
