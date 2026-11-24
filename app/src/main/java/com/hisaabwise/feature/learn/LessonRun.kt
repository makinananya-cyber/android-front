package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.TypedAmount
import kotlin.math.abs

/**
 * One run through one lesson — the app's **one** client-side calculation (invariant 10). A line-for-line
 * port of iOS `LessonRun`: it grades, it never scores. Immutable: every mutation returns a new run.
 *
 * Rules (tested in `LessonRunTest`):
 * - 3 hearts; a wrong answer spends one and clears the combo; the run ends at 0 and does not advance.
 * - The combo lands on every third consecutive right answer.
 * - Choice questions grade by **set equality**.
 * - Numeric answers are read at hundredths and are right when strictly `< 0.50` away.
 */
data class LessonRun(
    val lessonId: String,
    val steps: List<Step>,
    val index: Int = 0,
    val heartsRemaining: Int = HEART_COUNT,
    val consecutiveCorrect: Int = 0,
    val results: List<QuestionResult> = emptyList(),
    val answer: Answer = Answer.None,
    val verdict: Verdict? = null,
) {
    sealed interface Answer {
        data object None : Answer
        data class One(val option: Int) : Answer
        data class Several(val options: Set<Int>) : Answer
        data class Typed(val text: String) : Answer
    }

    data class Verdict(
        val isCorrect: Boolean,
        /** Right option indices; empty on a numeric question. */
        val answers: List<Int>,
        val explanation: String,
        val comboMilestone: Int?,
    )

    val step: Step? get() = steps.getOrNull(index)
    val stepCount: Int get() = steps.size

    /** Every step behind the reader — the state that submits the lesson. Never true on an empty heart. */
    val isFinished: Boolean get() = index >= steps.size && !isOutOfHearts
    val isOutOfHearts: Boolean get() = heartsRemaining <= 0

    val isReadyToCheck: Boolean
        get() {
            val step = step ?: return false
            if (verdict != null) return true
            return when (step) {
                is Step.Teach -> true
                is Step.SingleChoice -> answer is Answer.One
                is Step.MultiSelect -> (answer as? Answer.Several)?.options?.isNotEmpty() == true
                is Step.Numeric -> (answer as? Answer.Typed)?.text?.any(::isNumberChar) == true
            }
        }

    val correctCount: Int get() = results.count { it.isCorrect }

    val stepNumberText: String get() = minOf(index + 1, stepCount).toString()
    val stepCountText: String get() = stepCount.toString()
    val heartsRemainingText: String get() = maxOf(heartsRemaining, 0).toString()

    /** Single-choice replaces; multi-select toggles. Ignored once graded. */
    fun choose(option: Int): LessonRun {
        if (verdict != null) return this
        return when (val step = step) {
            is Step.SingleChoice ->
                if (option in step.options.indices) copy(answer = Answer.One(option)) else this
            is Step.MultiSelect -> {
                if (option !in step.options.indices) return this
                val chosen = (answer as? Answer.Several)?.options ?: emptySet()
                copy(answer = Answer.Several(if (option in chosen) chosen - option else chosen + option))
            }
            else -> this
        }
    }

    /** What was typed, exactly as typed. Parsed at grading time. */
    fun type(text: String): LessonRun =
        if (verdict == null && step is Step.Numeric) copy(answer = Answer.Typed(text)) else this

    fun check(): LessonRun {
        if (verdict != null) return this
        val step = step ?: return this
        val isCorrect: Boolean
        val answers: List<Int>
        val explanation: String
        when (step) {
            is Step.Teach -> return this
            is Step.SingleChoice -> {
                val chosen = (answer as? Answer.One)?.option ?: return this
                isCorrect = step.answers.toSet() == setOf(chosen)
                answers = step.answers
                explanation = step.explanation
            }
            is Step.MultiSelect -> {
                val chosen = (answer as? Answer.Several)?.options ?: return this
                if (chosen.isEmpty()) return this
                isCorrect = step.answers.toSet() == chosen
                answers = step.answers
                explanation = step.explanation
            }
            is Step.Numeric -> {
                val text = (answer as? Answer.Typed)?.text ?: return this
                if (!text.any(::isNumberChar)) return this
                isCorrect = isWithinTolerance(text, step.answer)
                answers = emptyList()
                explanation = step.explanation
            }
        }
        val consecutive = if (isCorrect) consecutiveCorrect + 1 else 0
        return copy(
            results = results + QuestionResult(index, isCorrect),
            consecutiveCorrect = consecutive,
            heartsRemaining = if (isCorrect) heartsRemaining else heartsRemaining - 1,
            verdict = Verdict(isCorrect, answers, explanation, comboMilestone(consecutive)),
        )
    }

    /** Moves on, clearing the answer and the verdict. A run with no hearts left does not advance. */
    fun advance(): LessonRun {
        if (isOutOfHearts || index >= steps.size) return this
        return copy(index = index + 1, answer = Answer.None, verdict = null)
    }

    companion object {
        const val HEART_COUNT = 3
        const val COMBO_INTERVAL = 3
        const val TOLERANCE_IN_HUNDREDTHS = 50L
        private const val TYPED_EXPONENT = 2
        private const val HUNDREDTHS_PER_UNIT = 100L

        val heartCountText: String get() = HEART_COUNT.toString()

        fun isWithinTolerance(typed: String, answer: Int): Boolean {
            val hundredths = TypedAmount.scaled(typed, TYPED_EXPONENT) ?: return false
            return try {
                val correct = Math.multiplyExact(answer.toLong(), HUNDREDTHS_PER_UNIT)
                abs(Math.subtractExact(hundredths, correct)) < TOLERANCE_IN_HUNDREDTHS
            } catch (_: ArithmeticException) {
                false
            }
        }

        fun comboMilestone(consecutive: Int): Int? =
            if (consecutive >= COMBO_INTERVAL && consecutive % COMBO_INTERVAL == 0) consecutive else null

        /** Swift `Character.isNumber`: decimal digits, letter numbers and other numbers in any script. */
        fun isNumberChar(c: Char): Boolean = when (Character.getType(c).toByte()) {
            Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER -> true
            else -> false
        }
    }
}
