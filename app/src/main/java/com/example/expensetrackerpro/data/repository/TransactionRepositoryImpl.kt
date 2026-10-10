package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.onSuccess
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    private val _deletions = MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
    override val deletions: Flow<Transaction> = _deletions.asSharedFlow()

    override suspend fun saveTransaction(transaction: Transaction): Outcome<Unit> =
        write { transactionDao.insertTransaction(transaction.toEntity()) }

    override suspend fun updateTransaction(transaction: Transaction): Outcome<Unit> =
        write { transactionDao.updateTransaction(transaction.toEntity()) }

    override suspend fun deleteTransaction(transaction: Transaction): Outcome<Unit> =
        write { transactionDao.deleteTransaction(transaction.toEntity()) }
            .onSuccess { _deletions.tryEmit(transaction) }

    override suspend fun getTransactionById(id: Int): Transaction? =
        transactionDao.getTransactionById(id)?.toDomain()

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type).map { list -> list.map { it.toDomain() } }

    override fun getTransactions(range: DateRange, query: String): Flow<List<Transaction>> {
        val search = query.trim()
        val transactions = if (search.isEmpty()) {
            transactionDao.getTransactionsBetween(range.startMillis, range.endMillis)
        } else {
            transactionDao.searchTransactionsBetween(
                range.startMillis,
                range.endMillis,
                search.escapedForLike()
            )
        }

        return transactions.map { list -> list.map { it.toDomain() } }
    }

    // The only foreign key a transaction has is its category, so breaking it means that is gone
    private inline fun write(block: () -> Unit): Outcome<Unit> =
        storageOutcome(foreignKeyError = DomainError.NotFound, block = block)

    // So the user's %, _ and \ are searched for literally
    private fun String.escapedForLike(): String =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
