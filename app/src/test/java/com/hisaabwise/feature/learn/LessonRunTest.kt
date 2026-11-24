package com.hisaabwise.feature.learn

import com.hisaabwise.feature.learn.components.AnswerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonRunTest {

    private val teach = Step.Teach(heading = "h")
    private val single = Step.SingleChoice("p", listOf("a", "b", "c", "d"), listOf(1), "e")
    private val multi = Step.MultiSelect("p", listOf("a", "b", "c", "d"), listOf(0, 2), "e")
    private val numeric = Step.Numeric("p", answer = 3600, showsCurrencySymbol = true, explanation = "e")

    private fun run(vararg steps: Step) = LessonRun("u1l1", steps.toList())

    // region Tolerance

    @Test
    fun numericToleranceIsStrictlyUnderHalf() {
        assertTrue(LessonRun.isWithinTolerance("3600", 3600))
        assertTrue(LessonRun.isWithinTolerance("3600.0", 3600))
        assertTrue(LessonRun.isWithinTolerance("3600.49", 3600))
        assertTrue(LessonRun.isWithinTolerance("3599.51", 3600))
        assertFalse(LessonRun.isWithinTolerance("3600.5", 3600))
        assertFalse(LessonRun.isWithinTolerance("3599.5", 3600))
        assertFalse(LessonRun.isWithinTolerance("3601", 3600))
    }

    @Test
    fun numericAcceptsCommaDecimalsGroupingAndOtherScripts() {
        assertTrue(LessonRun.isWithinTolerance("3600,4", 3600))
        assertTrue(LessonRun.isWithinTolerance("3,600", 3600))
        assertTrue(LessonRun.isWithinTolerance("٣٦٠٠", 3600))
        assertFalse(LessonRun.isWithinTolerance("abc", 3600))
    }

    @Test
    fun zeroAnswerIsGradable() {
        assertTrue(LessonRun.isWithinTolerance("0", 0))
        assertTrue(LessonRun.isWithinTolerance("0.4", 0))
    }

    // endregion

    // region Grading

    @Test
    fun singleChoiceReplacesAndGrades() {
        var r = run(single).choose(0).choose(1)
        assertEquals(LessonRun.Answer.One(1), r.answer)
        r = r.check()
        assertTrue(r.verdict!!.isCorrect)
        assertEquals(listOf(QuestionResult(0, true)), r.results)
        assertEquals(LessonRun.HEART_COUNT, r.heartsRemaining)
    }

    @Test
    fun multiSelectTogglesAndGradesBySetEquality() {
        var r = run(multi).choose(2).choose(0).choose(1).choose(1)
        assertEquals(LessonRun.Answer.Several(setOf(0, 2)), r.answer)
        r = r.check()
        assertTrue(r.verdict!!.isCorrect)

        val partial = run(multi).choose(0).check()
        assertFalse(partial.verdict!!.isCorrect)
        val extra = run(multi).choose(0).choose(2).choose(3).check()
        assertFalse(extra.verdict!!.isCorrect)
    }

    @Test
    fun outOfRangeOptionsAreIgnored() {
        assertEquals(LessonRun.Answer.None, run(single).choose(9).answer)
        assertEquals(LessonRun.Answer.None, run(multi).choose(-1).answer)
    }

    @Test
    fun gradedQuestionIgnoresFurtherInput() {
        val graded = run(single).choose(0).check()
        assertSame(graded, graded.choose(1))
        assertSame(graded, graded.check())
        val typed = run(numeric).type("3600").check()
        assertSame(typed, typed.type("1"))
    }

    @Test
    fun numericGradesTypedFigure() {
        val right = run(numeric).type("3600").check()
        assertTrue(right.verdict!!.isCorrect)
        assertTrue(right.verdict!!.answers.isEmpty())
        val wrong = run(numeric).type("4000").check()
        assertFalse(wrong.verdict!!.isCorrect)
    }

    @Test
    fun readinessFollowsTheStep() {
        assertTrue(run(teach).isReadyToCheck)
        assertFalse(run(single).isReadyToCheck)
        assertTrue(run(single).choose(0).isReadyToCheck)
        assertFalse(run(multi).isReadyToCheck)
        assertFalse(run(multi).choose(0).choose(0).isReadyToCheck)
        assertFalse(run(numeric).type(".").isReadyToCheck)
        assertTrue(run(numeric).type("1").isReadyToCheck)
        assertTrue(run(single).choose(0).check().isReadyToCheck)
        assertFalse(run(teach).advance().isReadyToCheck)
    }

    @Test
    fun teachingChecksToNothingAndAdvances() {
        val r = run(teach, single)
        assertSame(r, r.check())
        val next = r.advance()
        assertEquals(1, next.index)
        assertEquals(single, next.step)
    }

    // endregion

    // region Hearts and combo

    @Test
    fun threeWrongAnswersEndTheRunAndItDoesNotAdvance() {
        var r = run(single, single, single, single)
        repeat(3) { r = r.choose(0).check().let { checked -> if (checked.isOutOfHearts) checked else checked.advance() } }
        assertTrue(r.isOutOfHearts)
        assertEquals(0, r.heartsRemaining)
        assertEquals("0", r.heartsRemainingText)
        val stuck = r.advance()
        assertEquals(r.index, stuck.index)
        assertFalse(stuck.isFinished)
    }

    @Test
    fun finishingEveryStepIsFinished() {
        var r = run(teach, single)
        r = r.advance().choose(1).check().advance()
        assertTrue(r.isFinished)
        assertNull(r.step)
        assertEquals(1, r.correctCount)
        assertEquals("2", r.stepNumberText)
    }

    @Test
    fun wrongAnswerClearsTheComboAndComboLandsEveryThird() {
        var r = run(*Array(7) { single })
        val milestones = mutableListOf<Int?>()
        // right, right, right (×3), wrong, right, right, right (×3)
        listOf(1, 1, 1, 0, 1, 1, 1).forEach { option ->
            r = r.choose(option).check()
            milestones += r.verdict!!.comboMilestone
            r = r.advance()
        }
        assertEquals(listOf(null, null, 3, null, null, null, 3), milestones)
        assertEquals(2, r.heartsRemaining)
    }

    @Test
    fun comboMilestones() {
        assertNull(LessonRun.comboMilestone(0))
        assertNull(LessonRun.comboMilestone(2))
        assertEquals(3, LessonRun.comboMilestone(3))
        assertNull(LessonRun.comboMilestone(4))
        assertEquals(6, LessonRun.comboMilestone(6))
    }

    // endregion

    // region Player mapping

    @Test
    fun optionStatesBeforeAndAfterGrading() {
        val chosen = run(single).choose(0)
        assertEquals(AnswerState.Chosen, optionState(0, chosen))
        assertEquals(AnswerState.Offered, optionState(1, chosen))
        val graded = chosen.check()
        assertEquals(AnswerState.Wrong, optionState(0, graded))
        assertEquals(AnswerState.Right, optionState(1, graded))
        assertEquals(AnswerState.Offered, optionState(2, graded))
    }

    @Test
    fun headlinesRotateDeterministically() {
        val right = LessonRun.Verdict(true, emptyList(), "", null)
        val wrong = LessonRun.Verdict(false, emptyList(), "", null)
        assertEquals(praise[1], headline(right, 1))
        assertEquals(praise[0], headline(right, 7))
        assertEquals(encouragement[1], headline(wrong, 5))
    }

    // endregion
}
