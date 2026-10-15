package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    /** Each transaction as it is deleted. Nothing is replayed to late collectors. */
    val deletions: Flow<Transaction>

    // A write fails with NotFound when the transaction's category is no longer stored

    suspend fun saveTransaction(transaction: Transaction): Outcome<Unit>

    suspend fun updateTransaction(transaction: Transaction): Outcome<Unit>

    suspend fun deleteTransaction(transaction: Transaction): Outcome<Unit>

    suspend fun getTransactionById(id: TransactionId): Transaction?

    /** The transaction with the latest date, or null when there are none. */
    suspend fun getLatestTransaction(): Transaction?

    suspend fun countTransactions(): Int

    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    /**
     * Transactions dated within [range], newest first. A non-blank [query] keeps only those
     * whose title, note or category name contains it, ignoring case.
     */
    fun getTransactions(range: DateRange, query: String = ""): Flow<List<Transaction>>
}
