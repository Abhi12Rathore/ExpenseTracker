package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.TextLine
import java.util.Locale

/**
 * Finds the merchant: the largest print among the first lines, leaving out headings such as
 * "TAX INVOICE", phone numbers and lines that are mostly digits.
 */
internal class BillTitleParser(private val keywords: BillKeywords) {

    fun parse(lines: List<TextLine>): String? =
        lines.take(LINES_CHECKED)
            .filter(::isName)
            // Of lines equally tall, the first
            .maxByOrNull { it.height }
            ?.let { titleCase(it.text).take(keywords.titleMaxLength).trim() }
            ?.takeIf { it.isNotEmpty() }

    private fun isName(line: TextLine): Boolean {
        val letters = line.text.count(Char::isLetter)
        val digits = line.text.count(Char::isDigit)
        val words = BillTokens.words(line.text)

        return letters >= MIN_LETTERS &&
            digits < letters &&
            keywords.titleStopWords.none { BillTokens.hasPhrase(words, it) }
    }

    /** "CAFE COFFEE DAY" as "Cafe Coffee Day", without the decoration around it. */
    private fun titleCase(text: String): String =
        text.trim { !it.isLetterOrDigit() }
            .split(WHITESPACE)
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
            }

    private companion object {
        const val LINES_CHECKED = 3
        const val MIN_LETTERS = 2
        val WHITESPACE = Regex("""\s+""")
    }
}
