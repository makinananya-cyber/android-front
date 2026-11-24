package com.hisaabwise.core.network

import com.hisaabwise.core.data.TokenStore
import com.hisaabwise.core.model.Acknowledgement
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.RefreshRequest
import com.hisaabwise.core.model.SessionTokensWire
import com.hisaabwise.core.model.SignOutRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

/** One shared JSON configuration for every payload. */
val HWJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    // An unknown enum value falls back to the property's default instead of failing the screen.
    coerceInputValues = true
}

/**
 * The only code that talks HTTP. A port of the iOS `APIClient` actor — behaviour must match:
 *
 * - Headers: `Accept`, `Accept-Language` (the chosen app language), `Content-Type` with a body,
 *   `Idempotency-Key` on every POST, `Authorization: Bearer` on session calls, `If-None-Match` on content.
 * - Before a session call, refresh if the access token is missing or within 60 s of expiry.
 * - On a 401, refresh **once** and retry **once** (the security-epoch case).
 * - Refresh is **single-flight**: concurrent callers share one rotation. The backend revokes the
 *   whole token family if a spent refresh token is presented again, so two refreshes = signed out.
 * - A refused refresh ends the session ([sessionEnded]); offline or 5xx keep it.
 */
class ApiClient(
    baseUrl: String,
    private val transport: HttpTransport,
    private val acceptLanguage: () -> String,
    private var refreshTokens: TokenStore,
) {
    enum class Auth { Session, Anonymous }

    sealed interface ContentResponse {
        data object NotModified : ContentResponse
        class Fetched(val data: ByteArray, val etag: String?) : ContentResponse
    }

    private val base: HttpUrl = baseUrl.trimEnd('/').toHttpUrl()
    private val json = HWJson
    private val lock = Mutex()
    private var accessToken: AccessToken? = null
    private var refreshInFlight: Deferred<AccessToken>? = null

    /** Outlives any one caller: a screen that goes away mid-refresh must not cancel its joiners' refresh. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessionEnded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the server ended the session (not when the user signed out). */
    val sessionEnded: SharedFlow<Unit> = _sessionEnded.asSharedFlow()

    // region Typed API

    suspend inline fun <reified R> get(path: String, auth: Auth = Auth.Session): R =
        perform("GET", path, null, null, auth, serializer<R>())

    suspend inline fun <reified B, reified R> post(
        path: String,
        body: B,
        idempotencyKey: String = UUID.randomUUID().toString(),
        auth: Auth = Auth.Session,
    ): R = perform("POST", path, encode(body, serializer<B>()), idempotencyKey, auth, serializer<R>())

    suspend inline fun <reified B, reified R> put(path: String, body: B): R =
        perform("PUT", path, encode(body, serializer<B>()), null, Auth.Session, serializer<R>())

    suspend inline fun <reified R> delete(path: String): R =
        perform("DELETE", path, null, null, Auth.Session, serializer<R>())

    fun <B> encode(body: B, serializer: KSerializer<B>): ByteArray = json.encodeToString(serializer, body).encodeToByteArray()

    /** Anonymous ETag-revalidated GET for cacheable content. */
    suspend fun content(path: String, ifNoneMatch: String?): ContentResponse {
        val response = respond("GET", path, null, null, null, ifNoneMatch)
        if (response.status == 304) return ContentResponse.NotModified
        if (response.status !in 200..299) throw ApiError.Server(response.status, errorCode(response.body))
        return ContentResponse.Fetched(response.body, response.header("ETag"))
    }

    // endregion

    // region Session

    suspend fun hasSession(): Boolean {
        if (accessToken != null) return true
        return try {
            storedRefreshToken() != null
        } catch (_: ApiError) {
            true // a store that cannot be read is not a store that is empty
        }
    }

    suspend fun beginSession(tokens: SessionTokensWire, store: TokenStore) {
        val token = AccessToken.parse(tokens.accessToken) ?: throw ApiError.MalformedResponse
        lock.withLock {
            runCatching { refreshTokens.clear() }
            refreshTokens = store
            accessToken = token
        }
        runCatching { store.save(tokens.refreshToken) }
    }

    /** User-requested sign-out. Bypasses the 401-retry path so it never signals a forced logout. */
    suspend fun endSession() {
        refreshIfNearExpiry()
        val refreshToken = runCatching { storedRefreshToken() }.getOrNull()
        if (refreshToken != null) {
            runCatching {
                send(
                    "POST", Endpoint.LOGOUT,
                    encode(SignOutRequest(refreshToken), SignOutRequest.serializer()),
                    UUID.randomUUID().toString(), accessToken?.raw, serializer<Acknowledgement>(),
                )
            }
        }
        clearSession(signal = false)
    }

    suspend fun refreshIfNearExpiry() {
        if (accessToken?.isNearExpiry() == false) return
        if (!hasSession()) return
        runCatching { refreshed(presented = accessToken) }
    }

    // endregion

    // region The one request

    suspend fun <R> perform(
        method: String,
        path: String,
        body: ByteArray?,
        idempotencyKey: String?,
        auth: Auth,
        serializer: KSerializer<R>,
    ): R {
        if (auth == Auth.Anonymous) return send(method, path, body, idempotencyKey, null, serializer)
        val presented = sessionToken()
        return try {
            send(method, path, body, idempotencyKey, presented?.raw, serializer)
        } catch (e: ApiError) {
            if (!e.isUnauthorized) throw e
            send(method, path, body, idempotencyKey, refreshed(presented = presented).raw, serializer)
        }
    }

    private suspend fun storedRefreshToken(): String? = try {
        refreshTokens.refreshToken()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        throw ApiError.Offline
    }

    private suspend fun sessionToken(): AccessToken? {
        accessToken?.let { if (!it.isNearExpiry()) return it }
        if (storedRefreshToken() == null) return accessToken
        return refreshed(presented = accessToken)
    }

    private suspend fun refreshed(presented: AccessToken?): AccessToken {
        val task = lock.withLock {
            accessToken?.let { current ->
                if (current.raw != presented?.raw && !current.isNearExpiry()) return current
            }
            refreshInFlight ?: scope.async { rotateTokens() }.also { refreshInFlight = it }
        }
        return try {
            task.await()
        } finally {
            lock.withLock { if (refreshInFlight === task && task.isCompleted) refreshInFlight = null }
        }
    }

    private suspend fun rotateTokens(): AccessToken {
        val stored = storedRefreshToken()
        if (stored == null) {
            clearSession(signal = true)
            throw ApiError.Unauthenticated
        }
        val rotated = try {
            send(
                "POST", Endpoint.REFRESH,
                encode(RefreshRequest(stored), RefreshRequest.serializer()),
                UUID.randomUUID().toString(), null, SessionTokensWire.serializer(),
            )
        } catch (e: ApiError) {
            if (e.isUnauthorized) {
                clearSession(signal = true)
                throw ApiError.Unauthenticated
            }
            throw e // offline / 5xx keep the session
        }
        val token = AccessToken.parse(rotated.accessToken) ?: throw ApiError.MalformedResponse
        lock.withLock { accessToken = token }
        runCatching { refreshTokens.save(rotated.refreshToken) }
        return token
    }

    private suspend fun clearSession(signal: Boolean) {
        lock.withLock {
            accessToken = null
            refreshInFlight = null
        }
        runCatching { refreshTokens.clear() }
        if (signal) _sessionEnded.tryEmit(Unit)
    }

    private suspend fun <R> send(
        method: String,
        path: String,
        body: ByteArray?,
        idempotencyKey: String?,
        bearer: String?,
        serializer: KSerializer<R>,
    ): R {
        val response = respond(method, path, body, idempotencyKey, bearer, null)
        if (response.status !in 200..299) throw ApiError.Server(response.status, errorCode(response.body))
        return try {
            json.decodeFromString(serializer, response.body.decodeToString().ifEmpty { "{}" })
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            throw ApiError.MalformedResponse
        }
    }

    private suspend fun respond(
        method: String,
        path: String,
        body: ByteArray?,
        idempotencyKey: String?,
        bearer: String?,
        ifNoneMatch: String?,
    ): HttpResponse {
        val url = base.newBuilder().encodedPath(path).build()
        val builder = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("Accept-Language", acceptLanguage())
        val requestBody = body?.toRequestBody(JSON_MEDIA)
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete(requestBody)
            else -> builder.method(method, requestBody ?: ByteArray(0).toRequestBody(JSON_MEDIA))
        }
        idempotencyKey?.let { builder.header("Idempotency-Key", it) }
        bearer?.let { builder.header("Authorization", "Bearer $it") }
        ifNoneMatch?.let { builder.header("If-None-Match", it) }

        NetworkLogger.request(method, url.toString(), body?.decodeToString())
        val started = System.currentTimeMillis()
        return try {
            transport.send(builder.build()).also {
                NetworkLogger.response(method, url.toString(), it.status, System.currentTimeMillis() - started, it.body.decodeToString())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            NetworkLogger.failure(method, url.toString(), e)
            throw ApiError.Offline
        }
    }

    @Serializable
    private data class Envelope(val error: Failure) {
        @Serializable
        data class Failure(val code: String)
    }

    private fun errorCode(body: ByteArray): ErrorCode = try {
        ErrorCode(json.decodeFromString(Envelope.serializer(), body.decodeToString()).error.code)
    } catch (_: Exception) {
        ErrorCode.Unknown
    }

    private companion object {
        val JSON_MEDIA = "application/json".toMediaType()
    }

    // endregion
}
