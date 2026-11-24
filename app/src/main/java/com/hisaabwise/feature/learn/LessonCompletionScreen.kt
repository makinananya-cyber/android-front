package com.hisaabwise.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.feature.learn.components.CompletionStat
import com.hisaabwise.feature.learn.components.CompletionStatTone
import com.hisaabwise.feature.learn.components.CompletionStats
import com.hisaabwise.feature.learn.components.Confetti
import com.hisaabwise.feature.learn.components.DoneBadge
import com.hisaabwise.feature.learn.components.RunButton
import com.hisaabwise.feature.learn.components.RunButtonTone
import com.hisaabwise.feature.learn.components.WeekDay
import com.hisaabwise.feature.learn.components.WeekStrip
import com.hisaabwise.feature.learn.components.announce
import com.hisaabwise.feature.learn.components.verdictHaptic

@Composable
fun LessonCompletionRoute(request: CompletionRequest, onScreenUpdate: (LearnPayload) -> Unit, onContinue: () -> Unit) {
    val container = LocalAppContainer.current
    val view = LocalView.current
    val context = LocalContext.current
    // Keyed by the idempotency key: this object *is* the intent, so a retry reuses it and its key.
    val vm = viewModel(key = "completion-${request.idempotencyKey}") {
        LessonCompletionViewModel(container.api, request, onScreenUpdate)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            if (effect is LessonCompletionEffect.Landed) {
                // Fires on the response, not on the last tap: what is confirmed is that the lesson counted.
                view.verdictHaptic(true)
                view.announce(
                    context.getString(
                        R.string.learn_completion_announcement,
                        effect.completion.xpEarned.accessibilityLabel,
                        effect.completion.accuracy.accessibilityLabel,
                    ),
                )
            }
        }
    }
    LessonCompletionScreen(state, vm::dispatch, onContinue)
}

/** The celebration. Every figure is read from the response. Port of iOS `LessonCompletionView`. */
@Composable
fun LessonCompletionScreen(state: LessonCompletionState, onIntent: (LessonCompletionIntent) -> Unit, onContinue: () -> Unit) {
    SurfaceGround {
        StateView(
            state.content,
            onRetry = { onIntent(LessonCompletionIntent.Load) },
            empty = {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.learn_completion_empty), style = HWType.Body, color = HWColors.InkSecondary, textAlign = TextAlign.Center)
                }
            },
        ) { completion ->
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 26.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    DoneBadge()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(if (completion.isFirstCompletion) R.string.learn_completion_title else R.string.learn_completion_title_again),
                            style = HWType.Title, color = HWColors.Ink, textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(completion.streakLine, style = HWType.BodyLarge, color = HWColors.InkSecondary, textAlign = TextAlign.Center)
                    }
                    val xp = stringResource(R.string.learn_completion_xp)
                    val accuracy = stringResource(R.string.learn_completion_accuracy)
                    val streak = stringResource(R.string.learn_completion_streak)
                    CompletionStats(
                        listOf(
                            { m -> CompletionStat(CompletionStatTone.Experience, completion.xpEarned.display, xp, completion.xpEarned.accessibilityLabel, m) },
                            { m -> CompletionStat(CompletionStatTone.Accuracy, completion.accuracy.display, accuracy, completion.accuracy.accessibilityLabel, m) },
                            { m -> CompletionStat(CompletionStatTone.Streak, completion.screen.streak.display, streak, completion.screen.streak.accessibilityLabel, m) },
                        ),
                    )
                    WeekStrip(completion.week.map { WeekDay(it.label, it.isComplete, it.isToday, it.accessibilityLabel) })
                    RunButton(stringResource(R.string.learn_completion_continue), RunButtonTone.Right, Modifier.padding(top = 4.dp), onClick = onContinue)
                }
                Confetti()
            }
        }
    }
}
