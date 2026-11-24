package com.hisaabwise.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWBanner
import com.hisaabwise.core.model.AppTab
import com.hisaabwise.core.walkthrough.LocalWalkthrough
import com.hisaabwise.core.walkthrough.WalkthroughTrigger
import com.hisaabwise.feature.account.AccountTab
import com.hisaabwise.feature.expenses.ExpensesTab
import com.hisaabwise.feature.home.HomeTab
import com.hisaabwise.feature.learn.LearnTab
import com.hisaabwise.feature.reports.ReportsTab
import kotlinx.coroutines.delay

/** Pushed pages hide the tab bar (iOS `.hwHidesTabBar()`); call [HideTabBar] inside them. */
@Stable
class TabBarController {
    var hiders by mutableIntStateOf(0)
    val isVisible: Boolean get() = hiders == 0
}

val LocalTabBar = staticCompositionLocalOf { TabBarController() }

@Composable
fun HideTabBar() {
    val controller = LocalTabBar.current
    DisposableEffect(controller) {
        controller.hiders++
        onDispose { controller.hiders-- }
    }
}

private data class TabItem(val tab: AppTab, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(AppTab.Home, R.string.shell_tab_home, Icons.Filled.Home),
    TabItem(AppTab.Expenses, R.string.shell_tab_expenses, Icons.Filled.CreditCard),
    TabItem(AppTab.Learn, R.string.shell_tab_learn, Icons.AutoMirrored.Filled.MenuBook),
    TabItem(AppTab.Reports, R.string.shell_tab_reports, Icons.Filled.BarChart),
    TabItem(AppTab.Account, R.string.shell_tab_account, Icons.Filled.Person),
)

/**
 * The five-tab shell. Each tab keeps its own back stack (its own NavHost, saved while another
 * tab is showing). The guided tour starts 800 ms after the shell first appears.
 */
@Composable
fun AppShell() {
    val container = LocalAppContainer.current
    val session by container.session.state.collectAsStateWithLifecycle()
    val walkthrough = container.walkthrough
    val tour by walkthrough.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { androidx.compose.runtime.mutableStateOf(AppTab.Home) }
    val saveable = rememberSaveableStateHolder()
    val tabBar = remember { TabBarController() }

    LaunchedEffect(Unit) {
        delay(800)
        walkthrough.startIfNeeded(session.lastAuthEntry)
    }
    // The tour switches tab per step.
    val tourScreen = walkthrough.current(tour)?.screen
    LaunchedEffect(tourScreen) { if (tourScreen != null) selected = tourScreen }

    CompositionLocalProvider(LocalTabBar provides tabBar, LocalWalkthrough provides walkthrough) {
        Box(Modifier.fillMaxSize().background(HWColors.Background)) {
            saveable.SaveableStateProvider(selected.name) {
                when (selected) {
                    AppTab.Home -> HomeTab(
                        onAddExpense = { selected = AppTab.Expenses },
                        onContinueLearning = { selected = AppTab.Learn },
                    )
                    AppTab.Expenses -> ExpensesTab()
                    AppTab.Learn -> LearnTab()
                    AppTab.Reports -> ReportsTab()
                    AppTab.Account -> AccountTab(onReplayTour = {
                        selected = AppTab.Home
                        walkthrough.start(WalkthroughTrigger.Replay)
                    })
                }
            }

            if (session.user?.emailVerified == false) {
                HWBanner(
                    stringResource(R.string.shell_verifyEmail),
                    Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 16.dp),
                )
            }

            AnimatedVisibility(
                visible = tabBar.isVisible && !tour.isPresenting,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                TabBar(selected) { selected = it }
            }

            if (tour.isPresenting) WalkthroughOverlay(walkthrough, tour)
        }
    }
}

/** The floating pill tab bar from the designs. */
@Composable
private fun TabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(18.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.2f), spotColor = HWColors.Ink.copy(alpha = 0.2f))
            .clip(HWRadius.Card)
            .background(HWColors.Surface.copy(alpha = 0.96f))
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        tabs.forEach { item ->
            val isSelected = item.tab == selected
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(HWRadius.Large)
                    .background(if (isSelected) HWColors.BackgroundSecondary else HWColors.Surface.copy(alpha = 0f))
                    .clickable(role = Role.Tab) { onSelect(item.tab) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(item.icon, null, tint = HWColors.Ink, modifier = Modifier.size(26.dp))
                Text(
                    stringResource(item.label),
                    style = HWType.Micro.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                    color = HWColors.Ink,
                )
            }
        }
    }
}
