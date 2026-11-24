package com.hisaabwise.feature.expenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisaabwise.R
import com.hisaabwise.app.HideTabBar
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.components.HWToast
import com.hisaabwise.core.model.LoadState

private const val ROOT = "expenses"
private const val CATEGORY = "category/{id}"

/**
 * The Expenses tab: the root and one category page, served by **one** [ExpensesViewModel] (as on iOS), so a
 * write on the category page re-renders both from the same payload.
 */
@Composable
fun ExpensesTab() {
    val container = LocalAppContainer.current
    val vm = viewModel { ExpensesViewModel(container.api, container.content, container.repaint) }
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    // On iOS the category destination lives inside the loaded content, so a payload that is no longer
    // loaded (offline write, failure, reload) pops back to the root, whose state view offers the retry.
    val loaded = state.content is LoadState.Loaded
    LaunchedEffect(loaded) {
        if (!loaded && nav.currentBackStackEntry?.destination?.route == CATEGORY) nav.popBackStack(ROOT, inclusive = false)
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = ROOT) {
            composable(ROOT) {
                ExpensesRootScreen(state, vm::dispatch, onOpenCategory = { id -> nav.navigate("category/$id") { launchSingleTop = true } })
            }
            composable(CATEGORY, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                HideTabBar()
                LaunchedEffect(id) {
                    vm.dispatch(ExpensesIntent.Open(id))
                    vm.dispatch(ExpensesIntent.LoadPicklists)
                }
                ExpenseCategoryScreen(state, id, vm::dispatch, onBack = { nav.popBackStack() })
            }
        }

        HWToast(state.notice?.let { stringResource(it.copy) }, onDismiss = { vm.dispatch(ExpensesIntent.DismissNotice) })
    }

    state.refileOffer?.let { offer ->
        AlertDialog(
            onDismissRequest = { vm.dispatch(ExpensesIntent.DismissRefile) },
            title = { Text(stringResource(R.string.expenses_refile_title)) },
            text = { Text(stringResource(R.string.expenses_refile_message, offer.monthLabel)) },
            confirmButton = {
                TextButton(onClick = { vm.dispatch(ExpensesIntent.Refile) }) {
                    Text(stringResource(R.string.expenses_refile_confirm, offer.monthLabel), color = HWColors.Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dispatch(ExpensesIntent.DismissRefile) }) {
                    Text(stringResource(R.string.expenses_refile_cancel), color = HWColors.Accent)
                }
            },
            containerColor = HWColors.Surface,
        )
    }
}

private val ExpensesNotice.copy: Int
    get() = when (this) {
        ExpensesNotice.EntryAdded -> R.string.expenses_notice_entryAdded
        ExpensesNotice.EntryRemoved -> R.string.expenses_notice_entryRemoved
        ExpensesNotice.FixedUpdated -> R.string.expenses_notice_fixedUpdated
        ExpensesNotice.BillsUpdated -> R.string.expenses_notice_billsUpdated
        ExpensesNotice.WantsShareUpdated -> R.string.expenses_notice_wantsShareUpdated
    }
