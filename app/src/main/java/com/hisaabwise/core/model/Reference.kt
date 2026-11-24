package com.hisaabwise.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Country(val code: String, val dialCode: String, val name: String)

@Serializable
data class Currency(val code: String, val name: String, val symbol: String, val exponent: Int = 2)

@Serializable
data class SecurityQuestion(val id: String, val text: String)

@Serializable
data class CountryList(val countries: List<Country>)

@Serializable
data class CurrencyList(val currencies: List<Currency>)

@Serializable
data class SecurityQuestionList(val questions: List<SecurityQuestion>)

@Serializable
data class Tip(val id: String, val text: String)

@Serializable
data class TipList(val tips: List<Tip>)

@Serializable
data class PicklistOption(val id: String, val name: String, val opensFreeText: Boolean = false)

@Serializable
data class Picklists(val transport: List<PicklistOption>, val other: List<PicklistOption>)

/** Cacheable, anonymous, ETag-revalidated content. Port of iOS `ContentResource`. */
sealed class ContentResource(val key: String, val path: String) {
    data object Countries : ContentResource("countries", "/v1/content/reference/countries")
    data object Currencies : ContentResource("currencies", "/v1/content/reference/currencies")
    data object SecurityQuestions : ContentResource("securityQuestions", "/v1/content/security-questions")
    data object Tips : ContentResource("tips", "/v1/content/tips")
    data object Picklists : ContentResource("picklists", "/v1/content/picklists")
    data object Curriculum : ContentResource("curriculum", "/v1/curriculum")
    class Article(id: String) : ContentResource("article-${sanitise(id)}", "/v1/content/articles/${sanitise(id)}")

    companion object {
        fun sanitise(id: String) = id.filter { it.isLetterOrDigit() || it == '-' }
    }
}
