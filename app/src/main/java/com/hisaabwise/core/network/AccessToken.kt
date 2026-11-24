package com.hisaabwise.core.network

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The short-lived JWT, held in memory only. The client reads `exp` and `sec` without checking
 * the signature — the server does that. Sent exactly as issued.
 */
class AccessToken private constructor(val raw: String, val expiresAtEpochSeconds: Long, val securityEpoch: Int?) {

    fun isNearExpiry(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean =
        expiresAtEpochSeconds - nowEpochSeconds < REFRESH_WINDOW_SECONDS

    @Serializable
    private data class Claims(val exp: Long, val sec: Int? = null)

    companion object {
        const val REFRESH_WINDOW_SECONDS = 60L
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(raw: String): AccessToken? {
            val segments = raw.split(".")
            if (segments.size != 3) return null
            return try {
                val payload = Base64.decode(segments[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val claims = json.decodeFromString(Claims.serializer(), payload.decodeToString())
                AccessToken(raw, claims.exp, claims.sec)
            } catch (_: Exception) {
                null
            }
        }
    }
}
