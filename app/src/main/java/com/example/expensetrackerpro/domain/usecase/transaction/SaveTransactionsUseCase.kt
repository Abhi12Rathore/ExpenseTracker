package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Adds several new transactions at once, such as the ones said in one quick-add sentence: all of
 * them or none. Each must pass the checks the form's Save makes; when one doesn't, nothing is
 * written.
 */
class SaveTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val validateTransactionDraft: ValidateTransactionDraftUseCase,
    private val formatAmountInput: FormatAmountInputUseCase
) {

    suspend operator fun invoke(transactions: List<Transaction>): Outcome<Unit> {
        if (transactions.isEmpty()) return Outcome.Success(Unit)
        if (!transactions.all(::isComplete)) {
            return Outcome.Failure(DomainError.InvalidInput(InvalidInputReason.TRANSACTION_INCOMPLETE))
        }

        return repository.saveTransactions(transactions)
    }

    private fun isComplete(transaction: Transaction): Boolean =
        transaction.id == TransactionId.UNSAVED &&
            validateTransactionDraft(
                amount = formatAmountInput(transaction.amount),
                title = transaction.title,
                note = transaction.note,
                hasCategory = true
            ).isValid
}
