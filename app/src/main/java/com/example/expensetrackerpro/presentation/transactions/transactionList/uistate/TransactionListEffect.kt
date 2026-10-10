package com.example.expensetrackerpro.presentation.transactions.transactionList.uistate

import androidx.annotation.StringRes

/** Things the screen does once, as opposed to state it keeps showing. */
sealed interface TransactionListEffect {
    data class DeleteFailed(@StringRes val messageRes: Int) : TransactionListEffect
}
