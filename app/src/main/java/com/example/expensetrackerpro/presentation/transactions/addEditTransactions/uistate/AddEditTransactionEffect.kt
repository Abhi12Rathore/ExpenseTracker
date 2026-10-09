package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate

/** Things the screen does once, as opposed to state it keeps showing. */
sealed interface AddEditTransactionEffect {
    /** The transaction was saved or deleted, so the form is done. */
    data object NavigateBack : AddEditTransactionEffect
    data class ShowError(val error: AddEditTransactionError) : AddEditTransactionEffect
}

enum class AddEditTransactionError {
    SAVE_FAILED, DELETE_FAILED
}
