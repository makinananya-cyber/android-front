package com.hisaabwise.feature.home

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Server content with inline markdown emphasis (`**bold**`, `*italic*`), as an [AnnotatedString].
 * Port of iOS `HWMarkdown`: inline only, and anything that does not parse stays literal — a stray
 * asterisk reads as a stray asterisk.
 */
object HWMarkdown {

    data class Run(val text: String, val bold: Boolean = false, val italic: Boolean = false)

    /** The text split into styled runs. Pure, so it is unit-tested. */
    fun runs(text: String): List<Run> {
        val out = mutableListOf<Run>()
        parse(text, bold = false, italic = false, out)
        // Merge neighbours with the same style.
        return out.fold(mutableListOf()) { acc, run ->
            val last = acc.lastOrNull()
            if (last != null && last.bold == run.bold && last.italic == run.italic) acc[acc.lastIndex] = last.copy(text = last.text + run.text)
            else if (run.text.isNotEmpty()) acc += run
            acc
        }
    }

    private fun parse(text: String, bold: Boolean, italic: Boolean, out: MutableList<Run>) {
        val plain = StringBuilder()
        var i = 0
        fun flush() {
            if (plain.isNotEmpty()) out += Run(plain.toString(), bold, italic)
            plain.clear()
        }
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2 && !text[i + 2].isWhitespace()) {
                    flush()
                    parse(text.substring(i + 2, end), bold = true, italic = italic, out)
                    i = end + 2
                    continue
                }
            } else if (text[i] == '*') {
                val end = findItalicEnd(text, i + 1)
                if (end > i + 1 && !text[i + 1].isWhitespace()) {
                    flush()
                    parse(text.substring(i + 1, end), bold = bold, italic = true, out)
                    i = end + 1
                    continue
                }
            }
            plain.append(text[i])
            i++
        }
        flush()
    }

    /** The next single `*` (skipping `**` pairs, which belong to bold runs inside the italic). */
    private fun findItalicEnd(text: String, from: Int): Int {
        var j = from
        while (j < text.length) {
            if (text.startsWith("**", j)) {
                val close = text.indexOf("**", j + 2)
                if (close < 0) return -1
                j = close + 2
                continue
            }
            if (text[j] == '*') return j
            j++
        }
        return -1
    }

    fun annotated(text: String): AnnotatedString = buildAnnotatedString {
        runs(text).forEach { run ->
            val style = SpanStyle(
                fontWeight = if (run.bold) FontWeight.ExtraBold else null,
                fontStyle = if (run.italic) FontStyle.Italic else null,
            )
            if (run.bold || run.italic) withStyle(style) { append(run.text) } else append(run.text)
        }
    }
}
