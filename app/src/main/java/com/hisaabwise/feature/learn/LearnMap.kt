package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.CurrencyToken

/**
 * The unit map: the curriculum joined to this reader's progress **by lesson id** — a lookup, not a
 * calculation. Port of iOS `LearnMap`.
 *
 * - A lesson the payload says nothing about is skipped.
 * - A unit whose every lesson went missing is dropped.
 * - A unit the payload does not mention reads as locked.
 */
data class LearnMap(val units: List<MapUnit>, val progress: LearnPayload) {

    data class MapUnit(val content: CurriculumUnit, val isUnlocked: Boolean, val lessons: List<MapLesson>) {
        val id: String get() = content.id
    }

    data class MapLesson(val content: CurriculumLesson, val progress: LessonProgress, val isNext: Boolean) {
        val id: String get() = content.id
        val isOpen: Boolean get() = progress.isOpen
    }

    /** Everything the player needs, taken once when a lesson opens. */
    data class Material(val unit: MapUnit, val lesson: MapLesson, val currencyToken: CurrencyToken)

    val allLessons: List<MapLesson> get() = units.flatMap { it.lessons }

    fun unit(id: String): MapUnit? = units.firstOrNull { it.id == id }

    fun material(lessonId: String): Material? {
        for (unit in units) {
            val lesson = unit.lessons.firstOrNull { it.id == lessonId } ?: continue
            return Material(unit, lesson, progress.currencyToken)
        }
        return null
    }

    companion object {
        fun join(curriculum: Curriculum, progress: LearnPayload): LearnMap {
            val units = curriculum.units.mapNotNull { unit ->
                val lessons = unit.lessons.mapNotNull { lesson ->
                    progress.lesson(lesson.id)?.let { MapLesson(lesson, it, progress.isNext(lesson.id)) }
                }
                if (lessons.isEmpty()) null
                else MapUnit(unit, progress.unit(unit.id)?.isUnlocked ?: false, lessons)
            }
            return LearnMap(units, progress)
        }
    }
}
