package com.hisaabwise.core.network

import com.hisaabwise.core.data.InMemoryTokenStore
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.SessionTokensWire
import com.hisaabwise.core.model.SessionUser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

/**
 * The session rules the backend depends on: single-flight refresh, one retry on 401,
 * idempotency keys on every POST, offline never ends a session.
 */
class ApiClientTest {

    private fun jwt(exp: Long): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"HS256"}""".toByteArray())
        val payload = enc.encodeToString("""{"exp":$exp,"sec":1}""".toByteArray())
        return "$header.$payload.sig"
    }

    private val fresh get() = jwt(System.currentTimeMillis() / 1000 + 900)
    private val expired get() = jwt(System.currentTimeMillis() / 1000 - 10)

    private class Recorder(val respond: suspend (Request) -> HttpResponse) : HttpTransport {
        val requests = mutableListOf<Request>()
        override suspend fun send(request: Request): HttpResponse {
            synchronized(requests) { requests += request }
            return respond(request)
        }
    }

    private fun ok(body: String) = HttpResponse(200, body.toByteArray(), emptyMap())
    private val me = """{"email":"a@b.co","displayName":"A","emailVerified":true}"""

    @Test fun concurrentCallsShareOneRefresh() = runBlocking {
        val refreshes = AtomicInteger()
        val transport = Recorder { req ->
            if (req.url.encodedPath == Endpoint.REFRESH) {
                refreshes.incrementAndGet()
                kotlinx.coroutines.delay(50)
                ok("""{"accessToken":"$fresh","refreshToken":"r2"}""")
            } else ok(me)
        }
        val store = InMemoryTokenStore().apply { save("r1") }
        val client = ApiClient("https://api.test", transport, { "en" }, store)
        client.beginSession(SessionTokensWire(expired, "r1"), store)
        (1..5).map { async { client.get<SessionUser>(Endpoint.ME) } }.awaitAll()
        assertEquals(1, refreshes.get())
        assertEquals("r2", store.refreshToken())
    }

    @Test fun a401RefreshesOnceAndRetriesOnce() = runBlocking {
        var meCalls = 0
        val transport = Recorder { req ->
            when (req.url.encodedPath) {
                Endpoint.REFRESH -> ok("""{"accessToken":"$fresh","refreshToken":"r2"}""")
                else -> if (meCalls++ == 0) HttpResponse(401, """{"error":{"code":"UNAUTHENTICATED"}}""".toByteArray(), emptyMap()) else ok(me)
            }
        }
        val store = InMemoryTokenStore()
        val client = ApiClient("https://api.test", transport, { "ar" }, store)
        client.beginSession(SessionTokensWire(fresh, "r1"), store)
        client.get<SessionUser>(Endpoint.ME)
        assertEquals(listOf(Endpoint.ME, Endpoint.REFRESH, Endpoint.ME), transport.requests.map { it.url.encodedPath })
        assertEquals("ar", transport.requests.first().header("Accept-Language"))
    }

    @Test fun everyPostCarriesAnIdempotencyKey() = runBlocking {
        val transport = Recorder { ok("{}") }
        val client = ApiClient("https://api.test", transport, { "en" }, InMemoryTokenStore())
        client.post<Map<String, String>, kotlinx.serialization.json.JsonObject>("/v1/events", mapOf("a" to "b"), auth = ApiClient.Auth.Anonymous)
        assertTrue(transport.requests.single().header("Idempotency-Key")!!.isNotBlank())
    }

    @Test fun refusedRefreshEndsTheSessionButOfflineDoesNot() = runBlocking {
        var offline = true
        val transport = Recorder { req ->
            if (offline) throw IOException("no network")
            if (req.url.encodedPath == Endpoint.REFRESH) HttpResponse(401, "{}".toByteArray(), emptyMap()) else ok(me)
        }
        val store = InMemoryTokenStore()
        val client = ApiClient("https://api.test", transport, { "en" }, store)
        client.beginSession(SessionTokensWire(expired, "r1"), store)

        try { client.get<SessionUser>(Endpoint.ME); fail() } catch (e: ApiError) { assertEquals(ApiError.Offline, e) }
        assertEquals("r1", store.refreshToken())

        offline = false
        try { client.get<SessionUser>(Endpoint.ME); fail() } catch (e: ApiError) { assertEquals(ApiError.Unauthenticated, e) }
        assertEquals(null, store.refreshToken())
    }

    @Test fun serverErrorCodeIsReadFromTheEnvelope() = runBlocking {
        val transport = Recorder { HttpResponse(409, """{"error":{"code":"MONTH_CLOSED","message":"x"}}""".toByteArray(), emptyMap()) }
        val client = ApiClient("https://api.test", transport, { "en" }, InMemoryTokenStore())
        try {
            client.get<SessionUser>(Endpoint.ME, auth = ApiClient.Auth.Anonymous); fail()
        } catch (e: ApiError.Server) {
            assertEquals(ErrorCode.MonthClosed, e.code)
        }
    }
}
