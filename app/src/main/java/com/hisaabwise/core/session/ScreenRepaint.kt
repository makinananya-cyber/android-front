package com.hisaabwise.core.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * After a display-currency or language change every money-bearing screen must reload
 * (iOS `TabViewModels.repaintEveryScreen`). Tab view models collect [requests] and reload.
 */
class ScreenRepaint {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    fun repaintEveryScreen() {
        _requests.tryEmit(Unit)
    }
}
