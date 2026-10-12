package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import com.example.expensetrackerpro.domain.models.Money

/** The parts of the form a draft, such as a scanned bill, can fill in. */
enum class DraftField {
    AMOUNT, TITLE, DATE, NOTE, CATEGORY
}

/** What a draft has put into the form, for as long as the form shows that it did. */
data class DraftFill(
    /** The fields still holding the draft's value. One leaves as soon as the user changes it. */
    val filledFields: Set<DraftField> = emptySet(),
    /** Every amount the draft offers, the likeliest first, to pick from in one tap. */
    val amountOptions: List<Money> = emptyList(),
    /** The draft had no amount it was sure of, and the form has none yet. */
    val isAmountMissing: Boolean = false
)
