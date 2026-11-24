package com.hisaabwise.feature.auth.registration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.HWButton
import com.hisaabwise.core.designsystem.components.HWButtonStyle
import com.hisaabwise.core.designsystem.components.HWMoneyField
import com.hisaabwise.feature.auth.GoalComparison
import com.hisaabwise.feature.auth.RegistrationField
import com.hisaabwise.feature.auth.RegistrationIntent
import com.hisaabwise.feature.auth.RegistrationState
import com.hisaabwise.feature.auth.components.AuthFormFailure
import com.hisaabwise.feature.auth.components.AuthNote
import com.hisaabwise.feature.auth.components.CtaGlow
import com.hisaabwise.feature.auth.components.authEnters

/**
 * Step 3 — "set up your goals" (port of iOS `RegistrationGoalStep`). Submit sends what is in the
 * box; Skip sends the 20% suggestion with `goalWasSkipped = true`. The percentage under the box is
 * a ratio between two typed numbers, not money.
 */
@Composable
fun RegistrationGoalStep(state: RegistrationState, onIntent: (RegistrationIntent) -> Unit) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Statement(state, Modifier.authEnters(3))

        HWMoneyField(
            text = state.goalText,
            onTextChange = { onIntent(RegistrationIntent.GoalChanged(it)) },
            currencyCode = state.currency?.code.orEmpty(),
            modifier = Modifier.authEnters(4),
            label = stringResource(R.string.registration_three_goal_label),
            error = state.failure(RegistrationField.Goal)?.let { stringResource(it.copy) },
            onImeAction = { focus.clearFocus() },
        )

        AnimatedVisibility(state.suggestsUsingTheSuggestion) {
            HWButton(
                stringResource(R.string.registration_three_useSuggested),
                onClick = { onIntent(RegistrationIntent.UseSuggestedGoal) },
                style = HWButtonStyle.Ghost,
                icon = Icons.Filled.Check,
            )
        }

        AuthNote(hint(state), centred = false, alpha = 0.7f)

        Box(Modifier.authEnters(5)) {
            CtaGlow {
                HWButton(
                    stringResource(R.string.registration_three_action),
                    onClick = {
                        focus.clearFocus()
                        onIntent(RegistrationIntent.Submit)
                    },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    isLoading = state.isSubmitting,
                )
            }
        }
        HWButton(
            stringResource(R.string.registration_three_skip),
            onClick = {
                focus.clearFocus()
                onIntent(RegistrationIntent.SkipGoal)
            },
            modifier = Modifier.authEnters(5),
            style = HWButtonStyle.Ghost,
            isLoading = state.isSubmitting,
        )

        // An email collision is not drawn here: it sends the user back to step 1.
        state.formFailure?.let { AuthFormFailure(stringResource(it.copy)) }
    }
}

/** `.statement` — the glass card that explains the 20% rule and what it comes to. */
@Composable
private fun Statement(state: RegistrationState, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(HWRadius.Card)
            .background(HWColors.BrandSurface)
            .border(1.dp, HWColors.BrandSeparatorStrong, HWRadius.Card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(stringResource(R.string.registration_three_rule_eyebrow).uppercase(), style = HWType.Eyebrow, color = HWColors.BrandInkAccent)
        Text(stringResource(R.string.registration_three_rule_body), style = HWType.Body, color = HWColors.BrandInkSecondary)
        // `.st-amount` — symbol, figure, and "a month" on one baseline, read as one thing.
        Row(
            Modifier.padding(vertical = 5.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(state.currency?.symbol.orEmpty(), style = HWType.Subheading, color = HWColors.BrandInkAccent, modifier = Modifier.alignByBaseline())
            Text(
                state.suggestedGoalText.orEmpty(),
                style = HWType.Display.copy(letterSpacing = (-1.2).sp, fontWeight = FontWeight.ExtraBold),
                color = HWColors.BrandInk,
                modifier = Modifier.alignByBaseline(),
            )
            Text(stringResource(R.string.registration_three_perMonth), style = HWType.Caption, color = HWColors.BrandInkSecondary, modifier = Modifier.alignByBaseline())
        }
        Text(stringResource(R.string.registration_three_rule_footer), style = HWType.Caption, color = HWColors.BrandInkSecondary.copy(alpha = 0.8f))
    }
}

/** `#goal-hint` — three whole sentences plus the empty one, so a translation places the number. */
@Composable
private fun hint(state: RegistrationState): String {
    val share = state.goalShareText
    val comparison = state.goalAgainstSuggestion
    if (share == null || comparison == null) return stringResource(R.string.registration_three_hint_empty)
    return when (comparison) {
        GoalComparison.Same -> stringResource(R.string.registration_three_hint_onTarget, share)
        GoalComparison.Above -> stringResource(R.string.registration_three_hint_above, share)
        GoalComparison.Below -> stringResource(R.string.registration_three_hint_below, share)
    }
}
