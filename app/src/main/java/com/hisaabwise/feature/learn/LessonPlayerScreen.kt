package com.hisaabwise.feature.learn

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.HideTabBar
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWMotion
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.LocalReduceMotion
import com.hisaabwise.feature.learn.components.AnswerField
import com.hisaabwise.feature.learn.components.AnswerOption
import com.hisaabwise.feature.learn.components.AnswerState
import com.hisaabwise.feature.learn.components.ComboBadge
import com.hisaabwise.feature.learn.components.FeedbackNote
import com.hisaabwise.feature.learn.components.Kicker
import com.hisaabwise.feature.learn.components.RunButton
import com.hisaabwise.feature.learn.components.RunButtonTone
import com.hisaabwise.feature.learn.components.RunHeader
import com.hisaabwise.feature.learn.components.TeachingEntry
import com.hisaabwise.feature.learn.components.TeachingTip
import com.hisaabwise.feature.learn.components.WorkedExample
import com.hisaabwise.feature.learn.components.announce
import com.hisaabwise.feature.learn.components.markdown
import com.hisaabwise.feature.learn.components.tint
import com.hisaabwise.feature.learn.components.verdictAccent
import com.hisaabwise.feature.learn.components.verdictHaptic
import kotlinx.coroutines.delay

/**
 * The lesson player route: full screen over the map (the tab bar is hidden), and once the run is
 * finished the completion screen covers it. [onScreenUpdate] re-renders the map from the completion.
 */
@Composable
fun LessonPlayerRoute(material: LearnMap.Material, onScreenUpdate: (LearnPayload) -> Unit, onClose: () -> Unit) {
    HideTabBar()
    val view = LocalView.current
    val vm = viewModel(key = "lesson-${material.lesson.id}") { LessonPlayerViewModel(material) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) {
        vm.effects.collect { if (it is LessonPlayerEffect.Graded) view.verdictHaptic(it.isCorrect) }
    }
    val completion = state.completion
    if (completion != null) {
        LessonCompletionRoute(completion, onScreenUpdate, onContinue = onClose)
    } else {
        LessonPlayerScreen(state, vm::dispatch, onClose)
    }
}

@Composable
fun LessonPlayerScreen(state: LessonPlayerState, onIntent: (LessonPlayerIntent) -> Unit, onClose: () -> Unit) {
    val run = state.run
    val tint = state.material.unit.content.accent.tint()
    val reduceMotion = LocalReduceMotion.current
    val view = LocalView.current
    val context = LocalContext.current

    // The dialog trails the run by 700 ms so the footer explaining the last answer is readable first;
    // no wait under reduce motion.
    var showOutOfHearts by remember { mutableStateOf(false) }
    LaunchedEffect(run.isOutOfHearts) {
        if (!run.isOutOfHearts) {
            showOutOfHearts = false
            return@LaunchedEffect
        }
        if (!reduceMotion) delay(700)
        showOutOfHearts = true
    }

    val comboText = state.comboMilestone?.let { stringResource(R.string.learn_player_combo, it.toString()) }
    LaunchedEffect(state.comboMilestone) {
        val milestone = state.comboMilestone ?: return@LaunchedEffect
        view.announce(context.getString(R.string.learn_player_combo_announcement, milestone.toString()))
        delay(1600)
        onIntent(LessonPlayerIntent.DismissCombo)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(HWColors.Background)
            .imePadding(),
    ) {
        RunHeader(
            stepsDone = run.index,
            stepCount = run.stepCount,
            heartsRemaining = run.heartsRemaining,
            heartCount = LessonRun.HEART_COUNT,
            tint = tint,
            progressLabel = stringResource(R.string.learn_player_progress_accessibilityLabel, run.stepNumberText, run.stepCountText),
            heartsLabel = stringResource(R.string.learn_player_hearts_accessibilityLabel, run.heartsRemainingText, LessonRun.heartCountText),
            onClose = onClose,
            modifier = Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp),
        )

        Box(Modifier.weight(1f).fillMaxWidth()) {
            // Keyed by step, and each page draws its own snapshot of the run, so the outgoing page keeps its content.
            AnimatedContent(
                targetState = run,
                contentKey = { it.index },
                transitionSpec = {
                    if (reduceMotion) fadeIn(tween(HWMotion.QUICK)) togetherWith fadeOut(tween(HWMotion.QUICK))
                    else (fadeIn(tween(HWMotion.SHORT)) + slideInVertically(tween(HWMotion.MEDIUM, easing = HWMotion.EaseOut)) { it / 12 }) togetherWith fadeOut(tween(HWMotion.QUICK))
                },
                label = "step",
            ) { pageRun ->
                val step = pageRun.step
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                ) {
                    if (step != null) {
                        StepPage(
                            step = step,
                            run = pageRun,
                            kicker = stringResource(R.string.learn_player_kicker, state.material.unit.content.numberText, state.material.lesson.content.title),
                            state = state,
                            onIntent = onIntent,
                        )
                    }
                }
            }
            ComboBadge(comboText.orEmpty(), state.comboMilestone != null, Modifier.align(Alignment.TopCenter))
        }

        Footer(state, onIntent)
    }

    if (showOutOfHearts) {
        // Not dismissible by accident: the run is over and both ways on are choices.
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.learn_player_outOfHearts_title)) },
            text = { Text(stringResource(R.string.learn_player_outOfHearts_message)) },
            confirmButton = {
                TextButton(onClick = { showOutOfHearts = false; onIntent(LessonPlayerIntent.Restart) }) {
                    Text(stringResource(R.string.learn_player_outOfHearts_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOutOfHearts = false; onClose() }) {
                    Text(stringResource(R.string.learn_player_outOfHearts_leave))
                }
            },
            containerColor = HWColors.Surface,
        )
    }
}

