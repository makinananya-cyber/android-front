package com.hisaabwise.feature.learn

import com.hisaabwise.core.mvi.MviViewModel
import java.util.UUID

/**
 * The lesson player: the run and what it can become — an exhausted set of hearts, or a completion.
 * Makes no request: the submission is the completion screen's own load. Port of iOS `LessonPlayerViewModel`.
 */
class LessonPlayerViewModel(
    private val material: LearnMap.Material,
) : MviViewModel<LessonPlayerState, LessonPlayerIntent, LessonPlayerEffect>(
    LessonPlayerState(material, freshRun(material)),
) {

    override suspend fun handle(intent: LessonPlayerIntent) {
        when (intent) {
            is LessonPlayerIntent.Choose -> reduce { copy(run = run.choose(intent.option)) }
            // Local input validation, not a calculation: digits in any script and either separator.
            is LessonPlayerIntent.Type -> reduce {
                copy(run = run.type(intent.text.filter { LessonRun.isNumberChar(it) || it == '.' || it == ',' }))
            }
            LessonPlayerIntent.Primary -> primary()
            LessonPlayerIntent.Restart -> reduce { copy(run = freshRun(material), comboMilestone = null) }
            LessonPlayerIntent.DismissCombo -> reduce { copy(comboMilestone = null) }
        }
    }

    private fun primary() {
        val run = current.run
        if (!run.isReadyToCheck) return
        if (run.step?.isQuestion == true && run.verdict == null) {
            val checked = run.check()
            reduce { copy(run = checked, comboMilestone = checked.verdict?.comboMilestone) }
            checked.verdict?.let { emit(LessonPlayerEffect.Graded(it.isCorrect)) }
            return
        }
        val advanced = run.advance()
        reduce { copy(run = advanced) }
        if (advanced.isFinished && current.completion == null) {
            reduce {
                copy(completion = CompletionRequest(advanced.lessonId, advanced.results, UUID.randomUUID().toString()))
            }
        }
    }

    companion object {
        private fun freshRun(material: LearnMap.Material) = LessonRun(material.lesson.id, material.lesson.content.steps)
    }
}
