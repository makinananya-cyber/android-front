package com.hisaabwise.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWBackBar
import com.hisaabwise.core.designsystem.components.HWCard
import com.hisaabwise.core.designsystem.components.HWCategoryKey
import com.hisaabwise.core.designsystem.components.HWDonut
import com.hisaabwise.core.designsystem.components.HWSavingsMeter
import com.hisaabwise.core.designsystem.components.HWTopBar
import com.hisaabwise.core.designsystem.components.HWVerdictBadge
import com.hisaabwise.core.designsystem.components.PageHorizontalPadding
import com.hisaabwise.core.designsystem.components.SpendSlice
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.feature.reports.ReportsMonthPayload.Portion
import com.hisaabwise.feature.reports.components.AccordionEntry
import com.hisaabwise.feature.reports.components.AccordionRow
import com.hisaabwise.feature.reports.components.AllowanceBar
import com.hisaabwise.feature.reports.components.CardTop
import com.hisaabwise.feature.reports.components.FactGrid
import com.hisaabwise.feature.reports.components.FactItem
import com.hisaabwise.feature.reports.components.FigureSplit
import com.hisaabwise.feature.reports.components.PortionSplit
import com.hisaabwise.feature.reports.components.SpendSummary
import com.hisaabwise.feature.reports.components.SplitRow
import com.hisaabwise.feature.reports.components.SummaryNote
import com.hisaabwise.feature.reports.components.isAccessibilityFontSize
import com.hisaabwise.feature.reports.components.vector

@Composable
fun ReportsMonthRoute(monthKey: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel(key = "reports-month-$monthKey") { ReportsMonthViewModel(monthKey, container.api, container.repaint) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.effects.collect { if (it is ReportsMonthEffect.Back) onBack() } }
    ReportsMonthScreen(state, vm::dispatch)
}

/** Port of iOS `ReportsMonthView` / `ReportsMonthPage`: seven cards over one request. */
@Composable
fun ReportsMonthScreen(state: ReportsMonthState, onIntent: (ReportsMonthIntent) -> Unit) {
    SurfaceGround {
        Column(Modifier.fillMaxSize()) {
            HWBackBar(
                title = state.content.valueOrNull?.title.orEmpty(),
                onBack = { onIntent(ReportsMonthIntent.Back) },
                backDescription = stringResource(R.string.reports_back),
            )
            Column(Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.statusBars)) {
                StateView(
                    state.content,
                    onRetry = { onIntent(ReportsMonthIntent.Load) },
                    empty = { Text(stringResource(R.string.reports_detail_empty), style = HWType.Body, color = HWColors.InkSecondary) },
                ) { screen ->
                    MonthPage(screen, state, onIntent)
                }
            }
        }
    }
}

@Composable
private fun MonthPage(screen: ReportsMonthPayload, state: ReportsMonthState, onIntent: (ReportsMonthIntent) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HWTopBar(title = screen.title, eyebrow = stringResource(R.string.reports_detail_eyebrow))
        Column(
            Modifier.padding(horizontal = PageHorizontalPadding).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Summary(screen)
            SpendingCard(screen, state, onIntent)
            SavingsCard(screen)
            WantsCard(screen.wants)
            SplitCard(screen.split)
            EntriesCard(screen, state, onIntent)
            FactsCard(screen)
        }
        Spacer(Modifier.navigationBarsPadding().height(32.dp))
    }
}

// MARK: - The totals

@Composable
private fun Summary(screen: ReportsMonthPayload) {
    val needs = screen.segment(Portion.Needs)
    val note = if (screen.totals.isAdapted && needs != null) {
        stringResource(R.string.reports_detail_note_adapted, needs.amount.display, screen.split.income.display)
    } else {
        stringResource(R.string.reports_detail_note_plain)
    }
    SpendSummary(
        caption = stringResource(R.string.reports_detail_summary_caption),
        total = screen.totals.spent.display,
        splits = listOf(
            FigureSplit(stringResource(R.string.reports_detail_summary_fixed), screen.totals.fixed.display),
            FigureSplit(stringResource(R.string.reports_detail_summary_variable), screen.totals.variable.display),
            FigureSplit(stringResource(R.string.reports_detail_summary_additionalIncome), screen.totals.additionalIncome.display),
        ),
    ) {
        SummaryNote(note)
    }
}

// MARK: - The donut

