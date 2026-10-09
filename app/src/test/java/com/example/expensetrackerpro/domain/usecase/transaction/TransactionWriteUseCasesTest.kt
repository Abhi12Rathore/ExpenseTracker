package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.domain.models.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionWriteUseCasesTest {

    private val lunch = transaction(id = 1, amount = 120.0, note = "Lunch")
    private val taxi = transaction(id = 2, amount = 300.0, note = "Taxi")

    @Test
    fun addTransaction_storesIt() = runTest {
        val repository = FakeTransactionRepository()

        AddTransactionUseCase(repository)(lunch)

        assertEquals(listOf(lunch), repository.current)
    }

    @Test
    fun updateTransaction_replacesOnlyTheMatchingId() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, taxi))
        val edited = lunch.copy(amount = 150.0, note = "Team lunch")

        UpdateTransactionUseCase(repository)(edited)

        assertEquals(listOf(edited, taxi), repository.current)
    }

    @Test
    fun deleteTransaction_removesOnlyTheMatchingId() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, taxi))

        DeleteTransactionUseCase(repository)(lunch)

        assertEquals(listOf(taxi), repository.current)
    }

    @Test
    fun saveTransaction_addsWithoutAnId_andUpdatesWithOne() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch))
        val save = SaveTransactionUseCase(AddTransactionUseCase(repository), UpdateTransactionUseCase(repository))
        val unsaved = taxi.copy(id = 0)
        val edited = lunch.copy(amount = 150.0)

        assertTrue(save(unsaved))
        assertTrue(save(edited))

        assertEquals(listOf(edited, unsaved), repository.current)
    }

    @Test
    fun restoreTransaction_putsADeletedOneBackUnderItsId() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, taxi))
        DeleteTransactionUseCase(repository)(lunch)

        assertTrue(RestoreTransactionUseCase(repository)(lunch))

        assertEquals(listOf(taxi, lunch), repository.current)
    }

    @Test
    fun deletedTransactions_areReportedAsTheyHappen() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch, taxi))
        val deleted = mutableListOf<Transaction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveDeletedTransactionsUseCase(repository)().toList(deleted)
        }

        DeleteTransactionUseCase(repository)(taxi)

        assertEquals(listOf(taxi), deleted)
    }

    @Test
    fun rejectedWrites_returnFalseAndChangeNothing() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch)).apply { failWrites = true }

        assertFalse(AddTransactionUseCase(repository)(taxi))
        assertFalse(UpdateTransactionUseCase(repository)(lunch.copy(amount = 1.0)))
        assertFalse(DeleteTransactionUseCase(repository)(lunch))

        assertEquals(listOf(lunch), repository.current)
    }

    @Test
    fun deleteLastTransaction_leavesRepositoryEmpty() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch))

        DeleteTransactionUseCase(repository)(lunch)

        assertTrue(repository.current.isEmpty())
    }
}
