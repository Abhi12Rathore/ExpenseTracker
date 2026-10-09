package com.example.expensetrackerpro.data.repository

import android.database.sqlite.SQLiteException
import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.data.mapper.toEntity
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

    override suspend fun saveTransaction(transaction: Transaction): Boolean =
        write { transactionDao.insertTransaction(transaction.toEntity()) }

    override suspend fun updateTransaction(transaction: Transaction): Boolean =
        write { transactionDao.updateTransaction(transaction.toEntity()) }

    override suspend fun deleteTransaction(transaction: Transaction): Boolean {
        val deleted = write { transactionDao.deleteTransaction(transaction.toEntity()) }
        if (deleted) _deletions.tryEmit(transaction)
        return deleted
    }

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

    // A write the database rejects (a category that no longer exists, a full disk) is an
    // expected failure, reported to the caller as false
    private inline fun write(block: () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: SQLiteException) {
            false
        }

    // So the user's %, _ and \ are searched for literally
    private fun String.escapedForLike(): String =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
