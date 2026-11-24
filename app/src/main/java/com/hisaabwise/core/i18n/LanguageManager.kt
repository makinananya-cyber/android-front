package com.hisaabwise.core.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hisaabwise.core.data.AppPreferences
import com.hisaabwise.core.model.AppLanguage
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.Endpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * The app's chosen language. Port of iOS `LanguageManager`.
 *
 * Initial choice: the stored language, else the device's preferred languages, else English.
 * Selecting is optimistic: `PUT /v1/me/language`, save on success, revert on failure.
 * The language drives both the UI (per-app locale via AppCompat, RTL for ar/ur, Latin digits)
 * and the `Accept-Language` header, which is how the server formats money and labels.
 */
class LanguageManager(private val prefs: AppPreferences) {

    private val _selected = MutableStateFlow(
        prefs.language
            ?: AppLanguage.preferring(LocaleListCompat.getAdjustedDefault().toList())
            ?: AppLanguage.English,
    )
    val selected: StateFlow<AppLanguage> = _selected.asStateFlow()

    val acceptLanguage: String get() = _selected.value.tag

    /** Apply the stored choice to the UI at startup. */
    fun applyToUi() {
        val tags = _selected.value.localeTag
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != tags) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
        }
    }

    @Serializable
    private data class LanguageUpdate(val language: String)

    class ServerDisagreed(val confirmed: String) : Exception("server kept $confirmed")

    /** Throws the [com.hisaabwise.core.network.ApiError] if the server refuses; the choice is reverted. */
    suspend fun select(language: AppLanguage, client: ApiClient) {
        if (language == _selected.value && prefs.language == language) return
        val previous = _selected.value
        _selected.value = language
        try {
            val confirmed = client.put<LanguageUpdate, LanguageUpdate>(Endpoint.LANGUAGE, LanguageUpdate(language.tag))
            if (confirmed.language != language.tag) throw ServerDisagreed(confirmed.language)
            prefs.language = language
            withContext(Dispatchers.Main) { applyToUi() }
        } catch (e: Exception) {
            _selected.value = previous
            throw e
        }
    }

    private fun LocaleListCompat.toList(): List<Locale> = (0 until size()).mapNotNull { get(it) }
}
