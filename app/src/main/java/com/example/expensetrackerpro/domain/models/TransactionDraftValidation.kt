package com.example.expensetrackerpro.domain.models

import java.math.BigDecimal

/** The outcome of checking a transaction form. [amount] is set only when the amount is valid. */
data class TransactionDraftValidation(
    val amount: BigDecimal? = null,
    val amountError: AmountError? = null,
    val isTitleBlank: Boolean = false,
    val isNoteTooLong: Boolean = false,
    val isCategoryMissing: Boolean = false
) {
    val isValid: Boolean
        get() = amountError == null && !isTitleBlank && !isNoteTooLong && !isCategoryMissing
}
