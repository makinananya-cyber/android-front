package com.hisaabwise.feature.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hisaabwise.app.LocalAppContainer

private const val ROUTE_MAP = "learn"
private const val ROUTE_LESSON = "lesson/{lessonId}"

/**
 * The Learn tab: the unit map, and a full-screen lesson player pushed over it (the tab bar hides).
 * The map's view model lives in the shell's store so the player can read its material and the
 * completion can re-render it.
 */
@Composable
fun LearnTab() {
    val container = LocalAppContainer.current
    val learn = viewModel { LearnViewModel(container.api, container.content, container.repaint) }
    val nav = rememberNavController()
    val close = remember(nav) { { if (nav.currentDestination?.route == ROUTE_LESSON) nav.popBackStack(); Unit } }

    NavHost(nav, startDestination = ROUTE_MAP) {
        composable(ROUTE_MAP) {
            LearnRoute(learn, onOpenLesson = { nav.navigate("lesson/$it") { launchSingleTop = true } })
        }
        composable(ROUTE_LESSON) { entry ->
            val lessonId = entry.arguments?.getString("lessonId").orEmpty()
            // Taken once and kept: a reload behind the player must not restart the run.
            val material = remember(lessonId) { learn.material(lessonId) }
            if (material == null) {
                LaunchedEffect(Unit) { close() }
            } else {
                LessonPlayerRoute(
                    material,
                    onScreenUpdate = { learn.dispatch(LearnIntent.ApplyScreen(it)) },
                    onClose = close,
                )
            }
        }
    }
}
