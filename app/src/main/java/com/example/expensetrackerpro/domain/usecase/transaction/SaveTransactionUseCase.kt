package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import javax.inject.Inject

/** Adds a transaction that has no id yet, otherwise updates the stored one. */
class SaveTransactionUseCase @Inject constructor(
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase
) {

    suspend operator fun invoke(transaction: Transaction): Outcome<Unit> =
        if (transaction.id == TransactionId.UNSAVED) addTransaction(transaction) else updateTransaction(transaction)
}
