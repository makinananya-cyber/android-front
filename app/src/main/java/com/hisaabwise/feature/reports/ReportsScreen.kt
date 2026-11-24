package com.hisaabwise.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.VerdictTint
import com.hisaabwise.core.designsystem.components.BottomSpacer
import com.hisaabwise.core.designsystem.components.HWTopBar
import com.hisaabwise.core.designsystem.components.PageHorizontalPadding
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.core.walkthrough.LocalWalkthrough
import com.hisaabwise.core.walkthrough.WalkthroughTarget
import com.hisaabwise.core.walkthrough.walkthroughTarget
import com.hisaabwise.feature.reports.components.ArchiveHero
import com.hisaabwise.feature.reports.components.EmptyNote
import com.hisaabwise.feature.reports.components.FigureSplit
import com.hisaabwise.feature.reports.components.MonthRow
import com.hisaabwise.feature.reports.components.TrendBar
import com.hisaabwise.feature.reports.components.TrendChart
import com.hisaabwise.feature.reports.components.YearHeader
import com.hisaabwise.feature.reports.components.proportionSegments

@Composable
fun ReportsRoute(onOpenMonth: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { ReportsViewModel(container.api, container.repaint) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Reload on every appearance, as iOS `.task { load() }` does (tab re-selected, back from a month).
    LaunchedEffect(vm) { vm.dispatch(ReportsIntent.Load) }
    LaunchedEffect(vm) {
        vm.effects.collect { if (it is ReportsEffect.OpenMonth) onOpenMonth(it.monthKey) }
    }
    ReportsScreen(state, vm::dispatch)
}

/** Port of iOS `ReportsView` / `ReportsArchivePage`. */
@Composable
fun ReportsScreen(state: ReportsState, onIntent: (ReportsIntent) -> Unit) {
    SurfaceGround {
        StateView(state.content, onRetry = { onIntent(ReportsIntent.Load) }) { screen ->
            ArchivePage(screen, onIntent)
        }
    }
}

@Composable
private fun ArchivePage(screen: ReportsScreenPayload, onIntent: (ReportsIntent) -> Unit) {
    val scroll = rememberScrollState()
    // The tour's two Reports steps sit at the top of the page: bring them into view.
    val walkthrough = LocalWalkthrough.current
    if (walkthrough != null) {
        val tour by walkthrough.state.collectAsStateWithLifecycle()
        val target = walkthrough.current(tour)?.target
        LaunchedEffect(target) {
            if (target == WalkthroughTarget.ReportsHero || target == WalkthroughTarget.ReportsMonth) scroll.animateScrollTo(0)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        HWTopBar(title = stringResource(R.string.reports_title), eyebrow = stringResource(R.string.reports_eyebrow))
        Column(
            Modifier.padding(horizontal = PageHorizontalPadding).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The heading stays when nothing has closed yet; only the empty part says so.
            if (screen.years.isEmpty()) {
                EmptyNote(stringResource(R.string.reports_empty))
            } else {
                Hero(screen, Modifier.walkthroughTarget(WalkthroughTarget.ReportsHero))
                val firstYear = screen.years.first()
                screen.years.forEach { year ->
                    Column(
                        Modifier.semantics { isTraversalGroup = true },
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        YearHeader(year.label, stringResource(R.string.reports_year_saved, year.totalSaved.display))
                        year.months.forEachIndexed { index, month ->
                            val isFirst = year === firstYear && index == 0
                            MonthRow(
                                name = month.label,
                                year = year.label,
                                spent = stringResource(R.string.reports_month_spent, month.spent.display),
                                verdict = month.verdict.tint,
                                percentageLabel = month.percentageLabel,
                                segments = proportionSegments(month.segments.map { it.slot to it.share }),
                                hint = stringResource(R.string.reports_month_hint),
                                onClick = { onIntent(ReportsIntent.OpenMonth(month.monthKey)) },
                                modifier = if (isFirst) Modifier.walkthroughTarget(WalkthroughTarget.ReportsMonth) else Modifier,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(0.dp))
        }
        BottomSpacer()
    }
}

@Composable
private fun Hero(screen: ReportsScreenPayload, modifier: Modifier) {
    val summary = screen.summary
    ArchiveHero(
        caption = stringResource(R.string.reports_hero_caption),
        subtitle = stringResource(R.string.reports_hero_subtitle, summary.goalsMetLabel),
        splits = listOf(
            FigureSplit(stringResource(R.string.reports_hero_months), summary.monthCount.display),
            FigureSplit(stringResource(R.string.reports_hero_averageSpend), summary.averageSpend.display),
            FigureSplit(stringResource(R.string.reports_hero_totalSaved), summary.totalSaved.display),
        ),
        modifier = modifier,
    ) {
        TrendChart(
            bars = screen.trend.bars.map {
                TrendBar(it.monthKey, it.label, it.fill, it.verdict.tint, it.percentageLabel, it.accessibilityLabel)
            },
            goalHeight = screen.trend.goalPosition,
            accessibilityLabel = stringResource(R.string.reports_trend_accessibilityLabel),
        )
    }
}

/** The payload's verdict as the design system's tint. */
val Verdict.tint: VerdictTint
    get() = when (this) {
        Verdict.Hit -> VerdictTint.Hit
        Verdict.Near -> VerdictTint.Near
        Verdict.Miss -> VerdictTint.Miss
    }
