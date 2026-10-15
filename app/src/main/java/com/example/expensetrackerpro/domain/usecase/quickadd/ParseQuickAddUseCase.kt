package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.QuickAddResult
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.quickadd.QuickAddInput
import com.example.expensetrackerpro.domain.quickadd.QuickAddParser
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Works out a transaction from what the user typed or said, then checks it, whichever parser did
 * the work: an amount must be above zero and under the limit, the date neither in the future nor
 * over a year back, the category one of the user's of the right type. A doubtful value is left
 * out rather than guessed, as with a scanned bill.
 */
class ParseQuickAddUseCase @Inject constructor(
    private val parser: QuickAddParser,
    private val timeProvider: TimeProvider,
    private val currencyProvider: CurrencyProvider
) {

    operator fun invoke(
        text: String,
        currentType: TransactionType,
        categories: List<Category>,
        learned: List<CategoryMemory>
    ): QuickAddResult {
        val today = timeProvider.now()
        val result = parser.parse(
            QuickAddInput(
                text = text,
                today = today,
                currentType = currentType,
                categories = categories,
                learned = learned,
                currencySymbol = currencyProvider.current().symbol
            )
        )

        return validated(result, today, currentType, categories)
    }

    private fun validated(
        result: QuickAddResult,
        today: Calendar,
        currentType: TransactionType,
        categories: List<Category>
    ): QuickAddResult {
        var draft = result.draft
        val confidence = result.confidence.toMutableMap()

        val amount = draft.amount
        if (amount != null && (!amount.isPositive || amount >= AMOUNT_LIMIT)) {
            draft = draft.copy(amount = null)
            confidence[QuickAddField.AMOUNT] = FieldConfidence.MISSING
        }

        val dateMillis = draft.dateMillis
        if (dateMillis != null && dateMillis !in earliestDay(today)..utcDay(today)) {
            // The form then keeps today, as when no date was said
            draft = draft.copy(dateMillis = null)
            confidence.remove(QuickAddField.DATE)
        }

        val type = draft.type ?: currentType
        if (draft.categoryId != null && categories.none { it.id == draft.categoryId && it.type == type }) {
            draft = draft.copy(categoryId = null)
            confidence[QuickAddField.CATEGORY] = FieldConfidence.MISSING
        }

        draft.title?.let { title -> draft = draft.copy(title = title.trim().take(TITLE_MAX_LENGTH).ifBlank { null }) }
        draft.note?.let { note -> draft = draft.copy(note = note.trim().take(NOTE_MAX_LENGTH).ifBlank { null }) }
        if (draft.title == null) confidence[QuickAddField.TITLE] = FieldConfidence.MISSING
        if (draft.note == null) confidence.remove(QuickAddField.NOTE)

        return QuickAddResult(draft, confidence)
    }

    /** Midnight UTC of [calendar]'s day, as a draft carries a date. */
    private fun utcDay(calendar: Calendar): Long = GregorianCalendar(UTC).apply {
        clear()
        set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis

    private fun earliestDay(today: Calendar): Long =
        utcDay((today.clone() as Calendar).apply { add(Calendar.YEAR, -1) })

    companion object {
        /** An amount this large or larger is taken for a slip: ₹10,00,000. */
        val AMOUNT_LIMIT: Money = Money.ofRupees(1_000_000)

        const val TITLE_MAX_LENGTH = 40
        const val NOTE_MAX_LENGTH = ValidateTransactionDraftUseCase.NOTE_MAX_LENGTH

        private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    }
}
