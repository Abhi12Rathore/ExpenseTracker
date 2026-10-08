package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun saveTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(transaction: Transaction)

    suspend fun getTransactionById(id: Int): Transaction?

    fun getAllTransactions(): Flow<List<Transaction>>

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>

    /** Transactions dated within [range], newest first. */
    fun getTransactions(range: DateRange): Flow<List<Transaction>>
}
