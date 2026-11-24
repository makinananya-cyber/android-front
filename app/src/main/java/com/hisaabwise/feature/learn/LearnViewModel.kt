package com.hisaabwise.feature.learn

import androidx.lifecycle.viewModelScope
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.ContentLoader
import com.hisaabwise.core.network.Endpoint
import com.hisaabwise.core.session.ScreenRepaint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Learn's unit map — two reads (curriculum with ETag + per-user screen), run concurrently and joined
 * by lesson id. Either failing fails the screen. Port of iOS `LearnViewModel`.
 */
class LearnViewModel(
    private val api: ApiClient,
    private val content: ContentLoader,
    repaint: ScreenRepaint,
) : MviViewModel<LearnState, LearnIntent, LearnEffect>(LearnState()) {

    /** Held so a write's response can be joined against it without refetching ~100 KB. */
    private var curriculum: Curriculum? = null

    init {
        dispatch(LearnIntent.Load)
        viewModelScope.launch { repaint.requests.collect { dispatch(LearnIntent.Load) } }
    }

    /** The material for the player, looked up once from the current map. */
    fun material(lessonId: String): LearnMap.Material? = state.value.content.valueOrNull?.material(lessonId)

    override suspend fun handle(intent: LearnIntent) {
        when (intent) {
            LearnIntent.Load -> {
                reduce { copy(content = LoadState.Loading) }
                val loaded = loadState(isEmpty = { it.units.isEmpty() }) {
                    coroutineScope {
                        val curriculumAsync = async { content.load<Curriculum>(ContentResource.Curriculum) }
                        val progressAsync = async { api.get<LearnPayload>(Endpoint.SCREEN_LEARN) }
                        val cur = curriculumAsync.await()
                        curriculum = cur
                        LearnMap.join(cur, progressAsync.await())
                    }
                }
                reduce { copy(content = loaded) }
            }
            is LearnIntent.OpenGuide -> reduce { copy(notice = null, openGuideUnitId = intent.unitId) }
            LearnIntent.CloseGuide -> reduce { copy(openGuideUnitId = null) }
            is LearnIntent.PressLesson -> {
                if (intent.isLocked) {
                    reduce { copy(notice = LearnNotice.LessonLocked) }
                } else if (material(intent.lessonId) != null) {
                    reduce { copy(notice = null, openGuideUnitId = null) }
                    emit(LearnEffect.OpenLesson(intent.lessonId))
                }
            }
            LearnIntent.DismissNotice -> reduce { copy(notice = null) }
            is LearnIntent.ApplyScreen -> {
                val cur = curriculum ?: return
                val map = LearnMap.join(cur, intent.screen)
                reduce { copy(content = if (map.units.isEmpty()) LoadState.Empty else LoadState.Loaded(map)) }
            }
        }
    }
}
