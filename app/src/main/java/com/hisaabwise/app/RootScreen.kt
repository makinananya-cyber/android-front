package com.hisaabwise.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.core.designsystem.components.HWMark
import com.hisaabwise.feature.auth.ForgotPasswordRoute
import com.hisaabwise.feature.auth.LandingRoute
import com.hisaabwise.feature.auth.RegistrationRoute
import com.hisaabwise.feature.auth.SignInRoute
import kotlinx.coroutines.delay

/**
 * Holds the ViewModelStore for the signed-in shell, so every tab view model lives exactly as long
 * as one session: a new session generation clears the store and no previous user's data survives
 * (iOS rebuilds `TabViewModels` on sign-out). Activity-scoped, so it survives a language change.
 */
class ShellStoreHolder : ViewModel() {
    private var generation = -1
    private var store = ViewModelStore()

    fun ownerFor(sessionGeneration: Int): ViewModelStoreOwner {
        if (sessionGeneration != generation) {
            store.clear()
            store = ViewModelStore()
            generation = sessionGeneration
        }
        val current = store
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = current
        }
    }

    override fun onCleared() = store.clear()
}

object PreAuthRoute {
    const val LANDING = "landing"
    const val SIGN_IN = "signIn"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgotPassword"
}

/** Signed in → the five-tab shell; otherwise the pre-auth stack starting at Landing. */
@Composable
fun RootScreen(shellStores: ShellStoreHolder) {
    val container = LocalAppContainer.current
    val session by container.session.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        if (session.isSignedIn) {
            val owner = remember(session.generation) { shellStores.ownerFor(session.generation) }
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                AppShell()
            }
        } else {
            // A new controller per signed-out spell: the pre-auth path is cleared whenever sign-in state changes.
            val nav = rememberNavController()
            NavHost(nav, startDestination = PreAuthRoute.LANDING) {
                composable(PreAuthRoute.LANDING) {
                    LandingRoute(onGetStarted = { nav.navigate(PreAuthRoute.SIGN_IN) })
                }
                composable(PreAuthRoute.SIGN_IN) {
                    SignInRoute(
                        onBack = { nav.popBackStack() },
                        onForgotPassword = { nav.navigate(PreAuthRoute.FORGOT_PASSWORD) },
                        onRegister = { nav.navigate(PreAuthRoute.REGISTER) },
                    )
                }
                composable(PreAuthRoute.REGISTER) {
                    RegistrationRoute(
                        onBack = { nav.popBackStack() },
                        onSignInInstead = {
                            nav.navigate(PreAuthRoute.SIGN_IN) { popUpTo(PreAuthRoute.LANDING) }
                        },
                    )
                }
                composable(PreAuthRoute.FORGOT_PASSWORD) {
                    ForgotPasswordRoute(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() })
                }
            }
        }
        PrivacyCover()
        LaunchOverlay()
    }
}

/**
 * While the app is not in the foreground, a full-screen brand cover hides the figures
 * (iOS `PrivacyOverlay`). On Android 13+ the Recents thumbnail is also disabled in MainActivity.
 */
@Composable
private fun PrivacyCover() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var covered by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> covered = true
                Lifecycle.Event.ON_RESUME -> covered = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (covered) {
        Box(Modifier.fillMaxSize().background(HWColors.BrandBackground), contentAlignment = Alignment.Center) {
            HWMark(64.dp)
        }
    }
}

/**
 * The launch splash after the system splash: the logo on a light radial ground, scaling
 * 0.94→1 and fading in, held 1.4 s (0.8 s with Reduce Motion), then fading out over 0.45 s.
 * Fixed timing — it never waits for the network.
 */
@Composable
private fun LaunchOverlay() {
    var shown by rememberSaveable { mutableStateOf(true) }
    val reduceMotion = LocalReduceMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!shown) return@LaunchedEffect
        progress.animateTo(1f, tween(if (reduceMotion) 200 else 600, easing = HWMotion.EaseOut))
        delay(if (reduceMotion) 600 else 800)
        shown = false
    }
    AnimatedVisibility(visible = shown, exit = fadeOut(tween(450))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(HWColors.Surface, HWColors.Background, HWColors.AccentSoft))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.hw_launch_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp)
                    .scale(if (reduceMotion) 1f else 0.94f + 0.06f * progress.value)
                    .alpha(progress.value),
            )
        }
    }
}
