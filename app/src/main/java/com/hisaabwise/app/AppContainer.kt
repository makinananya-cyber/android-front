package com.hisaabwise.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.hisaabwise.BuildConfig
import com.hisaabwise.core.data.AppPreferences
import com.hisaabwise.core.data.FileContentStore
import com.hisaabwise.core.data.InMemoryTokenStore
import com.hisaabwise.core.data.KeystoreTokenStore
import com.hisaabwise.core.i18n.LanguageManager
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.ContentLoader
import com.hisaabwise.core.network.OkHttpTransport
import com.hisaabwise.core.session.ScreenRepaint
import com.hisaabwise.core.session.SessionCoordinator
import com.hisaabwise.core.walkthrough.WalkthroughModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Terms and Privacy URLs from build config (iOS `LegalLinks`). */
data class LegalLinks(val terms: String, val privacy: String)

/**
 * The object graph — the Android twin of iOS `AppEnvironment`. Built once in
 * [com.hisaabwise.HisaabWiseApplication]; screens reach it through [LocalAppContainer] and build
 * their view models from it.
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val prefs = AppPreferences(context)
    val language = LanguageManager(prefs)
    private val keptTokens = KeystoreTokenStore(context)
    private val transientTokens = InMemoryTokenStore()

    val api = ApiClient(
        baseUrl = BuildConfig.API_BASE_URL,
        transport = OkHttpTransport(),
        acceptLanguage = { language.acceptLanguage },
        refreshTokens = transientTokens,
    )
    val content = ContentLoader(api, FileContentStore(context))
    val session = SessionCoordinator(api, keptTokens, transientTokens, appScope)
    val repaint = ScreenRepaint()
    val walkthrough = WalkthroughModel(prefs)
    val legal = LegalLinks(BuildConfig.TERMS_URL, BuildConfig.PRIVACY_URL)

    /** The support address shown on the Account screen. */
    val contactEmail = "hisaabwise1@gmail.com"
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
