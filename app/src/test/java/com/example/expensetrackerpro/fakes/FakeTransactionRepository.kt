package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory repository that, like the DAO, returns newest transactions first. */
class FakeTransactionRepository(
    initial: List<Transaction> = emptyList()
) : TransactionRepository {

    private val transactions = MutableStateFlow(initial)

    val current: List<Transaction> get() = transactions.value

    /** While true every write is rejected with [writeFailure], as a failing database would. */
    var failWrites = false

    var writeFailure: DomainError = DomainError.StorageFailure()

    /** How many times a delete was asked for, whether or not it went through. */
    var deleteCalls = 0
        private set

    /** When set, every delete waits for it to complete, as a slow database would. */
    var deleteGate: CompletableDeferred<Unit>? = null

    private val _deletions = MutableSharedFlow<Transaction>(extraBufferCapacity = 16)
    override val deletions: Flow<Transaction> = _deletions

    override suspend fun saveTransaction(transaction: Transaction): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(writeFailure)
        transactions.update { list -> list.filterNot { it.id == transaction.id } + transaction }
        return Outcome.Success(Unit)
    }

    override suspend fun updateTransaction(transaction: Transaction): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(writeFailure)
        transactions.update { list ->
            list.map { if (it.id == transaction.id) transaction else it }
        }
        return Outcome.Success(Unit)
    }

    override suspend fun deleteTransaction(transaction: Transaction): Outcome<Unit> {
        deleteCalls++
        deleteGate?.await()
        if (failWrites) return Outcome.Failure(writeFailure)
        transactions.update { list -> list.filterNot { it.id == transaction.id } }
        _deletions.tryEmit(transaction)
        return Outcome.Success(Unit)
    }

    override suspend fun getTransactionById(id: TransactionId): Transaction? =
        transactions.value.find { it.id == id }

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactions.map { list -> list.sortedByDescending { it.dateMillis } }

    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        getAllTransactions().map { list -> list.filter { it.transactionType == type } }

    override fun getTransactions(range: DateRange, query: String): Flow<List<Transaction>> =
        getAllTransactions().map { list ->
            list.filter { it.dateMillis >= range.startMillis && it.dateMillis < range.endMillis }
                .filter { it.matches(query.trim()) }
        }

    private fun Transaction.matches(query: String): Boolean =
        query.isEmpty() || listOf(title, note, categoryName).any { it.contains(query, ignoreCase = true) }
}
