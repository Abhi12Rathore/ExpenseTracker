package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.QuickAddBatch
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.QuickAddPart
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.TransactionType
import javax.inject.Inject

/**
 * Finds several transactions in one sentence: "450 swiggy and 120 uber today". The sentence is
 * split at new lines, at commas that are not inside a number, and at [QuickAddKeywords.splitWords],
 * and each part is read by [ParseQuickAddUseCase] with every check it makes. A sentence is split
 * only when it gives 2 to [MAX_PARTS] parts and every one of them has its own amount; otherwise
 * it is one transaction, and the result is null.
 *
 * A date is said once for all: a part that names no date takes the first one said.
 */
class SplitQuickAddUseCase(
    private val parseQuickAdd: ParseQuickAddUseCase,
    private val keywords: QuickAddKeywords
) {

    @Inject
    constructor(parseQuickAdd: ParseQuickAddUseCase) : this(parseQuickAdd, QuickAddKeywords.EN_IN)

    private val numberWords: Set<String> = keywords.numberWords.keys + keywords.multipliers.keys

    operator fun invoke(
        text: String,
        currentType: TransactionType,
        categories: List<Category>,
        learned: List<CategoryMemory>
    ): QuickAddBatch? {
        val pieces = split(text)
        if (pieces.size !in 2..MAX_PARTS) return null

        val parts = pieces.map { piece -> QuickAddPart(piece, parseQuickAdd(piece, currentType, categories, learned)) }
        if (parts.any { it.result.draft.amount == null }) return null

        val sharedDate = parts.firstNotNullOfOrNull { it.result.draft.dateMillis } ?: return QuickAddBatch(parts)
        return QuickAddBatch(parts.map { it.copy(result = it.result.withDate(sharedDate)) })
    }

    private fun QuickAddResult.withDate(dateMillis: Long): QuickAddResult {
        if (draft.dateMillis != null) return this
        return QuickAddResult(
            draft.copy(dateMillis = dateMillis),
            confidence + (QuickAddField.DATE to FieldConfidence.SURE)
        )
    }

    /** The sentence's pieces, trimmed, with the empty ones left out. */
    private fun split(text: String): List<String> {
        val pieces = mutableListOf<String>()
        var start = 0
        for (match in separator.findAll(text)) {
            if (match.value[0].isLetter() && joinsNumbers(text, match.range)) continue
            pieces += text.substring(start, match.range.first)
            start = match.range.last + 1
        }
        pieces += text.substring(start)

        return pieces.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private val separator: Regex = Regex(
        // A comma with a digit on both sides groups the digits of a number: "1,240"
        """\n|(?<!\d),|,(?!\d)|""" +
            keywords.splitWords.joinToString("|") { """(?<![\p{L}\d])${Regex.escape(it)}(?![\p{L}\d])""" },
        RegexOption.IGNORE_CASE
    )

    /** Whether the split word at [range] sits between two written-out numbers: "one hundred and fifty". */
    private fun joinsNumbers(text: String, range: IntRange): Boolean {
        val before = WORD.findAll(text.substring(0, range.first)).lastOrNull()?.value?.lowercase()
        val after = WORD.find(text, range.last + 1)?.value?.lowercase()
        return before in numberWords && after in numberWords
    }

    companion object {
        const val MAX_PARTS = 5

        private val WORD = Regex("""[\p{L}\d]+""")
    }
}
