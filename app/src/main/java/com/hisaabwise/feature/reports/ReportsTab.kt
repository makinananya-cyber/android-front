package com.hisaabwise.feature.reports

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hisaabwise.app.HideTabBar

private const val ARCHIVE = "archive"
private const val MONTH = "month/{monthKey}"

/** The Reports tab's stack: the archive, and one closed month pushed by its key (tab bar hidden). */
@Composable
fun ReportsTab() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = ARCHIVE) {
        composable(ARCHIVE) {
            ReportsRoute(onOpenMonth = { key -> nav.navigate("month/$key") { launchSingleTop = true } })
        }
        composable(MONTH, arguments = listOf(navArgument("monthKey") { type = NavType.StringType })) { entry ->
            HideTabBar()
            ReportsMonthRoute(
                monthKey = entry.arguments?.getString("monthKey").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
