package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory repository that, like the DAO, returns newest transactions first. */
class FakeTransactionRepository(
    initial: List<Transaction> = emptyList()
) : TransactionRepository {

    private val transactions = MutableStateFlow(initial)

    val current: List<Transaction> get() = transactions.value

    override suspend fun saveTransaction(transaction: Transaction) {
        transactions.update { list -> list.filterNot { it.id == transaction.id } + transaction }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.update { list ->
            list.map { if (it.id == transaction.id) transaction else it }
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.update { list -> list.filterNot { it.id == transaction.id } }
    }

    override suspend fun getTransactionById(id: Int): Transaction? =
        transactions.value.find { it.id == id }

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactions.map { list -> list.sortedByDescending { it.dateMillis } }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        getAllTransactions().map { list -> list.filter { it.transactionType == type } }

    override fun getFilteredTransactions(startTimestamp: Long): Flow<List<Transaction>> =
        getAllTransactions().map { list -> list.filter { it.dateMillis >= startTimestamp } }
}
