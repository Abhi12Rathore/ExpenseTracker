package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType

/** The parts of the form a draft, such as a scanned bill, can fill in. */
enum class DraftField {
    AMOUNT, TITLE, DATE, NOTE, CATEGORY, TYPE
}

/** Where a draft came from, which the banner and the tags beside the fields say. */
sealed interface DraftSource {
    /** A photo of a bill. */
    data object Bill : DraftSource

    /** What the user typed or said into quick add, quoted on the banner. */
    data class Text(val text: String) : DraftSource

    /** One of the user's frequent transactions, named on the banner. */
    data class History(val title: String) : DraftSource
}

/** What a draft has put into the form, for as long as the form shows that it did. */
data class DraftFill(
    /** The fields still holding the draft's value. One leaves as soon as the user changes it. */
    val filledFields: Set<DraftField> = emptySet(),
    /** Every amount the draft offers, the likeliest first, to pick from in one tap. */
    val amountOptions: List<Money> = emptyList(),
    /** The draft had no amount it was sure of, and the form has none yet. */
    val isAmountMissing: Boolean = false,
    val source: DraftSource = DraftSource.Bill,
    /** The type the form had before the draft changed it, which Clear puts back. */
    val typeBeforeFill: TransactionType? = null,
    /** The category the draft picked, or null when it found none: what a correction is told from. */
    val suggestedCategoryId: CategoryId? = null
)
