package com.hisaabwise

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.app.RootScreen
import com.hisaabwise.app.ShellStoreHolder
import com.hisaabwise.core.designsystem.HisaabWiseTheme

/**
 * The single activity. AppCompat so the per-app language (AppCompatDelegate) applies; a language
 * change recreates it, and every view model survives through the activity's ViewModelStore.
 */
class MainActivity : AppCompatActivity() {

    private val shellStores: ShellStoreHolder by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Keep account figures out of the Recents thumbnail — the iOS app-switcher privacy cover.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)

        val container = (application as HisaabWiseApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                HisaabWiseTheme {
                    RootScreen(shellStores)
                }
            }
        }
    }
}
