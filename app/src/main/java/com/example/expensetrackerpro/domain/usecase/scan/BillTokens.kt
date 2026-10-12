package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.Money
import java.math.BigDecimal
import java.util.Locale

/** A number on a bill, read as rupees. */
internal data class AmountToken(
    val amount: Money,
    /**
     * False for a bare number that is too long or too short to be a price, such as a PIN code,
     * an order number or a quantity. It is taken for an amount only where a label says it is one.
     */
    val looksLikeMoney: Boolean
)

/** Splitting a line of a bill into the words and numbers the parsers look for. */
internal object BillTokens {

    /**
     * [text] in lowercase with everything but letters and digits turned into single spaces, and
     * a space at either end: `" sub total 1360 00 "`. A phrase is in it as whole words when
     * `" phrase "` is.
     */
    fun words(text: String): String =
        " " + text.lowercase(Locale.ROOT).replace(NOT_WORD, " ").trim() + " "

    /** Whether [phrase], lowercase words, is in [words] as whole words. */
    fun hasPhrase(words: String, phrase: String): Boolean = " $phrase " in words

    /** Where [phrase] starts in [words], or -1. */
    fun phraseIndex(words: String, phrase: String): Int = words.indexOf(" $phrase ")

    /**
     * [text] with the letters a scan mistakes digits for put right, inside numbers only:
     * `1,4O8.5O` becomes `1,408.50` and `l20` becomes `120`, while `Oil` stays as it is.
     */
    fun fixDigits(text: String): String =
        NUMBER_LIKE.replace(text) { match ->
            // "5l" is five litres, not fifty-one
            if (match.value.none(Char::isDigit) || match.value.last() in "lI") {
                match.value
            } else {
                match.value.map { MISREAD_DIGITS[it] ?: it }.joinToString("")
            }
        }

    /**
     * The amounts in [text], left to right: `₹1,428.00`, `Rs. 1428/-`, `INR 1428` or `1428`.
     * Percentages, quantities with a unit (`500ml`) and numbers inside codes are not amounts.
     */
    fun amounts(text: String): List<AmountToken> =
        AMOUNT.findAll(text).mapNotNull { match ->
            val (currency, whole, decimals, suffix) = match.destructured
            val digits = whole.replace(",", "")
            // More than any amount, and more than fits: an account or licence number
            if (digits.length > MAX_DIGITS) return@mapNotNull null

            val isMarked =
                currency.isNotEmpty() || decimals.isNotEmpty() || suffix.isNotEmpty() || ',' in whole
            AmountToken(
                amount = Money.fromRupees(BigDecimal(if (decimals.isEmpty()) digits else "$digits.$decimals")),
                looksLikeMoney = isMarked || digits.length in BARE_DIGITS
            )
        }.toList()

    private val NOT_WORD = Regex("""[^\p{L}\p{N}]+""")

    // A run of digits and digit look-alikes that is not part of a word
    private val NUMBER_LIKE = Regex("""(?<![\p{L}\d])[\dOoIl][\dOoIl,]*(?:\.[\dOoIl]{1,2})?(?![\p{L}\d])""")
    private val MISREAD_DIGITS = mapOf('O' to '0', 'o' to '0', 'I' to '1', 'l' to '1')

    // Currency, whole part, decimals, "/-". Not inside a word or a longer number, and not a percentage
    private val AMOUNT = Regex(
        """(?<![\p{L}\d.,])(₹|rs\.?|inr\.?)?\s*(\d+(?:,\d+)*)(?:\.(\d{1,2}))?(/-)?(?![\p{L}\d]|[.,]\d|\s*%)""",
        RegexOption.IGNORE_CASE
    )

    private const val MAX_DIGITS = 9
    private val BARE_DIGITS = 2..4
}
