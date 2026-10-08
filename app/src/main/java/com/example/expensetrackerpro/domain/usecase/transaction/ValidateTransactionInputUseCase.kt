package com.example.expensetrackerpro.domain.usecase.transaction

import javax.inject.Inject

/** A transaction can be saved once it has a category, a note and a positive amount. */
class ValidateTransactionInputUseCase @Inject constructor() {

    operator fun invoke(amount: String, note: String, hasCategory: Boolean): Boolean {
        val amountDouble = amount.toDoubleOrNull() ?: 0.0
        return hasCategory && note.isNotBlank() && amountDouble > 0.0
    }
}
