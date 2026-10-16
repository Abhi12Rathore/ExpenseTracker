package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import androidx.annotation.StringRes

/** Things the screen does once, as opposed to state it keeps showing. */
sealed interface AddEditTransactionEffect {
    /** The transaction was saved, so the form is done. Confirmed with a tap of the phone. */
    data object Saved : AddEditTransactionEffect

    /** The transaction was deleted, so the form is done. */
    data object NavigateBack : AddEditTransactionEffect
    data class ShowError(@StringRes val messageRes: Int) : AddEditTransactionEffect

    /** The scanned bill could not be read. The message comes with a way to scan again. */
    data class BillUnreadable(@StringRes val messageRes: Int) : AddEditTransactionEffect

    /** The text had no amount, so the keypad comes up for it. */
    data object FocusAmount : AddEditTransactionEffect

    /** The first correction for [title]: "Next time [title] goes to [categoryName]", with Undo. */
    data class CategoryLearned(val title: String, val categoryName: String) : AddEditTransactionEffect
}
