package com.hisaabwise.core.session

import com.hisaabwise.core.data.TokenStore
import com.hisaabwise.core.model.AuthEntry
import com.hisaabwise.core.model.RegistrationRequest
import com.hisaabwise.core.model.SessionTokensWire
import com.hisaabwise.core.model.SessionUser
import com.hisaabwise.core.model.SignInRequest
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.Endpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * App-wide session state. Port of iOS `SessionCoordinator`.
 *
 * Sign-in stores the refresh token in the Keystore when "Keep me signed in" is on, in memory
 * otherwise; registration always keeps it. [state]'s [SessionState.generation] bumps every time a
 * session ends, so everything scoped to the signed-in shell (the tab view models) is rebuilt and no
 * previous user's data survives.
 */
class SessionCoordinator(
    private val client: ApiClient,
    private val keptStore: TokenStore,
    private val transientStore: TokenStore,
    scope: CoroutineScope,
) {
    data class SessionState(
        val isSignedIn: Boolean = false,
        val user: SessionUser? = null,
        val lastAuthEntry: AuthEntry? = null,
        val generation: Int = 0,
    )

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        scope.launch { client.sessionEnded.collect { sessionDidEnd() } }
    }

    /**
     * Resumes a kept session at launch. The iOS app deliberately does not call this today
     * (every launch lands on Landing); kept here so the behaviour can be switched on in one place.
     */
    suspend fun restore() {
        if (!client.hasSession()) return
        _state.update { it.copy(isSignedIn = true) }
        revalidate()
    }

    suspend fun signIn(email: String, password: String, keepMeSignedIn: Boolean) {
        val tokens = client.post<SignInRequest, SessionTokensWire>(
            Endpoint.LOGIN, SignInRequest(email.trim(), password), auth = ApiClient.Auth.Anonymous,
        )
        client.beginSession(tokens, if (keepMeSignedIn) keptStore else transientStore)
        _state.update { it.copy(isSignedIn = true, lastAuthEntry = AuthEntry.SignedIn) }
        revalidate()
    }

    suspend fun register(request: RegistrationRequest) {
        val tokens = client.post<RegistrationRequest, SessionTokensWire>(
            Endpoint.REGISTER, request, auth = ApiClient.Auth.Anonymous,
        )
        client.beginSession(tokens, keptStore)
        _state.update { it.copy(isSignedIn = true, lastAuthEntry = AuthEntry.Registered) }
        revalidate()
    }

    /** The only user-initiated sign-out. */
    suspend fun signOut() {
        client.endSession()
        sessionDidEnd()
    }

    /** On every return to the foreground: refresh if near expiry, then revalidate. */
    suspend fun onForeground() {
        if (!_state.value.isSignedIn) return
        client.refreshIfNearExpiry()
        revalidate()
    }

    private suspend fun revalidate() {
        runCatching { client.get<SessionUser>(Endpoint.ME) }
            .onSuccess { user -> _state.update { it.copy(user = user) } }
        if (!client.hasSession()) sessionDidEnd()
    }

    private fun sessionDidEnd() {
        _state.update {
            if (!it.isSignedIn && it.user == null) it
            else SessionState(generation = it.generation + 1)
        }
    }
}
