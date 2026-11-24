package com.hisaabwise

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hisaabwise.app.AppContainer
import kotlinx.coroutines.launch

class HisaabWiseApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.language.applyToUi()

        // On every return to the foreground: refresh the token if near expiry, then revalidate (GET /v1/me).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.appScope.launch { container.session.onForeground() }
            }
        })
    }
}
