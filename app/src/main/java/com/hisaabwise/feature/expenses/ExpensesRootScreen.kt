package com.hisaabwise.feature.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.Appearance
import com.hisaabwise.core.designsystem.components.BottomSpacer
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.designsystem.components.HWSheet
import com.hisaabwise.core.designsystem.components.HWTopBar
import com.hisaabwise.core.designsystem.components.LocalAppearance
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.core.walkthrough.LocalWalkthrough
import com.hisaabwise.core.walkthrough.WalkthroughTarget
import com.hisaabwise.core.walkthrough.walkthroughTarget
import com.hisaabwise.feature.expenses.components.CategoryPanel
import com.hisaabwise.feature.expenses.components.CategoryRow
import com.hisaabwise.feature.expenses.components.SpendSummaryCard
import com.hisaabwise.feature.expenses.components.WantsBudgetBlock
import com.hisaabwise.feature.expenses.components.vector
import androidx.compose.runtime.CompositionLocalProvider

/** Expenses root: the summary card with the wants bar, and the category panel. Stateless. */
@Composable
fun ExpensesRootScreen(
    state: ExpensesState,
    onIntent: (ExpensesIntent) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    SurfaceGround {
        StateView(
            state = state.content,
            onRetry = { onIntent(ExpensesIntent.Load) },
            empty = {
                Text(
                    stringResource(R.string.expenses_empty),
                    style = HWType.Body,
                    color = HWColors.InkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                )
            },
        ) { screen ->
            ExpensesContent(screen, state.isWriting, onIntent, onOpenCategory)
        }
    }
}

@Composable
private fun ExpensesContent(
    screen: ExpensesScreen,
    isWriting: Boolean,
    onIntent: (ExpensesIntent) -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    var isEditingWantsShare by rememberSaveable { mutableStateOf(false) }
    val summaryRequester = remember { BringIntoViewRequester() }
    val wantsRequester = remember { BringIntoViewRequester() }
    val listRequester = remember { BringIntoViewRequester() }
    WalkthroughScrolls(
        mapOf(
            WalkthroughTarget.ExpensesSummary to summaryRequester,
            WalkthroughTarget.WantsBudget to wantsRequester,
            WalkthroughTarget.CategoryList to listRequester,
        ),
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HWTopBar(title = stringResource(R.string.expenses_title), eyebrow = stringResource(R.string.expenses_eyebrow))
        Column(
            Modifier.padding(horizontal = 18.dp).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            val wants = screen.wants
            val wantsCaption = stringResource(R.string.expenses_wants_caption)
            val wantsAmount = stringResource(R.string.expenses_wants_amount, wants.used.display, wants.allowance.display)
            val wantsDescription = stringResource(
                if (wants.isOver) R.string.expenses_wants_accessibilityValue_over else R.string.expenses_wants_accessibilityValue,
                wants.used.display, wants.allowance.display, wants.percentageLabel,
            )
            SpendSummaryCard(
                caption = stringResource(R.string.expenses_summary_caption),
                total = screen.summary.total.display,
                splits = listOf(
                    stringResource(R.string.expenses_summary_fixed) to screen.summary.fixed.display,
                    stringResource(R.string.expenses_summary_variable) to screen.summary.variable.display,
                    stringResource(R.string.expenses_summary_income) to screen.summary.income.display,
                ),
                modifier = Modifier
                    .bringIntoViewRequester(summaryRequester)
                    .walkthroughTarget(WalkthroughTarget.ExpensesSummary),
            ) {
                WantsBudgetBlock(
                    caption = wantsCaption,
                    amount = wantsAmount,
                    percentageLabel = wants.percentageLabel,
                    fill = wants.fill,
                    isOver = wants.isOver,
                    accessibilityDescription = wantsDescription,
                    modifier = Modifier
                        .bringIntoViewRequester(wantsRequester)
                        .walkthroughTarget(WalkthroughTarget.WantsBudget),
                )
                Spacer(Modifier.size(14.dp))
                // The control that changes the wants share, beside the bar it changes. Always offered.
                CompositionLocalProvider(LocalAppearance provides Appearance.Brand) {
                    HWButton(
                        text = stringResource(R.string.expenses_wants_edit),
                        onClick = { isEditingWantsShare = true },
                        style = HWButtonStyle.Secondary,
                        icon = Icons.Filled.Tune,
                        isLoading = isWriting,
                    )
                }
            }

            CategoryPanel(
                Modifier
                    .bringIntoViewRequester(listRequester)
                    .walkthroughTarget(WalkthroughTarget.CategoryList),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .semantics(mergeDescendants = true) { heading() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.expenses_categories_caption).uppercase(),
                        style = HWType.Eyebrow,
                        color = HWColors.AccentMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(stringResource(R.string.expenses_categories_hint), style = HWType.Caption, color = HWColors.Accent)
                }
                screen.orderedCategories.forEach { category ->
                    CategoryRow(
                        name = category.name,
                        hint = category.hint,
                        total = category.total.display,
                        icon = category.icon.vector(),
                        isIncoming = category.flow == ExpensesScreen.Flow.Incoming,
                        onClick = { onOpenCategory(category.id) },
                    )
                }
            }
        }
        BottomSpacer()
    }

    if (isEditingWantsShare) {
        WantsShareSheet(
            wants = screen.wants,
            onSelect = { percent ->
                isEditingWantsShare = false
                onIntent(ExpensesIntent.SetWantsShare(percent))
            },
            onDismiss = { isEditingWantsShare = false },
        )
    }
}