/** `.p-foot` — the feedback, then the one button, on the verdict's wash. */
@Composable
private fun Footer(state: LessonPlayerState, onIntent: (LessonPlayerIntent) -> Unit) {
    val run = state.run
    val verdict = run.verdict
    val tint = state.material.unit.content.accent.tint()
    val fill = verdict?.let { verdictAccent(it.isCorrect).soft } ?: HWColors.Background
    Column(Modifier.fillMaxWidth().background(fill)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(HWColors.Separator))
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            AnimatedVisibility(verdict != null, enter = fadeIn(), exit = fadeOut()) {
                if (verdict != null) {
                    FeedbackNote(
                        isCorrect = verdict.isCorrect,
                        headline = stringResource(headline(verdict, run.results.size)),
                        answerCaption = if (verdict.isCorrect) null else stringResource(R.string.learn_player_rightAnswer),
                        answers = if (verdict.isCorrect) emptyList() else answerText(state, verdict),
                        explanation = state.resolved(verdict.explanation),
                    )
                }
            }
            RunButton(
                title = stringResource(primaryTitle(run)),
                tone = when (verdict?.isCorrect) {
                    true -> RunButtonTone.Right
                    false -> RunButtonTone.Wrong
                    null -> RunButtonTone.Lesson(tint)
                },
                enabled = run.isReadyToCheck,
                onClick = { onIntent(LessonPlayerIntent.Primary) },
            )
        }
    }
}

/** One step, drawn: teaching, a question with options, or a box for a figure. Port of iOS `LessonStepPage`. */
@Composable
private fun StepPage(step: Step, run: LessonRun, kicker: String, state: LessonPlayerState, onIntent: (LessonPlayerIntent) -> Unit) {
    val tint = state.material.unit.content.accent.tint()
    val resolve = state::resolved
    when (step) {
        is Step.Teach -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Kicker(kicker, Icons.Outlined.Lightbulb, tint)
            Text(markdown(resolve(step.heading)), style = HWType.Heading, color = HWColors.Ink, modifier = Modifier.semantics { heading() })
            step.paragraphs.forEach { Paragraph(resolve(it)) }
            step.list.forEach { TeachingEntry(resolve(it.term), resolve(it.detail), tint) }
            step.afterList.forEach { Paragraph(resolve(it)) }
            step.example?.let { WorkedExample(resolve(it.heading), resolve(it.body), tint) }
            step.tip?.let { TeachingTip(stringResource(R.string.learn_player_tip), resolve(it)) }
        }
        is Step.Choice -> {
            val allowsMany = step is Step.MultiSelect
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Kicker(
                    stringResource(if (allowsMany) R.string.learn_player_kind_multi else R.string.learn_player_kind_single),
                    Icons.Filled.Star, tint,
                )
                Prompt(resolve(step.prompt))
                step.options.forEachIndexed { index, text ->
                    val optionState = optionState(index, run)
                    AnswerOption(
                        text = resolve(text),
                        state = optionState,
                        allowsMany = allowsMany,
                        stateLabel = optionStateLabel(optionState)?.let { stringResource(it) },
                    ) { onIntent(LessonPlayerIntent.Choose(index)) }
                }
            }
        }
        is Step.Numeric -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Kicker(stringResource(R.string.learn_player_kind_numeric), Icons.Filled.Numbers, tint)
            Prompt(resolve(step.prompt))
            AnswerField(
                label = stringResource(R.string.learn_player_answer_label),
                text = (run.answer as? LessonRun.Answer.Typed)?.text.orEmpty(),
                onTextChange = { onIntent(LessonPlayerIntent.Type(it)) },
                symbol = if (step.showsCurrencySymbol) state.material.currencyToken.token else null,
                state = numericState(run),
                hint = stringResource(R.string.learn_player_answer_hint),
                isEditable = run.verdict == null,
            )
        }
    }
}

