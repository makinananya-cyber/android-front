package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.CurrencyToken
import com.hisaabwise.core.network.HWJson
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnModelsTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    private val curriculum by lazy { HWJson.decodeFromString(Curriculum.serializer(), fixture("curriculum.json")) }

    private fun screen(name: String) = HWJson.decodeFromString(LearnPayload.serializer(), fixture(name))

    @Test
    fun curriculumCountsAreExact() {
        val steps = curriculum.allLessons.flatMap { it.steps }
        assertEquals(5, curriculum.units.size)
        assertEquals(15, curriculum.allLessons.size)
        assertEquals(124, steps.size)
        assertEquals(58, steps.count { it is Step.Teach })
        assertEquals(66, steps.count { it.isQuestion })
        assertEquals(50, steps.count { it is Step.SingleChoice })
        assertEquals(14, steps.count { it is Step.Numeric })
        assertEquals(2, steps.count { it is Step.MultiSelect })
    }

    @Test
    fun curriculumKeepsCurrencyTokensAndAnswerKeys() {
        val first = curriculum.lesson("u1l1")!!
        assertEquals(LessonIcon.Wallet, first.icon)
        assertEquals(UnitAccentName.Sun, curriculum.units.first().accent)
        val numeric = first.steps.filterIsInstance<Step.Numeric>().first()
        assertEquals(3600, numeric.answer)
        assertTrue(numeric.showsCurrencySymbol)
        assertTrue(numeric.prompt.contains(CurrencyToken.PLACEHOLDER))
        curriculum.allLessons.flatMap { it.steps }.filterIsInstance<Step.Choice>().forEach { q ->
            assertTrue(q.answers.isNotEmpty())
            q.answers.forEach { assertTrue(it in q.options.indices) }
        }
        val single = curriculum.allLessons.flatMap { it.steps }.filterIsInstance<Step.SingleChoice>()
        assertTrue(single.all { it.answers.size == 1 })
    }

    @Test
    fun unknownStepKindFailsTheDecode() {
        val json = """{"units":[{"id":"u1","number":1,"accent":"sun","title":"t","subtitle":"s","blurb":"b",
            "lessons":[{"id":"u1l1","number":1,"icon":"wallet","title":"t","blurb":"b","steps":[{"kind":"video"}]}]}]}"""
        assertThrows(SerializationException::class.java) { HWJson.decodeFromString(Curriculum.serializer(), json) }
    }

    @Test
    fun unknownAccentAndIconFallBack() {
        val json = """{"units":[{"id":"u1","number":1,"accent":"plaid","title":"t","subtitle":"s","blurb":"b",
            "lessons":[{"id":"u1l1","number":1,"icon":"rocket","title":"t","blurb":"b","steps":[]}]}]}"""
        val decoded = HWJson.decodeFromString(Curriculum.serializer(), json)
        assertEquals(UnitAccentName.Sky, decoded.units[0].accent)
        assertEquals(LessonIcon.Book, decoded.units[0].lessons[0].icon)
    }

    @Test
    fun learnScreensDecode() {
        val inProgress = screen("learn-in-progress.json")
        assertEquals(4, inProgress.streak.value)
        assertEquals("u1l3", inProgress.nextLesson?.lessonId)
        assertEquals(CurrencyToken("₹"), inProgress.currencyToken)
        assertEquals(LessonState.Available, inProgress.lesson("u1l3")?.state)
        assertEquals(15, inProgress.lessons.size)

        val firstRun = screen("learn-first-run.json")
        assertNotNull(firstRun.nextLesson)
        val complete = screen("learn-complete.json")
        assertNull(complete.nextLesson)
        assertTrue(complete.lessons.all { it.state == LessonState.Completed })
    }

    @Test
    fun ringSegmentsAgreeWithTheCurriculum() {
        val payload = screen("learn-in-progress.json")
        curriculum.allLessons.forEach { lesson ->
            assertEquals(lesson.id, lesson.questionCount, payload.lesson(lesson.id)?.segments)
        }
    }

    @Test
    fun completionsDecode() {
        val completed = HWJson.decodeFromString(LessonCompletion.serializer(), fixture("lesson-completed.json"))
        assertTrue(completed.isFirstCompletion)
        assertEquals("+50", completed.xpEarned.display)
        assertEquals(7, completed.week.size)
        assertEquals(1, completed.week.count { it.isToday })
        assertEquals(5, completed.screen.streak.value)

        val revisited = HWJson.decodeFromString(LessonCompletion.serializer(), fixture("lesson-revisited.json"))
        assertFalse(revisited.isFirstCompletion)
        assertEquals(0, revisited.xpEarned.value)
    }

    @Test
    fun joinIsALookup() {
        val payload = screen("learn-in-progress.json")
        val map = LearnMap.join(curriculum, payload)
        assertEquals(5, map.units.size)
        assertEquals(15, map.allLessons.size)
        assertEquals(listOf("u1l3"), map.allLessons.filter { it.isNext }.map { it.id })
        assertTrue(map.unit("u1")!!.isUnlocked)
        assertFalse(map.unit("u2")!!.isUnlocked)
        val material = map.material("u1l3")!!
        assertEquals("u1", material.unit.id)
        assertEquals(payload.currencyToken, material.currencyToken)
        assertNull(map.material("nope"))
    }

    @Test
    fun joinSkipsLessonsThePayloadOmitsAndDropsEmptyUnits() {
        val payload = screen("learn-in-progress.json")
        val trimmed = payload.copy(
            lessons = payload.lessons.filterNot { it.id == "u1l2" || it.id.startsWith("u5") },
            units = payload.units.filterNot { it.id == "u4" },
        )
        val map = LearnMap.join(curriculum, trimmed)
        assertEquals(4, map.units.size)
        assertNull(map.unit("u5"))
        assertEquals(listOf("u1l1", "u1l3"), map.unit("u1")!!.lessons.map { it.id })
        // A unit the payload does not mention reads as locked.
        assertFalse(map.unit("u4")!!.isUnlocked)
    }

    @Test
    fun unknownLessonStateFailsTheDecode() {
        val bad = fixture("learn-in-progress.json").replaceFirst("\"available\"", "\"paused\"")
        assertThrows(Exception::class.java) { HWJson.decodeFromString(LearnPayload.serializer(), bad) }
    }

    @Test
    fun resultsEncodeAsTheServerExpects() {
        val body = HWJson.encodeToString(LessonResults.serializer(), LessonResults(listOf(QuestionResult(4, true))))
        assertEquals("""{"results":[{"stepIndex":4,"isCorrect":true}]}""", body)
    }
}
