package com.hisaabwise.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The raw HTTP answer the client works from. */
class HttpResponse(val status: Int, val body: ByteArray, val headers: Map<String, String>) {
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

/** The seam tests replace. Production is [OkHttpTransport]. */
fun interface HttpTransport {
    /** Throws [IOException] for any transport failure; never for an HTTP status. */
    suspend fun send(request: Request): HttpResponse
}

class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .cache(null) // revalidation is done explicitly with ETags, like iOS (urlCache = nil)
        .callTimeout(30, TimeUnit.SECONDS)
        // Recover from a pooled keep-alive connection the server already closed (URLSession does this
        // silently). Safe for POSTs: every one carries an Idempotency-Key.
        .retryOnConnectionFailure(true)
        .build(),
) : HttpTransport {

    override suspend fun send(request: Request): HttpResponse = suspendCancellableCoroutine { cont ->
        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val bytes = it.body.bytes()
                        val headers = it.headers.names().associateWith { name -> it.header(name).orEmpty() }
                        cont.resume(HttpResponse(it.code, bytes, headers))
                    } catch (e: IOException) {
                        cont.resumeWithException(e)
                    }
                }
            }
        })
    }
}
