package com.hisaabwise.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.BottomSpacer
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.designsystem.components.HWCard
import com.hisaabwise.core.designsystem.components.HWCategoryKey
import com.hisaabwise.core.designsystem.components.HWDonut
import com.hisaabwise.core.designsystem.components.HWEyebrowRow
import com.hisaabwise.core.designsystem.components.HWMoneyField
import com.hisaabwise.core.designsystem.components.HWSavingsMeter
import com.hisaabwise.core.designsystem.components.HWSheet
import com.hisaabwise.core.designsystem.components.HWVerdictBadge
import com.hisaabwise.core.designsystem.components.SpendSlice
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.core.walkthrough.WalkthroughTarget
import com.hisaabwise.feature.home.components.HWReadRow
import com.hisaabwise.feature.home.components.HWStreakCard
import com.hisaabwise.feature.home.components.HWTipCard
import com.hisaabwise.feature.home.components.homeTourTarget
import com.hisaabwise.feature.home.components.stacksPairs

/** Wires [HomeViewModel] into [HomeScreen]. Loads every time the root appears, like iOS `.task`. */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onAddExpense: () -> Unit,
    onContinueLearning: () -> Unit,
    onOpenArticle: (HomeScreenPayload.ArticleTeaser) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.dispatch(HomeIntent.Load) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.GoToExpenses -> onAddExpense()
                HomeEffect.GoToLearn -> onContinueLearning()
                is HomeEffect.OpenArticle -> onOpenArticle(effect.teaser)
            }
        }
    }
    HomeScreen(state, viewModel::dispatch)
}

/**
 * Home: greeting, spending donut, savings meter, tip of the day, streak + reads. Port of iOS `HomeView`.
 * A first-run month keeps the whole screen; only the spending card shows the grey ring and its CTA.
 */
@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    SurfaceGround {
        StateView(
            state = state.content,
            onRetry = { onIntent(HomeIntent.Load) },
            empty = {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_empty), style = HWType.Body, color = HWColors.InkSecondary, textAlign = TextAlign.Center)
                }
            },
        ) { screen ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Greeting(screen)
                SpendingCard(screen, state.isolated, onIntent)
                SavingsCard(screen, state, onIntent)
                HWTipCard(
                    text = HomeLogic.tipShown(screen, state.substitutedTip).resolvedText,
                    onAnother = { onIntent(HomeIntent.ShowAnotherTip) },
                    modifier = Modifier.homeTourTarget(WalkthroughTarget.Tip),
                )
                Duo(screen, onIntent)
                BottomSpacer()
            }
            if (state.isEditingGoal) EditGoalSheet(screen, state, onIntent)
        }
    }
}

// MARK: - Greeting

@Composable
private fun Greeting(screen: HomeScreenPayload) {
    val line = stringResource(R.string.home_greeting, screen.greeting, screen.name)
    val range = HomeLogic.nameRange(line, screen.name)
    val text = buildAnnotatedString {
        append(line)
        if (range != null) addStyle(SpanStyle(color = HWColors.Accent), range.first, range.last + 1)
    }
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(text, style = HWType.Title, color = HWColors.Ink)
        Text(screen.dateLabel, style = HWType.Body, color = HWColors.InkTertiary)
    }
}

// MARK: - Spending

private fun slices(screen: HomeScreenPayload): List<SpendSlice> = screen.spending.categories.map {
    SpendSlice(id = it.id, name = it.name, amountDisplay = it.amount.display, share = it.share, shareLabel = it.shareLabel, slot = it.slot)
}

private val RingDiameter = 140.dp
private val RingThickness = 22.dp
private val RingHole = RingDiameter - RingThickness * 2

