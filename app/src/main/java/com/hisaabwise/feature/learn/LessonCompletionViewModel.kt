package com.hisaabwise.feature.learn

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.Endpoint

/**
 * The celebration: its load **is** `POST /v1/learn/lessons/{id}/complete`. Offline is `LoadState.Offline`
 * with a retry and nothing queued; a 422 is `Failed`. One idempotency key per completion, reused across
 * retries, and the same results each time. Port of iOS `LessonCompletionViewModel`.
 */
class LessonCompletionViewModel(
    private val api: ApiClient,
    private val request: CompletionRequest,
    private val onScreenUpdate: (LearnPayload) -> Unit,
) : MviViewModel<LessonCompletionState, LessonCompletionIntent, LessonCompletionEffect>(LessonCompletionState()) {

    init {
        dispatch(LessonCompletionIntent.Load)
    }

    override suspend fun handle(intent: LessonCompletionIntent) {
        when (intent) {
            LessonCompletionIntent.Load -> {
                if (current.content is LoadState.Loaded) return
                reduce { copy(content = LoadState.Loading) }
                val loaded = loadState {
                    val completion = api.post<LessonResults, LessonCompletion>(
                        Endpoint.lessonCompletion(request.lessonId),
                        LessonResults(request.results),
                        idempotencyKey = request.idempotencyKey,
                    )
                    // The map re-renders from the same response the celebration is drawn from.
                    onScreenUpdate(completion.screen)
                    completion
                }
                reduce { copy(content = loaded) }
                loaded.valueOrNull?.let { emit(LessonCompletionEffect.Landed(it)) }
            }
        }
    }
}
