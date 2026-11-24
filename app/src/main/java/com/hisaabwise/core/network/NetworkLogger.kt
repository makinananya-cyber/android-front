package com.hisaabwise.core.network

import android.util.Log
import com.hisaabwise.BuildConfig

/** Debug-only request log with secrets redacted (Authorization, passwords, tokens, answers). */
internal object NetworkLogger {
    private const val TAG = "HWNetwork"
    private val SECRET = Regex("\"(password|currentPassword|newPassword|refreshToken|accessToken|answer|ticket|secret)\"\\s*:\\s*\"[^\"]*\"")

    fun redact(body: String): String = SECRET.replace(body) { "\"${it.groupValues[1]}\":\"<redacted>\"" }

    fun request(method: String, url: String, body: String?) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "→ $method $url ${body?.let(::redact).orEmpty()}")
    }

    fun response(method: String, url: String, status: Int, ms: Long, body: String?) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "← $status $method $url (${ms}ms) ${body?.take(2000)?.let(::redact).orEmpty()}")
    }

    fun failure(method: String, url: String, error: Throwable) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "✕ $method $url ${error.javaClass.simpleName}: ${error.message}")
    }
}
