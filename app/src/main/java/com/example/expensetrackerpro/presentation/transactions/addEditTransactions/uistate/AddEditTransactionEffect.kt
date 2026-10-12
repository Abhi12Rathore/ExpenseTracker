package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

import androidx.annotation.StringRes

/** Things the screen does once, as opposed to state it keeps showing. */
sealed interface AddEditTransactionEffect {
    /** The transaction was saved or deleted, so the form is done. */
    data object NavigateBack : AddEditTransactionEffect
    data class ShowError(@StringRes val messageRes: Int) : AddEditTransactionEffect

    /** The scanned bill could not be read. The message comes with a way to scan again. */
    data class BillUnreadable(@StringRes val messageRes: Int) : AddEditTransactionEffect
}