@Composable
private fun SpendingCard(screen: HomeScreenPayload, isolated: String?, onIntent: (HomeIntent) -> Unit) {
    HWCard(Modifier.homeTourTarget(WalkthroughTarget.SpendingCard)) {
        HWEyebrowRow(stringResource(R.string.home_spending_caption), trailing = screen.monthLabel)
        Spacer(Modifier.height(16.dp))
        if (screen.spending.isFirstRun) {
            FirstRun(screen, onIntent)
        } else {
            val slices = slices(screen)
            val onIsolate: (String?) -> Unit = { onIntent(HomeIntent.Isolate(it)) }
            if (stacksPairs()) {
                // At the largest type sizes the ring stands aside for its list.
                HWCategoryKey(slices, isolated, onIsolate, showAmounts = true)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HWDonut(slices, isolated, onIsolate, diameter = RingDiameter, thickness = RingThickness) {
                        CentreReadout(screen, isolated)
                    }
                    Spacer(Modifier.width(14.dp))
                    HWCategoryKey(slices, isolated, onIsolate, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            HWButton(
                stringResource(R.string.home_spending_addMore),
                onClick = { onIntent(HomeIntent.AddExpense) },
                icon = Icons.Filled.Add,
                modifier = Modifier.homeTourTarget(WalkthroughTarget.AddExpense),
            )
        }
    }
}

/** `.donut-mid` — caption, figure, share; the total and % of pay, or the isolated category and % of spend. */
@Composable
private fun CentreReadout(screen: HomeScreenPayload, isolated: String?) {
    val category = HomeLogic.isolatedCategory(isolated, screen)
    val caption = category?.name ?: stringResource(R.string.home_spending_total)
    val figure = category?.amount?.display ?: screen.spending.total.display
    val share = category?.shareLabel ?: screen.spending.shareOfPayLabel
    val value = stringResource(R.string.home_spending_readout_accessibilityValue, figure, share)
    Column(
        Modifier
            .widthIn(max = RingHole - 10.dp)
            .clearAndSetSemantics {
                contentDescription = caption
                stateDescription = value
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // The hole is a fixed circle, so each line shrinks to fit on one line rather than wrapping.
        FitLine(caption, HWType.Caption.copy(color = HWColors.InkSecondary), 13f, 8f)
        FitLine(figure, HWType.Subheading.copy(color = HWColors.Ink, fontWeight = FontWeight.ExtraBold), 20f, 9f)
        FitLine(share, HWType.Caption.copy(color = HWColors.InkTertiary), 13f, 8f)
    }
}

@Composable
private fun FitLine(text: String, style: androidx.compose.ui.text.TextStyle, max: Float, min: Float) {
    BasicText(
        text,
        style = style.copy(textAlign = TextAlign.Center),
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = min.sp, maxFontSize = max.sp, stepSize = 0.5.sp),
    )
}

/** The first-run card: the real ring in the empty-track colour, a sentence, and Add your first expense. */
@Composable
private fun FirstRun(screen: HomeScreenPayload, onIntent: (HomeIntent) -> Unit) {
    val total = stringResource(R.string.home_spending_total)
    val nothing = stringResource(R.string.home_spending_nothingYet)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HWDonut(emptyList(), null, {}, diameter = RingDiameter, thickness = RingThickness) {
            Column(
                Modifier.widthIn(max = RingHole - 10.dp).clearAndSetSemantics {
                    contentDescription = total
                    stateDescription = nothing
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FitLine(total, HWType.Caption.copy(color = HWColors.InkSecondary), 13f, 8f)
                FitLine(screen.spending.total.display, HWType.Subheading.copy(color = HWColors.InkTertiary, fontWeight = FontWeight.ExtraBold), 20f, 9f)
                FitLine(nothing, HWType.Caption.copy(color = HWColors.InkTertiary), 13f, 8f)
            }
        }
        Text(
            stringResource(R.string.home_spending_firstRun),
            style = HWType.Body,
            color = HWColors.InkSecondary,
            textAlign = TextAlign.Center,
        )
        HWButton(
            stringResource(R.string.home_spending_addFirst),
            onClick = { onIntent(HomeIntent.AddExpense) },
            icon = Icons.Filled.Add,
            // Present only on first run, so the tour knows to show the first-run copy.
            modifier = Modifier.homeTourTarget(WalkthroughTarget.AddExpenseFirst),
        )
    }
}

// MARK: - Savings

@Composable
private fun SavingsCard(screen: HomeScreenPayload, state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val savings = screen.savings
    val meterLabel = stringResource(R.string.home_savings_meter_accessibilityLabel)
    val meterValue = stringResource(R.string.home_savings_meter_accessibilityValue, savings.saved.display, savings.goal.display, savings.percentageLabel)
    HWCard(Modifier.homeTourTarget(WalkthroughTarget.Savings)) {
        HWEyebrowRow(stringResource(R.string.home_savings_caption), trailing = screen.monthLabel)
        Spacer(Modifier.height(14.dp))
        if (stacksPairs()) {
            // The text form of the meter, read as the same sentence the bar carries.
            Column(
                Modifier.fillMaxWidth().clearAndSetSemantics {
                    contentDescription = meterLabel
                    stateDescription = meterValue
                },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MeterRow(stringResource(R.string.home_savings_saved_label), savings.saved.display)
                MeterRow(stringResource(R.string.home_savings_goal_label), savings.goal.display)
                MeterRow(stringResource(R.string.home_savings_progress_label), savings.percentageLabel)
            }
        } else {
            HWSavingsMeter(
                position = savings.position,
                savedLabel = stringResource(R.string.component_meter_saved, savings.saved.display),
                zeroLabel = savings.zeroLabel,
                goalLabel = stringResource(R.string.component_meter_goal, savings.goal.display),
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = meterLabel
                    stateDescription = meterValue
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(footLine(savings), style = HWType.Body.copy(fontWeight = FontWeight.Medium), color = HWColors.InkSecondary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            // A third reading of what the bar already says, so TalkBack skips it.
            HWVerdictBadge(savings.percentageLabel, HomeLogic.verdictTint(savings.verdict), Modifier.clearAndSetSemantics {})
        }
        val nudge = savings.goalNudge
        if (nudge != null && !state.hasDismissedGoalNudge) {
            Spacer(Modifier.height(14.dp))
            GoalNudge(nudge, state.isRaisingGoal, onIntent)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            HWButton(
                stringResource(R.string.home_savings_editGoal),
                onClick = { onIntent(HomeIntent.BeginEditingGoal) },
                style = HWButtonStyle.Secondary,
                icon = Icons.Outlined.TrackChanges,
                isLoading = state.isRaisingGoal,
                fillWidth = false,
            )
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

@Composable
private fun footLine(savings: HomeScreenPayload.Savings): String = when (val line = HomeLogic.footLine(savings)) {
    is HomeLogic.FootLine.Met -> stringResource(R.string.home_savings_foot_met, line.saved)
    HomeLogic.FootLine.Nothing -> stringResource(R.string.home_savings_foot_nothing)
    is HomeLogic.FootLine.Remaining -> stringResource(R.string.home_savings_foot_remaining, line.saved, line.remaining)
}

/** A pay rise has left the goal behind: offer the server's figure, and a way not to take it. */
@Composable
private fun GoalNudge(nudge: HomeScreenPayload.GoalNudge, isRaising: Boolean, onIntent: (HomeIntent) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(HWRadius.Medium)
            .background(HWColors.BackgroundSecondary)
            .border(1.dp, HWColors.Separator, HWRadius.Medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.home_goalNudge_title), style = HWType.Caption, color = HWColors.InkSecondary, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.home_goalNudge_body, nudge.suggested.display, nudge.current.display), style = HWType.Caption.copy(fontWeight = FontWeight.Normal), color = HWColors.InkSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            HWButton(
                stringResource(R.string.home_goalNudge_action),
                onClick = { onIntent(HomeIntent.RaiseGoal) },
                style = HWButtonStyle.Secondary,
                icon = Icons.Filled.ArrowUpward,
                isLoading = isRaising,
                fillWidth = false,
            )
            HWButton(
                stringResource(R.string.home_goalNudge_dismiss),
                onClick = { onIntent(HomeIntent.DismissGoalNudge) },
                style = HWButtonStyle.Ghost,
                fillWidth = false,
            )
        }
    }
}

/** The Edit goal sheet: the target, typed in the goal's currency. The server converts; nothing here does. */
@Composable
private fun EditGoalSheet(screen: HomeScreenPayload, state: HomeState, onIntent: (HomeIntent) -> Unit) {
    HWSheet(stringResource(R.string.home_editGoal_title), onDismiss = { onIntent(HomeIntent.CloseEditGoal) }) {
        Text(stringResource(R.string.home_editGoal_explain), style = HWType.Body, color = HWColors.InkSecondary)
        Spacer(Modifier.height(14.dp))
        HWMoneyField(
            text = state.goalDraft,
            onTextChange = { onIntent(HomeIntent.EditGoalAmount(it)) },
            currencyCode = screen.savings.goal.currency,
            label = stringResource(R.string.home_editGoal_label),
            prominent = true,
            onImeAction = { onIntent(HomeIntent.SaveGoal) },
        )
        Spacer(Modifier.height(14.dp))
        HWButton(
            stringResource(R.string.home_editGoal_action),
            onClick = { onIntent(HomeIntent.SaveGoal) },
            icon = Icons.Filled.Check,
            isLoading = state.isRaisingGoal,
        )
    }
}

// MARK: - Streak and reading

/** `.duo` — the streak panel and the reading list side by side (stacked at the largest type sizes). */
@Composable
private fun Duo(screen: HomeScreenPayload, onIntent: (HomeIntent) -> Unit) {
    val streak: @Composable (Modifier) -> Unit = { modifier ->
        HWStreakCard(
            streak = screen.learning.streak,
            summary = screen.learning.summary,
            nextLesson = screen.learning.nextLesson,
            onClick = { onIntent(HomeIntent.ContinueLearning) },
            modifier = modifier,
        )
    }
    if (stacksPairs()) {
        Column(Modifier.homeTourTarget(WalkthroughTarget.Duo), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            streak(Modifier)
            Reads(screen, onIntent, Modifier)
        }
    } else {
        Row(
            Modifier.homeTourTarget(WalkthroughTarget.Duo).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1 : 1.3 — the reading column carries the text, so it is the one that grows.
            streak(Modifier.weight(1f).fillMaxHeight())
            Reads(screen, onIntent, Modifier.weight(1.3f))
        }
    }
}

@Composable
private fun Reads(screen: HomeScreenPayload, onIntent: (HomeIntent) -> Unit, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.08f), spotColor = HWColors.Ink.copy(alpha = 0.08f))
            .clip(HWRadius.Card)
            .background(HWColors.Surface)
            .border(1.dp, HWColors.Separator, HWRadius.Card)
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.home_reads_caption).uppercase(),
            style = HWType.Eyebrow,
            color = HWColors.AccentMuted,
            modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
        )
        screen.articles.forEach { teaser ->
            HWReadRow(teaser.short, teaser.icon, teaser.accent, onClick = { onIntent(HomeIntent.OpenArticle(teaser)) })
        }
    }
}
