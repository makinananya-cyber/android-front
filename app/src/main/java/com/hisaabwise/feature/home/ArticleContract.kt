package com.hisaabwise.feature.home

import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.mvi.UiEffect
import com.hisaabwise.core.mvi.UiIntent
import com.hisaabwise.core.mvi.UiState

/** One article page. The title, glyph and accent travel with the teaser it was opened from. */
data class ArticleState(
    val title: String,
    val icon: ArticleIcon,
    val accent: Int,
    val content: LoadState<ArticleBody> = LoadState.Loading,
) : UiState

sealed interface ArticleIntent : UiIntent {
    data object Load : ArticleIntent
    data class OpenSource(val url: String) : ArticleIntent
    data object Back : ArticleIntent
}

sealed interface ArticleEffect : UiEffect {
    data class OpenUrl(val url: String) : ArticleEffect
    data object Close : ArticleEffect
}