/** The Edit budget sheet: one row per offered share, each with the allowance it buys, ticked if in force. */
@Composable
private fun WantsShareSheet(wants: ExpensesScreen.Wants, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    HWSheet(title = stringResource(R.string.expenses_wants_edit_title), onDismiss = onDismiss, skipPartiallyExpanded = false) {
        Text(
            stringResource(R.string.expenses_wants_edit_explain),
            style = HWType.Body,
            color = HWColors.InkSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        WantsShare.offered(wants).forEach { share ->
            val selected = share.percent == wants.sharePercent
            val meta = wantsRowMeta(share, wants)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable(role = Role.RadioButton) { onSelect(share.percent) }
                    .semantics(mergeDescendants = true) { this.selected = selected }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(share.label), style = HWType.BodyLarge, color = HWColors.Ink, modifier = Modifier.weight(1f))
                if (meta != null) {
                    Text(meta, style = HWType.Caption.copy(fontWeight = FontWeight.SemiBold), color = HWColors.InkSecondary)
                    Spacer(Modifier.size(10.dp))
                }
                if (selected) Icon(Icons.Filled.Check, null, tint = HWColors.Accent)
                else Spacer(Modifier.size(24.dp))
            }
            HorizontalDivider(color = HWColors.Separator)
        }
    }
}

/** The allowance a share would buy, from the server; the 30 row also says "Rule of thumb". */
@Composable
private fun wantsRowMeta(share: WantsShare, wants: ExpensesScreen.Wants): String? {
    val option = wants.options?.firstOrNull { it.percent == share.percent }
        ?: return if (share == WantsShare.RuleOfThumb) stringResource(R.string.expenses_wants_edit_default) else null
    return if (share == WantsShare.RuleOfThumb) stringResource(R.string.expenses_wants_edit_ruleOfThumb_meta, option.allowance.display)
    else option.allowance.display
}

private val WantsShare.label: Int
    get() = when (this) {
        WantsShare.Tenth -> R.string.expenses_wants_edit_option_10
        WantsShare.Fifteen -> R.string.expenses_wants_edit_option_15
        WantsShare.Fifth -> R.string.expenses_wants_edit_option_20
        WantsShare.Quarter -> R.string.expenses_wants_edit_option_25
        WantsShare.RuleOfThumb -> R.string.expenses_wants_edit_option_30
        WantsShare.ThirtyFive -> R.string.expenses_wants_edit_option_35
        WantsShare.TwoFifths -> R.string.expenses_wants_edit_option_40
    }

/** Scrolls each Expenses tour target into view as the guided tour reaches it (iOS `hwWalkthroughScrolls`). */
@Composable
private fun WalkthroughScrolls(requesters: Map<WalkthroughTarget, BringIntoViewRequester>) {
    val model = LocalWalkthrough.current ?: return
    val tour by model.state.collectAsStateWithLifecycle()
    val target = model.current(tour)?.target
    LaunchedEffect(target) {
        requesters[target]?.bringIntoView()
    }
}
