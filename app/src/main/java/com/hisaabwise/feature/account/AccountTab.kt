package com.hisaabwise.feature.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hisaabwise.app.HideTabBar
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.components.HWToast

private object AccountRoutes {
    const val ROOT = "account"
    fun of(section: AccountScreen.Section) = when (section) {
        AccountScreen.Section.Personal -> "account/personal"
        AccountScreen.Section.Language -> "account/language"
        AccountScreen.Section.Currency -> "account/currency"
        AccountScreen.Section.Password -> "account/password"
    }
}

/**
 * The Account tab: the settings root and its four pushed pages, all served by one
 * [AccountViewModel] (as on iOS). Pushed pages hide the tab bar.
 */
@Composable
fun AccountTab(onReplayTour: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel {
        AccountViewModel(
            api = container.api,
            content = container.content,
            language = container.language,
            session = container.session,
            repaint = container.repaint,
            contactEmail = container.contactEmail,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                AccountEffect.CloseDetail ->
                    if (nav.currentDestination?.route != AccountRoutes.ROOT) nav.popBackStack(AccountRoutes.ROOT, inclusive = false)
                is AccountEffect.OpenMail -> try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${effect.address}")))
                } catch (_: ActivityNotFoundException) {
                    // No mail app: the address is on screen to copy by hand.
                }
            }
        }
    }

    val back: () -> Unit = { nav.popBackStack() }
    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = AccountRoutes.ROOT) {
            composable(AccountRoutes.ROOT) {
                AccountRootScreen(
                    state = state,
                    onIntent = vm::dispatch,
                    onOpenSection = { nav.navigate(AccountRoutes.of(it)) { launchSingleTop = true } },
                    onReplayTour = onReplayTour,
                    contactEmail = container.contactEmail,
                )
            }
            composable(AccountRoutes.of(AccountScreen.Section.Personal)) {
                HideTabBar()
                AccountPersonalScreen(state, vm::dispatch, back)
            }
            composable(AccountRoutes.of(AccountScreen.Section.Language)) {
                HideTabBar()
                AccountLanguageScreen(state, vm::dispatch, back)
            }
            composable(AccountRoutes.of(AccountScreen.Section.Currency)) {
                HideTabBar()
                AccountCurrencyScreen(state, vm::dispatch, back)
            }
            composable(AccountRoutes.of(AccountScreen.Section.Password)) {
                HideTabBar()
                AccountPasswordScreen(state, vm::dispatch, back)
            }
        }
        val notice = state.notice
        HWToast(notice?.let { stringResource(noticeCopy(it)) }) { vm.dispatch(AccountIntent.DismissNotice) }
    }
}
