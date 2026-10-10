package com.example.expensetrackerpro.domain.models


/** The outcome of checking a transaction form. [amount] is set only when the amount is valid. */
data class TransactionDraftValidation(
    val amount: Money? = null,
    val amountError: AmountError? = null,
    val isTitleBlank: Boolean = false,
    val isNoteTooLong: Boolean = false,
    val isCategoryMissing: Boolean = false
) {
    val isValid: Boolean
        get() = amountError == null && !isTitleBlank && !isNoteTooLong && !isCategoryMissing
}
