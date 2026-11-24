package com.hisaabwise.feature.home

import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.MviViewModel
import com.hisaabwise.core.mvi.loadState
import com.hisaabwise.core.network.ContentLoader

/**
 * One article body, through the ETag'd [ContentLoader] — a second visit is a 304 and an offline visit is
 * the cached copy. An article with no sections is [LoadState.Empty]. Port of iOS `ArticleViewModel`.
 */
class ArticleViewModel(
    private val id: String,
    title: String,
    icon: ArticleIcon,
    accent: Int,
    private val content: ContentLoader,
) : MviViewModel<ArticleState, ArticleIntent, ArticleEffect>(ArticleState(title, icon, accent)) {

    init {
        dispatch(ArticleIntent.Load)
    }

    override suspend fun handle(intent: ArticleIntent) {
        when (intent) {
            ArticleIntent.Load -> {
                reduce { copy(content = LoadState.Loading) }
                val loaded = loadState({ body: ArticleBody -> body.sections.isEmpty() }) {
                    content.load<ArticleBody>(ContentResource.Article(id))
                }
                reduce { copy(content = loaded) }
            }
            is ArticleIntent.OpenSource -> emit(ArticleEffect.OpenUrl(intent.url))
            ArticleIntent.Back -> emit(ArticleEffect.Close)
        }
    }
}
