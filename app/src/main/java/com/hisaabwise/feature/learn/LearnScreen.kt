package com.hisaabwise.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisaabwise.R
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.components.BottomSpacer
import com.hisaabwise.core.designsystem.components.HWMark
import com.hisaabwise.core.designsystem.components.HWSheet
import com.hisaabwise.core.designsystem.components.HWToast
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.core.walkthrough.WalkthroughTarget
import com.hisaabwise.core.walkthrough.walkthroughTarget
import com.hisaabwise.feature.learn.components.LessonNode
import com.hisaabwise.feature.learn.components.LessonNodeState
import com.hisaabwise.feature.learn.components.LessonRow
import com.hisaabwise.feature.learn.components.LessonTrack
import com.hisaabwise.feature.learn.components.StatChip
import com.hisaabwise.feature.learn.components.StatChipTone
import com.hisaabwise.feature.learn.components.UnitHeader
import com.hisaabwise.feature.learn.components.nodeState
import com.hisaabwise.feature.learn.components.tint
import com.hisaabwise.feature.learn.components.vector

@Composable
fun LearnRoute(viewModel: LearnViewModel, onOpenLesson: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { if (it is LearnEffect.OpenLesson) onOpenLesson(it.lessonId) }
    }
    LearnMapScreen(state, viewModel::dispatch)
}

/** The unit map: stats bar, then the five units and their paths. Port of iOS `LearnView` + `LearnMapPage`. */
@Composable
fun LearnMapScreen(state: LearnState, onIntent: (LearnIntent) -> Unit) {
    SurfaceGround {
        StateView(
            state.content,
            onRetry = { onIntent(LearnIntent.Load) },
            empty = {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.learn_empty), style = HWType.Body, color = HWColors.InkSecondary, textAlign = TextAlign.Center)
                }
            },
        ) { map ->
            LearnMapPage(
                map,
                onOpenGuide = { onIntent(LearnIntent.OpenGuide(it)) },
                onSelect = { onIntent(LearnIntent.PressLesson(it.id, it.state == LessonNodeState.Locked)) },
            )
        }

        if (state.openGuideUnitId != null) {
            val unit = state.openGuideUnit
            if (unit == null) {
                // A unit that went away under an open sheet: close rather than show an empty panel.
                LaunchedEffect(state.openGuideUnitId) { onIntent(LearnIntent.CloseGuide) }
            } else {
                GuideSheet(unit, onClose = { onIntent(LearnIntent.CloseGuide) }) { lesson ->
                    onIntent(LearnIntent.PressLesson(lesson.id, lesson.state == LessonNodeState.Locked))
                }
            }
        }

        HWToast(
            if (state.notice == LearnNotice.LessonLocked) stringResource(R.string.learn_notice_locked) else null,
            onDismiss = { onIntent(LearnIntent.DismissNotice) },
        )
    }
}

@Composable
private fun LearnMapPage(map: LearnMap, onOpenGuide: (String) -> Unit, onSelect: (LessonNode) -> Unit) {
    val title = stringResource(R.string.learn_title)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(Modifier.walkthroughTarget(WalkthroughTarget.LearnPath), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // The stats row is the screen's heading; the design draws no title line.
            Row(
                Modifier.fillMaxWidth().semantics { heading(); contentDescription = title },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HWMark(34.dp)
                StatChip(StatChipTone.Streak, map.progress.streak.display, map.progress.streak.accessibilityLabel)
                Spacer(Modifier.weight(1f))
                StatChip(StatChipTone.Experience, map.progress.xp.display, map.progress.xp.accessibilityLabel)
            }
            val startLabel = stringResource(R.string.learn_start)
            map.units.forEach { unit ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnitHeader(
                        number = unit.content.numberText,
                        caption = unitCaption(unit.content),
                        title = unit.content.title,
                        subtitle = unit.content.subtitle,
                        tint = unit.content.accent.tint(),
                        isUnlocked = unit.isUnlocked,
                        onOpenGuide = { onOpenGuide(unit.id) },
                    )
                    LessonTrack(unit.content.accent.tint(), unit.lessons.map(::node), startLabel, onSelect)
                }
            }
        }
        BottomSpacer()
    }
}

/** "Unit 3 · Debt & Credit" — the band's caption and the guide sheet's, one string. */
@Composable
internal fun unitCaption(unit: CurriculumUnit): String = stringResource(R.string.learn_unit_caption, unit.numberText, unit.subtitle)

internal fun node(lesson: LearnMap.MapLesson) = LessonNode(
    id = lesson.id,
    number = lesson.content.numberText,
    title = lesson.content.title,
    progressLabel = lesson.progress.progressLabel,
    icon = lesson.content.icon.vector(),
    state = lesson.progress.state.nodeState(),
    segments = lesson.progress.segments,
    filledSegments = lesson.progress.filledSegments,
    isNext = lesson.isNext,
)

/** `.sheet` / `.guide-*` — the unit's blurb and its lessons in order. */
@Composable
private fun GuideSheet(unit: LearnMap.MapUnit, onClose: () -> Unit, onSelect: (LessonNode) -> Unit) {
    HWSheet(stringResource(R.string.learn_guide_title), onClose, skipPartiallyExpanded = false) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(unitCaption(unit.content).uppercase(), style = HWType.Eyebrow, color = HWColors.AccentMuted)
            Text(unit.content.title, style = HWType.Subheading, color = HWColors.Ink)
            Text(unit.content.blurb, style = HWType.Body, color = HWColors.InkSecondary)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            unit.lessons.forEach { lesson ->
                val n = node(lesson)
                LessonRow(
                    number = lesson.content.numberText,
                    title = lesson.content.title,
                    // The guide row's second line is the lesson's blurb; the progress sentence is on the node.
                    detail = lesson.content.blurb,
                    state = n.state,
                    isNext = lesson.isNext,
                    tint = unit.content.accent.tint(),
                ) {
                    onClose()
                    onSelect(n)
                }
            }
        }
    }
}
