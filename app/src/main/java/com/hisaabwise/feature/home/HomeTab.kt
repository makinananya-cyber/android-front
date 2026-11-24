package com.hisaabwise.feature.home

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisaabwise.app.LocalAppContainer

private const val ROUTE_HOME = "home"
private const val ROUTE_ARTICLE = "article/{id}?title={title}&icon={icon}&accent={accent}"

private fun articleRoute(teaser: HomeScreenPayload.ArticleTeaser): String =
    "article/${Uri.encode(teaser.id)}?title=${Uri.encode(teaser.short)}&icon=${teaser.icon.name}&accent=${teaser.accent}"

/**
 * The Home tab's own stack: the Home root and pushed articles. The Home view model lives in the shell's
 * store (rebuilt per session); an article's lives with its back-stack entry, one per tap.
 */
@Composable
fun HomeTab(onAddExpense: () -> Unit, onContinueLearning: () -> Unit) {
    val container = LocalAppContainer.current
    val shellOwner = checkNotNull(LocalViewModelStoreOwner.current) { "HomeTab needs a ViewModelStoreOwner" }
    val homeViewModel = viewModel(viewModelStoreOwner = shellOwner) {
        HomeViewModel(container.api, container.content, container.repaint)
    }
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeRoute(
                viewModel = homeViewModel,
                onAddExpense = onAddExpense,
                onContinueLearning = onContinueLearning,
                onOpenArticle = { teaser -> nav.navigate(articleRoute(teaser)) { launchSingleTop = true } },
            )
        }
        composable(
            ROUTE_ARTICLE,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("icon") { type = NavType.StringType; defaultValue = ArticleIcon.Lightbulb.name },
                navArgument("accent") { type = NavType.IntType; defaultValue = 4 },
            ),
        ) { entry ->
            val args = entry.arguments
            ArticleRoute(
                id = args?.getString("id").orEmpty(),
                title = args?.getString("title").orEmpty(),
                icon = ArticleIcon.fromName(args?.getString("icon")),
                accent = args?.getInt("accent") ?: 4,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
