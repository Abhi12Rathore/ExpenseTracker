package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

/** Things the screen does once, as opposed to state it keeps showing. */
sealed interface TransactionListEffect {
    data object DeleteFailed : TransactionListEffect
}
