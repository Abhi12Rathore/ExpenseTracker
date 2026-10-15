package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.quickadd.QuickAddInput
import com.example.expensetrackerpro.domain.quickadd.QuickAddParser
import com.example.expensetrackerpro.domain.usecase.scan.ResolveCategoryHintUseCase
import javax.inject.Inject

/**
 * Quick add by rules: free, instant and on every phone. Times and the date are taken first, so
 * their numbers aren't read as the amount; then the amount; then whatever words are left become
 * the title and note. The category is the one the user picked for this title before, or else the
 * first keyword's.
 */
class RuleBasedQuickAddParser(private val keywords: QuickAddKeywords) : QuickAddParser {

    @Inject
    constructor() : this(QuickAddKeywords.EN_IN)

    private val dateParser = QuickAddDateParser(keywords)
    private val amountParser = QuickAddAmountParser(keywords)
    private val textParser = QuickAddTextParser(keywords)
    private val resolveCategoryHint = ResolveCategoryHintUseCase()

    override fun parse(input: QuickAddInput): QuickAddResult {
        val tokens = QuickAddTokens.tokenize(input.text)
        val used = BooleanArray(tokens.size)

        dateParser.markTimes(tokens, used)
        val dateMillis = dateParser.parse(tokens, used, input.today)
        val amount = amountParser.parse(tokens, used, input.currencySymbol)
        val text = textParser.parse(tokens, used)
        val type = typeOf(tokens)
        val categoriesOfType = input.categories.filter { it.type == (type ?: input.currentType) }
        val category = category(text.title, type ?: input.currentType, tokens, categoriesOfType, input.learned)

        val confidence = buildMap {
            put(QuickAddField.AMOUNT, amount?.confidence ?: FieldConfidence.MISSING)
            put(QuickAddField.TITLE, if (text.title != null) FieldConfidence.SURE else FieldConfidence.MISSING)
            put(QuickAddField.CATEGORY, category?.second ?: FieldConfidence.MISSING)
            if (dateMillis != null) put(QuickAddField.DATE, FieldConfidence.SURE)
            if (text.note != null) put(QuickAddField.NOTE, FieldConfidence.SURE)
            if (type != null) put(QuickAddField.TYPE, FieldConfidence.SURE)
        }

        return QuickAddResult(
            draft = TransactionDraft(
                amount = amount?.amount,
                title = text.title,
                dateMillis = dateMillis,
                note = text.note,
                type = type,
                categoryId = category?.first
            ),
            confidence = confidence
        )
    }

    /** Income or expense when a word says so, the first such word winning; null when none does. */
    private fun typeOf(tokens: List<Token>): TransactionType? =
        tokens.firstNotNullOfOrNull { token ->
            when (token.lower) {
                in keywords.incomeWords -> TransactionType.INCOME
                in keywords.expenseWords -> TransactionType.EXPENSE
                else -> null
            }
        }

    private fun category(
        title: String?,
        type: TransactionType,
        tokens: List<Token>,
        categories: List<Category>,
        learned: List<CategoryMemory>
    ): Pair<CategoryId, FieldConfidence>? {
        if (title != null) {
            val key = CategoryMemory.keyOf(title)
            val remembered = learned.firstOrNull { it.titleKey == key && it.type == type }
                ?.takeIf { memory -> categories.any { it.id == memory.categoryId } }
            if (remembered != null) return remembered.categoryId to FieldConfidence.SURE
        }

        val words = " " + tokens.filter { it.kind == Token.Kind.WORD }.joinToString(" ") { it.lower } + " "
        return keywords.categoryKeywords.asSequence()
            .filter { (keyword, _) ->
                // A short word such as "ola" only on its own; a longer one also inside a name
                if (keyword.length <= keywords.shortKeywordLength) " $keyword " in words else keyword in words
            }
            .firstNotNullOfOrNull { (_, hint) -> resolveCategoryHint(hint, categories) }
            ?.let { it.id to FieldConfidence.GUESS }
    }
}
