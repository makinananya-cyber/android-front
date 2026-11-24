package com.hisaabwise.core.model

/**
 * Parses what the user typed into an amount field into integer minor units.
 * A line-for-line port of iOS `TypedAmount` — keep them in step.
 *
 * - Digits in any script are accepted.
 * - The last `.` or `,` is the decimal separator if at most [exponent] digits follow it.
 * - Otherwise the text is accepted only if it is correctly grouped in threes ("1,250,000").
 * - Zero, empty input and overflow give `null`.
 */
object TypedAmount {

    fun minor(text: String, exponent: Int): Long? {
        val scaled = scaled(text, exponent) ?: return null
        return if (scaled > 0) scaled else null
    }

    fun scaled(text: String, exponent: Int): Long? {
        if (exponent < 0) return null
        val runs = mutableListOf(StringBuilder())
        for (ch in text) {
            when {
                Character.isDigit(ch) -> runs.last().append(Character.digit(ch, 10))
                ch == '.' || ch == ',' -> runs.add(StringBuilder())
            }
        }
        val digits = runs.joinToString("")
        if (digits.isEmpty()) return null
        val whole = digits.toLongOrNull() ?: return null
        val fraction = when {
            runs.size == 1 -> 0
            runs.last().length <= exponent -> runs.last().length
            isGrouped(runs) -> 0
            else -> return null
        }
        var scaled = whole
        repeat(exponent - fraction) {
            scaled = try {
                Math.multiplyExact(scaled, 10L)
            } catch (_: ArithmeticException) {
                return null
            }
        }
        return scaled
    }

    private fun isGrouped(runs: List<StringBuilder>): Boolean {
        val first = runs.firstOrNull() ?: return false
        if (first.length !in 1..3) return false
        return runs.drop(1).all { it.length == 3 }
    }

    /** Minor units back to plain text for an editable field ("1250.5"), without a formatter. */
    fun major(minor: Long, exponent: Int): String {
        if (exponent <= 0) return minor.toString()
        var divisor = 1L
        repeat(exponent) { divisor *= 10 }
        val units = minor / divisor
        val fraction = kotlin.math.abs(minor % divisor)
        if (fraction == 0L) return units.toString()
        return "$units.${fraction.toString().padStart(exponent, '0')}"
    }
}

/** Advisory password strength (1–4). The only hard rule is 8+ characters. Port of iOS `PasswordStrength`. */
enum class PasswordStrength(val score: Int) {
    None(0), Weak(1), Fair(2), Good(3), Strong(4);

    companion object {
        fun of(password: String): PasswordStrength {
            if (password.isEmpty()) return None
            var score = 0
            if (password.length >= 8) score++
            if (password.length >= 12) score++
            if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
            val hasDigit = password.any { it.isDigit() }
            val hasSymbol = password.any { !it.isLetter() && !it.isDigit() }
            if (hasDigit && hasSymbol) score++
            return entries.first { it.score == score.coerceIn(1, 4) }
        }
    }
}

object Validation {
    const val MIN_PASSWORD_LENGTH = 8
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    fun isEmail(text: String) = EMAIL.matches(text.trim())
}
