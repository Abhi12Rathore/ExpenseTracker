package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.Money

/** The amount to pay, and what else on the bill might be it. */
internal data class BillAmounts(val amount: Money?, val others: List<Money>)

/**
 * Finds the amount to pay. The number beside the strongest total label wins, the last one when
 * a label is printed twice. A bill with no such label at all gets its largest amount; one whose
 * only "total" is a sub total, or a count of items, gets none, since the largest amount on it is
 * then most likely not what was paid.
 */
internal class BillAmountParser(private val keywords: BillKeywords) {

    /** [lines] are the bill's lines without their dates and bill numbers. */
    fun parse(lines: List<String>): BillAmounts {
        val cleaned = lines.map(::withoutCodes)
        val candidates = candidates(cleaned)

        val amount = labelledTotal(cleaned)
            ?: candidates.maxOrNull().takeUnless { cleaned.any(::mentionsTotal) }

        return BillAmounts(
            amount = amount,
            others = candidates.filter { it != amount }.distinct().sortedDescending().take(MAX_OTHERS)
        )
    }

    private fun labelledTotal(lines: List<String>): Money? =
        lines.indices
            .mapNotNull { index ->
                val rank = totalRank(lines[index]) ?: return@mapNotNull null
                val amount = amountAt(lines, index) ?: return@mapNotNull null
                Labelled(rank, index, amount)
            }
            .maxWithOrNull(compareBy<Labelled> { it.rank }.thenBy { it.index })
            ?.amount

    /** How strong a total label [line] has, or null when it has none or holds some other figure. */
    private fun totalRank(line: String): Int? {
        val words = BillTokens.words(line)
        val label = keywords.totalLabels
            .filter { BillTokens.hasPhrase(words, it.words) }
            .maxByOrNull { it.rank }
            ?: return null

        val skipWords = keywords.skipWords.filter { BillTokens.hasPhrase(words, it) }
        if (skipWords.isEmpty()) return label.rank

        // "Grand Total (incl. GST)" is still the total; "CGST on Net Amount" and "Total Tax" are not
        val labelStart = BillTokens.phraseIndex(words, label.words)
        val isTaxNote = label.rank >= BillKeywords.STRONG_RANK &&
            skipWords.all { it in keywords.taxWords && BillTokens.phraseIndex(words, it) > labelStart }

        return label.rank.takeIf { isTaxNote }
    }

    /** The last amount on the line, or the one on the next line when that holds nothing else. */
    private fun amountAt(lines: List<String>, index: Int): Money? {
        val onLine = BillTokens.amounts(lines[index]).lastOrNull()
        val amount = onLine
            ?: lines.getOrNull(index + 1)
                ?.takeIf { next -> next.none(Char::isLetter) || CURRENCY_ONLY.matches(next.filter(Char::isLetter)) }
                ?.let { BillTokens.amounts(it).lastOrNull() }

        return amount?.amount?.takeIf(::isPlausible)
    }

    /** Every amount on the bill that could be what was paid. */
    private fun candidates(lines: List<String>): List<Money> =
        lines
            .filterNot { line ->
                val words = BillTokens.words(line)
                keywords.nonAmountWords.any { BillTokens.hasPhrase(words, it) }
            }
            .flatMap(BillTokens::amounts)
            .filter { it.looksLikeMoney && isPlausible(it.amount) }
            .map { it.amount }

    // Also inside a longer word, as in "Subtotal"
    private fun mentionsTotal(line: String): Boolean {
        val words = BillTokens.words(line)
        return keywords.totalLabels.any { it.words in words }
    }

    private fun isPlausible(amount: Money): Boolean = amount.isPositive && amount < keywords.amountLimit

    private fun withoutCodes(line: String): String =
        CODES.fold(line) { text, code -> code.replace(text, " ") }

    private data class Labelled(val rank: Int, val index: Int, val amount: Money)

    private companion object {
        const val MAX_OTHERS = 3

        // What is left of a line such as "Rs." or "INR" once the number is gone
        val CURRENCY_ONLY = Regex("""rs|inr""", RegexOption.IGNORE_CASE)

        // Times, GST numbers and phone numbers: digits that are never amounts
        val CODES = listOf(
            Regex("""\d{1,2}:\d{2}(?::\d{2})?(?:\s*[ap]m)?""", RegexOption.IGNORE_CASE),
            Regex("""\d{2}[A-Z]{5}\d{4}[A-Z][A-Z\d]{3}"""),
            Regex("""(?<!\d)(?:\+?91[\s-]?)?\d{5}[\s-]?\d{5}(?!\d)""")
        )
    }
}