@Composable
private fun SpendingCard(screen: ReportsMonthPayload, state: ReportsMonthState, onIntent: (ReportsMonthIntent) -> Unit) {
    val slices = screen.spending.categories.map {
        SpendSlice(it.id, it.name, it.amount.display, it.share, it.shareLabel, it.slot)
    }
    HWCard {
        CardTop(stringResource(R.string.reports_detail_spending_caption), screen.spending.categoryCountLabel)
        Spacer(Modifier.height(14.dp))
        if (slices.isEmpty()) {
            NothingLogged(screen)
        } else {
            val category = state.isolatedCategory
            val figure = category?.amount?.display ?: screen.totals.spent.display
            val share = category?.shareLabel ?: screen.spending.shareOfIncomeLabel
            val caption = category?.name ?: stringResource(R.string.reports_detail_spending_total)
            val readoutValue = stringResource(R.string.reports_detail_spending_readout_accessibilityValue, figure, share)
            val onIsolate: (String?) -> Unit = { onIntent(ReportsMonthIntent.Isolate(it)) }
            if (isAccessibilityFontSize()) {
                // The ring goes at accessibility sizes; the readout and the key remain.
                Readout(caption, figure, readoutValue, HWColors.Ink, Modifier.fillMaxWidth(), TextAlign.Start)
                Text(share, style = HWType.Caption, color = HWColors.InkTertiary, modifier = Modifier.clearAndSetSemantics {})
            } else {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HWDonut(slices, state.isolated, onIsolate) {
                        Readout(caption, figure, readoutValue, HWColors.Ink, Modifier.padding(horizontal = 30.dp), TextAlign.Center)
                    }
                    // Under the ring rather than inside it: the sentence does not fit the hole.
                    Text(share, style = HWType.Caption, color = HWColors.InkTertiary, modifier = Modifier.clearAndSetSemantics {})
                }
            }
            Spacer(Modifier.height(14.dp))
            HWCategoryKey(slices, state.isolated, onIsolate)
        }
    }
}

@Composable
private fun Readout(caption: String, figure: String, value: String, ink: androidx.compose.ui.graphics.Color, modifier: Modifier, align: TextAlign) {
    Column(
        modifier.clearAndSetSemantics { contentDescription = caption; stateDescription = value },
        horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(caption, style = HWType.Caption, color = HWColors.InkSecondary, textAlign = align)
        Text(figure, style = HWType.Subheading, color = ink, textAlign = align)
    }
}

