package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.CurrencyToken
import com.hisaabwise.core.model.Stat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// region Curriculum — GET /v1/curriculum (cacheable, ETag; port of iOS `Curriculum`)

/**
 * The five units, fifteen lessons and 124 steps. Carries no per-user state (invariant 8): progress
 * arrives in [LearnPayload] and the two are joined by lesson id in [LearnMap].
 */
@Serializable
data class Curriculum(val units: List<CurriculumUnit>) {
    val allLessons: List<CurriculumLesson> get() = units.flatMap { it.lessons }
    fun lesson(id: String): CurriculumLesson? = allLessons.firstOrNull { it.id == id }
}

/** A unit's accent, by name. Unknown names degrade to [Sky] (the design's own fallback). */
@Serializable
enum class UnitAccentName {
    @SerialName("sun") Sun,
    @SerialName("mint") Mint,
    @SerialName("coral") Coral,
    @SerialName("sky") Sky,
    @SerialName("violet") Violet,
}

@Serializable
data class CurriculumUnit(
    val id: String,
    val number: Int,
    val accent: UnitAccentName = UnitAccentName.Sky,
    val title: String,
    val subtitle: String,
    val blurb: String,
    val lessons: List<CurriculumLesson>,
) {
    val numberText: String get() = number.toString()
}

/** The design's fifteen lesson glyphs. Unknown names fall back to [Book]. */
@Serializable
enum class LessonIcon {
    @SerialName("wallet") Wallet,
    @SerialName("clock") Clock,
    @SerialName("split") Split,
    @SerialName("flow") Flow,
    @SerialName("pie") Pie,
    @SerialName("shield") Shield,
    @SerialName("bank") Bank,
    @SerialName("gauge") Gauge,
    @SerialName("scales") Scales,
    @SerialName("steps") Steps,
    @SerialName("umbrella") Umbrella,
    @SerialName("lock") Lock,
    @SerialName("spark") Spark,
    @SerialName("layers") Layers,
    @SerialName("grid") Grid,
    @SerialName("book") Book,
}

@Serializable
data class CurriculumLesson(
    val id: String,
    val number: Int,
    val icon: LessonIcon = LessonIcon.Book,
    val title: String,
    val blurb: String,
    val steps: List<Step>,
) {
    val numberText: String get() = number.toString()
    val questionCount: Int get() = steps.count { it.isQuestion }
}

/**
 * One step: teaching or a question, discriminated on `kind`.
 *
 * **It refuses to guess** (iOS parity): an unknown `kind` fails the decode, because a wrong guess
 * would grade the wrong thing. The screen then renders `LoadState.Failed`.
 */
@Serializable(with = StepSerializer::class)
sealed interface Step {
    val isQuestion: Boolean get() = this !is Teach

    @Serializable
    data class Teach(
        val heading: String,
        val paragraphs: List<String> = emptyList(),
        val list: List<Entry> = emptyList(),
        /** Paragraphs drawn **after** the list — the position is load-bearing. */
        val afterList: List<String> = emptyList(),
        val example: Example? = null,
        val tip: String? = null,
    ) : Step

    /** Fields shared by single-choice and multi-select questions. */
    sealed interface Choice : Step {
        val prompt: String
        val options: List<String>
        /** Indices into [options] that are right, ascending. */
        val answers: List<Int>
        val explanation: String
    }

    @Serializable
    data class SingleChoice(
        override val prompt: String,
        override val options: List<String>,
        override val answers: List<Int>,
        override val explanation: String,
    ) : Choice

    @Serializable
    data class MultiSelect(
        override val prompt: String,
        override val options: List<String>,
        override val answers: List<Int>,
        override val explanation: String,
    ) : Choice

    @Serializable
    data class Numeric(
        val prompt: String,
        val answer: Int,
        val showsCurrencySymbol: Boolean = false,
        val explanation: String,
    ) : Step {
        val answerText: String get() = answer.toString()
    }

    @Serializable
    data class Entry(val term: String, val detail: String)

    @Serializable
    data class Example(val heading: String, val body: String)
}

object StepSerializer : JsonContentPolymorphicSerializer<Step>(Step::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Step> =
        when (val kind = element.jsonObject["kind"]?.jsonPrimitive?.content) {
            "teach" -> Step.Teach.serializer()
            "singleChoice" -> Step.SingleChoice.serializer()
            "multiSelect" -> Step.MultiSelect.serializer()
            "numeric" -> Step.Numeric.serializer()
            else -> throw SerializationException("Unknown step kind: $kind")
        }
}

// endregion

// region Learn screen — GET /v1/screens/learn (per-user, never cached; port of iOS `LearnScreen`)

@Serializable
data class LearnPayload(
    val streak: Stat,
    val xp: Stat,
    /** Where the START badge goes; null once every lesson is finished. */
    val nextLesson: NextLesson? = null,
    /** What `{c}` becomes in the lesson steps. */
    val currencyToken: CurrencyToken,
    val units: List<UnitProgress>,
    val lessons: List<LessonProgress>,
) {
    fun lesson(id: String): LessonProgress? = lessons.firstOrNull { it.id == id }
    fun unit(id: String): UnitProgress? = units.firstOrNull { it.id == id }
    fun isNext(lessonId: String): Boolean = nextLesson?.lessonId == lessonId
}

@Serializable
data class NextLesson(val unitId: String, val lessonId: String, val title: String)

@Serializable
data class UnitProgress(val id: String, val isUnlocked: Boolean)

@Serializable
data class LessonProgress(
    val id: String,
    /** No default on purpose: an unknown state fails the screen rather than guessing a lock (iOS parity). */
    val state: LessonState,
    val segments: Int,
    val filledSegments: Int,
    val progressLabel: String,
) {
    val isOpen: Boolean get() = state != LessonState.Locked
}

@Serializable
enum class LessonState {
    @SerialName("completed") Completed,
    @SerialName("available") Available,
    @SerialName("locked") Locked,
}

// endregion

// region Lesson writes — POST /v1/learn/lessons/{id}/complete

/** One question's outcome as submitted. The server recomputes XP from these (invariant 10). */
@Serializable
data class QuestionResult(val stepIndex: Int, val isCorrect: Boolean)

@Serializable
data class LessonResults(val results: List<QuestionResult>)

/** The completion screen, fully computed, with the updated Learn screen inside it. */
@Serializable
data class LessonCompletion(
    val isFirstCompletion: Boolean,
    val xpEarned: Stat,
    val accuracy: Stat,
    val streakLine: String,
    val week: List<Day>,
    val screen: LearnPayload,
) {
    @Serializable
    data class Day(
        val label: String,
        val isComplete: Boolean,
        val isToday: Boolean,
        val accessibilityLabel: String,
    )
}

// endregion
