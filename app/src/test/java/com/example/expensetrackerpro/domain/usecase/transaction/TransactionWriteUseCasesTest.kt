package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun deleteLastTransaction_leavesRepositoryEmpty() = runTest {
        val repository = FakeTransactionRepository(listOf(lunch))

        DeleteTransactionUseCase(repository)(lunch)

        assertTrue(repository.current.isEmpty())
    }
}