/** A month with nothing logged keeps the ring, in the empty-track colour with the readout. */
@Composable
private fun NothingLogged(screen: ReportsMonthPayload) {
    val total = stringResource(R.string.reports_detail_spending_total)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val readout = @Composable { m: Modifier, align: TextAlign ->
            Readout(total, screen.totals.spent.display, screen.totals.spent.display, HWColors.InkTertiary, m, align)
        }
        if (isAccessibilityFontSize()) {
            readout(Modifier.fillMaxWidth(), TextAlign.Start)
        } else {
            HWDonut(emptyList(), null, {}) { readout(Modifier.padding(horizontal = 30.dp), TextAlign.Center) }
        }
        Text(
            stringResource(R.string.reports_detail_spending_nothingLogged),
            style = HWType.Body,
            color = HWColors.InkSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// MARK: - The savings meter

@Composable
private fun SavingsCard(screen: ReportsMonthPayload) {
    val savings = screen.savings
    val saved = savings.saved.display
    val foot = when {
        savings.remaining != null -> stringResource(R.string.reports_detail_savings_foot_short, saved, savings.remaining.display)
        savings.surplus != null -> stringResource(R.string.reports_detail_savings_foot_over, saved, savings.surplus.display)
        else -> stringResource(R.string.reports_detail_savings_foot_exact, saved)
    }
    val description = stringResource(
        R.string.reports_detail_savings_meter_accessibilityValue, saved, savings.goal.display, screen.percentageLabel,
    )
    val meterLabel = stringResource(R.string.home_savings_meter_accessibilityLabel)
    HWCard {
        CardTop(stringResource(R.string.reports_detail_savings_caption), savings.shareOfIncomeLabel)
        Spacer(Modifier.height(14.dp))
        if (isAccessibilityFontSize()) {
            Column(
                Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = meterLabel; stateDescription = description },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MeterRow(stringResource(R.string.home_savings_saved_label), saved)
                MeterRow(stringResource(R.string.home_savings_goal_label), savings.goal.display)
                MeterRow(stringResource(R.string.home_savings_progress_label), screen.percentageLabel)
            }
        } else {
            HWSavingsMeter(
                position = savings.position,
                savedLabel = stringResource(R.string.component_meter_saved, saved),
                zeroLabel = savings.zeroLabel,
                goalLabel = stringResource(R.string.component_meter_goal, savings.goal.display),
                modifier = Modifier.clearAndSetSemantics { contentDescription = meterLabel; stateDescription = description },
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(foot, style = HWType.Body.copy(fontWeight = FontWeight.Medium), color = HWColors.InkSecondary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            HWVerdictBadge(screen.percentageLabel, screen.verdict.tint, Modifier.clearAndSetSemantics {})
        }
    }
}

@Composable
private fun MeterRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = HWType.Caption, color = HWColors.InkSecondary)
        Spacer(Modifier.width(10.dp))
        Text(value, style = HWType.Body.copy(fontWeight = FontWeight.SemiBold), color = HWColors.Ink, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

// MARK: - The wants allowance

@Composable
private fun WantsCard(wants: ReportsMonthPayload.Wants) {
    val description = if (wants.isOver) {
        stringResource(R.string.reports_detail_wants_accessibilityValue_over, wants.used.display, wants.allowance.display, wants.percentageLabel)
    } else {
        stringResource(R.string.reports_detail_wants_accessibilityValue, wants.used.display, wants.allowance.display, wants.percentageLabel)
    }
    // No line when the payload has no figure for the sentence to name.
    val foot = if (wants.isOver) {
        wants.excess?.let { stringResource(R.string.reports_detail_wants_foot_over, it.display) }
    } else {
        wants.remaining?.let { stringResource(R.string.reports_detail_wants_foot_within, it.display) }
    }
    HWCard {
        CardTop(stringResource(R.string.reports_detail_wants_caption), stringResource(R.string.reports_detail_wants_hint))
        Spacer(Modifier.height(14.dp))
        AllowanceBar(
            amount = wants.used.display,
            allowance = stringResource(R.string.reports_detail_wants_allowance, wants.allowance.display),
            percentageLabel = wants.percentageLabel,
            fill = wants.fill,
            isOver = wants.isOver,
            accessibilityDescription = description,
        )
        if (foot != null) {
            Spacer(Modifier.height(14.dp))
            Text(foot, style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkSecondary, modifier = Modifier.fillMaxWidth())
        }
    }
}

// MARK: - The split

@Composable
private fun SplitCard(split: ReportsMonthPayload.Split) {
    val rows = split.segments.map { segment ->
        SplitRow(
            name = stringResource(segment.portion.nameRes),
            amount = segment.amount.display,
            target = segment.target?.let { stringResource(R.string.reports_detail_split_target, it.display) },
            share = segment.share,
            color = segment.portion.colour,
        )
    }
    HWCard {
        CardTop(
            stringResource(R.string.reports_detail_split_caption),
            stringResource(R.string.reports_detail_split_sub, split.income.display),
        )
        Spacer(Modifier.height(14.dp))
        PortionSplit(rows, stringResource(R.string.reports_detail_split_accessibilityLabel))
    }
}

private val Portion.nameRes: Int
    get() = when (this) {
        Portion.Needs -> R.string.reports_detail_split_needs
        Portion.Wants -> R.string.reports_detail_split_wants
        Portion.Saved -> R.string.reports_detail_split_saved
        Portion.Surplus -> R.string.reports_detail_split_surplus
    }

private val Portion.colour
    get() = when (this) {
        Portion.Needs -> HWColors.CategoryRent
        Portion.Wants -> HWColors.CategoryEntertainment
        Portion.Saved -> HWColors.MeterReached
        Portion.Surplus -> HWColors.MeterHigh
    }

// MARK: - Everything logged

@Composable
private fun EntriesCard(screen: ReportsMonthPayload, state: ReportsMonthState, onIntent: (ReportsMonthIntent) -> Unit) {
    HWCard {
        CardTop(stringResource(R.string.reports_detail_entries_caption), stringResource(R.string.reports_detail_entries_hint))
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            screen.groups.forEach { group ->
                val expanded = state.isExpanded(group.id)
                val incoming = group.flow == Flow.Incoming
                AccordionRow(
                    name = group.name,
                    summaryLabel = group.summaryLabel,
                    amount = group.total.display,
                    icon = group.icon.vector(),
                    slot = group.slot,
                    isIncoming = incoming,
                    isExpanded = expanded,
                    entries = group.entries.map { AccordionEntry(it.label, it.dateLabel, it.amount.display) },
                    emptyNote = if (incoming) {
                        stringResource(R.string.reports_detail_entries_noIncome)
                    } else {
                        stringResource(R.string.reports_detail_entries_none, group.name)
                    },
                    hint = stringResource(if (expanded) R.string.reports_detail_entries_collapse else R.string.reports_detail_entries_expand),
                    onToggle = { onIntent(ReportsMonthIntent.Toggle(group.id)) },
                )
            }
        }
    }
}

// MARK: - For the record

@Composable
private fun FactsCard(screen: ReportsMonthPayload) {
    val facts = screen.facts.map { fact ->
        FactItem(fact.kind.name, stringResource(fact.kind.labelRes), fact.value, fact.note)
    }
    HWCard {
        CardTop(stringResource(R.string.reports_detail_facts_caption), null)
        Spacer(Modifier.height(14.dp))
        FactGrid(facts)
    }
}

private val ReportsMonthPayload.Kind.labelRes: Int
    get() = when (this) {
        ReportsMonthPayload.Kind.Salary -> R.string.reports_detail_fact_salary
        ReportsMonthPayload.Kind.Goal -> R.string.reports_detail_fact_goal
        ReportsMonthPayload.Kind.Saved -> R.string.reports_detail_fact_saved
        ReportsMonthPayload.Kind.BiggestCost -> R.string.reports_detail_fact_biggestCost
        ReportsMonthPayload.Kind.Needs -> R.string.reports_detail_fact_needs
        // Filtered out at decode; never drawn.
        ReportsMonthPayload.Kind.LeftOver, ReportsMonthPayload.Kind.Unknown -> R.string.reports_detail_fact_leftOver
    }