@Composable
private fun Paragraph(text: String) {
    Text(markdown(text), style = HWType.BodyLarge.copy(fontWeight = FontWeight.Normal), color = HWColors.InkSecondary, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun Prompt(text: String) {
    Text(
        markdown(text), style = HWType.Subheading, color = HWColors.Ink,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).semantics { heading() },
    )
}

// region Mapping (pure; mirrors the iOS statics)

internal fun optionState(option: Int, run: LessonRun): AnswerState {
    val verdict = run.verdict ?: return if (isChosen(option, run)) AnswerState.Chosen else AnswerState.Offered
    if (option in verdict.answers) return AnswerState.Right
    return if (isChosen(option, run)) AnswerState.Wrong else AnswerState.Offered
}

internal fun isChosen(option: Int, run: LessonRun): Boolean = when (val a = run.answer) {
    is LessonRun.Answer.One -> a.option == option
    is LessonRun.Answer.Several -> option in a.options
    else -> false
}

internal fun optionStateLabel(state: AnswerState): Int? = when (state) {
    AnswerState.Offered -> null
    AnswerState.Chosen -> R.string.learn_player_option_chosen
    AnswerState.Right -> R.string.learn_player_option_right
    AnswerState.Wrong -> R.string.learn_player_option_wrong
}

internal fun numericState(run: LessonRun): AnswerState {
    val verdict = run.verdict ?: return AnswerState.Offered
    return if (verdict.isCorrect) AnswerState.Right else AnswerState.Wrong
}

internal fun primaryTitle(run: LessonRun): Int {
    val verdict = run.verdict
        ?: return if (run.step?.isQuestion == true) R.string.learn_player_check else R.string.learn_player_continue
    return if (verdict.isCorrect) R.string.learn_player_continue else R.string.learn_player_gotIt
}

internal val praise = listOf(
    R.string.learn_player_praise_1, R.string.learn_player_praise_2, R.string.learn_player_praise_3,
    R.string.learn_player_praise_4, R.string.learn_player_praise_5, R.string.learn_player_praise_6,
    R.string.learn_player_praise_7,
)

internal val encouragement = listOf(
    R.string.learn_player_encourage_1, R.string.learn_player_encourage_2,
    R.string.learn_player_encourage_3, R.string.learn_player_encourage_4,
)

/** The design's PRAISE / ENCOURAGE lists, rotated deterministically by how many have been answered. */
internal fun headline(verdict: LessonRun.Verdict, answered: Int): Int {
    val lines = if (verdict.isCorrect) praise else encouragement
    return lines[kotlin.math.abs(answered) % lines.size]
}

/** The right answer as it should have been given: the correct options, or the figure. */
private fun answerText(state: LessonPlayerState, verdict: LessonRun.Verdict): List<String> = when (val step = state.run.step) {
    is Step.Choice -> verdict.answers.mapNotNull { step.options.getOrNull(it)?.let(state::resolved) }
    is Step.Numeric -> listOf(step.answerText)
    else -> emptyList()
}

// endregion
