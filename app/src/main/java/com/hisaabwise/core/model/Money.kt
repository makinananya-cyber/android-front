package com.hisaabwise.core.model

import kotlinx.serialization.Serializable

/**
 * A server-formatted amount. The client **never** formats, sums or converts money — it renders
 * [display] exactly as sent (invariant 1). [minor] and [exponent] exist for editing an amount
 * the user already authored (e.g. pre-filling a field), never for arithmetic shown on screen.
 */
@Serializable
data class Money(
    val minor: Long,
    val currency: String,
    val exponent: Int,
    val display: String,
) {
    init {
        require(currency.length == 3 && currency.all { it in 'A'..'Z' }) { "unknown currency $currency" }
        require(exponent in 0..4) { "unsupported exponent $exponent" }
        require(display.isNotBlank()) { "missing display string" }
    }
}

/** Money as the client sends it: integer minor units in the currency the user typed in. */
@Serializable
data class MoneyAmount(val minor: Long, val currency: String)

/** The currency the user is typing amounts in, with what to show beside the field. */
@Serializable
data class AuthoringCurrency(
    val code: String,
    val symbol: String,
    val displayCode: String,
    val exponent: Int,
)

/** Replaces the `{c}` placeholder in tips and lesson text with the user's currency token. */
@JvmInline
@Serializable
value class CurrencyToken(val token: String) {
    fun resolve(text: String): String = text.replace(PLACEHOLDER, token)

    companion object {
        const val PLACEHOLDER = "{c}"
    }
}

/** A labelled number the server has already formatted (streak, XP, accuracy…). */
@Serializable
data class Stat(val value: Int, val display: String, val accessibilityLabel: String)
