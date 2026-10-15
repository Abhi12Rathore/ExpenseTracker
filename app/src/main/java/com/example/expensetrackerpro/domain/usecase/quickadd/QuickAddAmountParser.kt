package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.Money
import java.math.BigDecimal

/** The amount found, and how sure the rule is that it is the one meant. */
internal data class FoundAmount(val amount: Money, val confidence: FieldConfidence)

/**
 * Finds the amount: `450`, `₹450`, `rs 450`, `450 rupees`, `1,05,000`, `4.5k`, `1.05L`,
 * `1.05 lakh`, `2 crore` or words, `one lakh five thousand`. Of several numbers it takes the one
 * next to a currency word or symbol, else the largest, which is then only a guess. Numbers already
 * taken as a date or time are not looked at.
 */
internal class QuickAddAmountParser(private val keywords: QuickAddKeywords) {

    /** The amount, with its tokens marked as [used]; null when the sentence has none. */
    fun parse(tokens: List<Token>, used: BooleanArray, currencySymbol: String): FoundAmount? {
        val candidates = mutableListOf<Candidate>()
        var i = 0
        while (i < tokens.size) {
            val candidate = if (used[i]) null else readAt(tokens, used, i, currencySymbol)
            if (candidate != null) {
                candidates += candidate
                i = candidate.span.last + 1
            } else {
                i++
            }
        }

        val marked = candidates.firstOrNull { it.isMarked }
        val chosen = marked ?: candidates.filter { it.hasDigits || it.hasMultiplier }.maxByOrNull { it.value }
            ?: return null
        chosen.span.forEach { used[it] = true }

        val isOnlyNumber = candidates.size == 1
        return FoundAmount(
            amount = Money.fromRupees(chosen.value),
            confidence = if (chosen === marked || isOnlyNumber) FieldConfidence.SURE else FieldConfidence.GUESS
        )
    }

    /**
     * The number starting at [start], with what makes it up: digits and words, a multiplier joined
     * on or apart ("4.5k", "2 crore", "one lakh five thousand"). Null when no amount starts there.
     */
    private fun readAt(tokens: List<Token>, used: BooleanArray, start: Int, currencySymbol: String): Candidate? {
        var total = BigDecimal.ZERO
        var current = BigDecimal.ZERO
        var hasCurrent = false
        var lastWasTens = false
        var hasDigits = false
        var hasMultiplier = false
        var hasCurrencySuffix = false
        var end = start - 1

        fun multiply(by: Long) {
            if (by < THOUSAND) {
                current = current.multiply(BigDecimal.valueOf(by))
            } else {
                total += current.multiply(BigDecimal.valueOf(by))
                current = BigDecimal.ZERO
            }
            hasCurrent = false
            hasMultiplier = true
        }

        var j = start
        while (j < tokens.size && !used[j]) {
            val token = tokens[j]
            when {
                token.kind == Token.Kind.NUMBER -> {
                    // Two numbers side by side are two amounts; after a multiplier, the rest of one: "1 lakh 5 thousand"
                    if (hasCurrent) break
                    val parts = QuickAddTokens.numberParts(token) ?: break
                    if (parts.digits.substringBefore('.').length > MAX_DIGITS) break
                    val multiplier = keywords.multipliers[parts.suffix]
                    when {
                        parts.suffix.isEmpty() || multiplier != null -> Unit
                        parts.suffix in keywords.currencyWords -> hasCurrencySuffix = true
                        // 500ml, 2kg, 12th: a quantity or a day, not an amount
                        else -> break
                    }
                    current += BigDecimal(parts.digits)
                    hasCurrent = true
                    hasDigits = true
                    lastWasTens = false
                    if (multiplier != null) multiply(multiplier)
                }
                token.kind == Token.Kind.WORD && token.lower in keywords.numberWords -> {
                    val value = keywords.numberWords.getValue(token.lower)
                    // "twenty five" is one number, "five five" is not
                    if (hasCurrent && !(lastWasTens && value < TEN)) break
                    current += BigDecimal.valueOf(value)
                    hasCurrent = true
                    lastWasTens = value >= TWENTY && value % TEN == 0L
                }
                token.kind == Token.Kind.WORD && token.lower in keywords.multipliers -> {
                    // A multiplier with no number before it is just a word, e.g. "L" in a name
                    if (!hasCurrent) break
                    multiply(keywords.multipliers.getValue(token.lower))
                }
                token.kind == Token.Kind.WORD && token.lower in keywords.numberJoiners -> {
                    // Only inside a number: "one hundred and fifty"
                    val next = tokens.wordAt(j + 1)
                    if (end < start || next == null || next !in keywords.numberWords) break
                }
                else -> break
            }
            end = j
            if (hasCurrencySuffix) break
            j++
        }

        if (end < start) return null
        val value = total + current
        if (value.signum() <= 0) return null
        // Number words alone, as in "one plate", are only an amount when a currency word says so
        val isMarked = hasCurrencySuffix || isCurrency(tokens, before(tokens, start), currencySymbol) ||
            tokens.wordAt(end + 1) in keywords.currencyWords

        if (!hasDigits && !hasMultiplier && !isMarked) return null
        return Candidate(value, start..end, isMarked, hasDigits, hasMultiplier)
    }

    /** The token before [i], passing over a "." as in "rs. 450". */
    private fun before(tokens: List<Token>, i: Int): Int =
        if (tokens.getOrNull(i - 1)?.text == ".") i - 2 else i - 1

    private fun isCurrency(tokens: List<Token>, i: Int, currencySymbol: String): Boolean {
        val token = tokens.getOrNull(i) ?: return false
        return token.text == currencySymbol || token.lower in keywords.currencyWords
    }

    private data class Candidate(
        val value: BigDecimal,
        val span: IntRange,
        val isMarked: Boolean,
        val hasDigits: Boolean,
        val hasMultiplier: Boolean
    )

    private companion object {
        const val TEN = 10L
        const val TWENTY = 20L
        const val THOUSAND = 1_000L

        // More than any amount: a phone or account number
        const val MAX_DIGITS = 9
    }
}
