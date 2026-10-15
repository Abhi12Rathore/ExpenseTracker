package com.example.expensetrackerpro.domain.usecase.quickadd

import java.util.Locale

/** One piece of a quick-add sentence. [text] is as typed; [lower] is what the rules compare. */
internal data class Token(val text: String, val kind: Kind) {

    val lower: String = text.lowercase(Locale.ROOT)

    enum class Kind {
        /** Letters: "swiggy", "kal". */
        WORD,

        /** Digits, perhaps with comma groups, decimals and letters joined on: "1,05,000", "4.5k", "12th". */
        NUMBER,

        /** A comma, semicolon or new line: where a title can end and a note begin. */
        BREAK,

        /** Any other mark, such as "₹" or "/". */
        SYMBOL
    }
}

/** A number as typed, split into its digits and what is joined on after them. */
internal data class NumberParts(val digits: String, val suffix: String)

internal object QuickAddTokens {

    // Letters joined on after the digits stay with the number, as in "4.5k", "12th" or "8pm"
    private val TOKEN = Regex("""\d+(?:,\d+)*(?:\.\d+)?\p{L}*|\p{L}+(?:['’]\p{L}+)*|[,;\n]|[^\s\p{L}\d]""")
    private val NUMBER = Regex("""(\d+(?:,\d+)*(?:\.\d+)?)(\p{L}*)""")

    // Groups of three ("105,000"), or the Indian three then twos ("1,05,000")
    private val GROUPED = Regex("""\d{1,3}(?:,\d{3})+|\d{1,2}(?:,\d{2})*,\d{3}""")

    fun tokenize(text: String): List<Token> =
        TOKEN.findAll(text).map { match ->
            val value = match.value
            val kind = when {
                value[0].isDigit() -> Token.Kind.NUMBER
                value[0].isLetter() -> Token.Kind.WORD
                value in BREAKS -> Token.Kind.BREAK
                else -> Token.Kind.SYMBOL
            }
            Token(value, kind)
        }.toList()

    /** The parts of a number token, or null when its commas don't group digits, as in "450,20". */
    fun numberParts(token: Token): NumberParts? {
        val match = NUMBER.matchEntire(token.lower) ?: return null
        val (number, suffix) = match.destructured
        val whole = number.substringBefore('.')
        if (',' in whole && !GROUPED.matches(whole)) return null

        return NumberParts(digits = number.replace(",", ""), suffix = suffix)
    }

    private val BREAKS = setOf(",", ";", "\n")
}
