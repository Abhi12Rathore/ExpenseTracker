package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import com.example.expensetrackerpro.domain.models.Transaction

/** Things the screen does once, as opposed to state it keeps showing. */
sealed interface TransactionListEffect {
    /** [transaction] was just deleted, here or on the edit screen, and can still be restored. */
    data class ShowUndoDelete(val transaction: Transaction) : TransactionListEffect
    data object ShowRestoreFailed : TransactionListEffect
}
