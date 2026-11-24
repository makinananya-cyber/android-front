package com.hisaabwise.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI building blocks.
 *
 * Every screen is three types and one view model:
 * - **State**: an immutable data class, the single thing the screen renders.
 * - **Intent**: a sealed interface of everything the user (or the screen's lifecycle) can ask for.
 * - **Effect**: one-shot events the screen must act on once (navigate, open a URL, haptic).
 *
 * The screen composable is stateless: `XScreen(state, onIntent)`. A thin `XRoute` collects the
 * view model's [MviViewModel.state] and [MviViewModel.effects] and wires them in.
 *
 * Intents flow one way, into [MviViewModel.dispatch]; state flows one way, out of [MviViewModel.state].
 * Only [MviViewModel.reduce] changes state, so every change is an explicit `copy`.
 */
interface UiState
interface UiIntent
interface UiEffect

/** For screens with no one-shot effects. */
object NoEffect : UiEffect

abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initial: S) : ViewModel() {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    /** The current state, for reading inside [handle]. */
    protected val current: S get() = _state.value

    /** Entry point for every intent. Each one is handled in its own coroutine. */
    fun dispatch(intent: I) {
        viewModelScope.launch { handle(intent) }
    }

    protected abstract suspend fun handle(intent: I)

    /** The only way state changes. */
    protected fun reduce(transform: S.() -> S) = _state.update(transform)

    protected fun emit(effect: E) {
        _effects.trySend(effect)
    }
}
