package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Transaction
import javax.inject.Inject

/** Adds a transaction that has no id yet, otherwise updates the stored one. */
class SaveTransactionUseCase @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase
) {

    /** Returns false when the transaction could not be written. */
    suspend operator fun invoke(transaction: Transaction): Boolean =
        if (transaction.id == 0) addTransaction(transaction) else updateTransaction(transaction)
}
