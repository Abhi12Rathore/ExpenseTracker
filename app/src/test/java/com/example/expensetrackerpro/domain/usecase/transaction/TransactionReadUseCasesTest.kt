package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionReadUseCasesTest {

    private val old = transaction(id = 1, dateMillis = 100L)
    private val mid = transaction(id = 2, dateMillis = 200L)
    private val new = transaction(id = 3, dateMillis = 300L)
    private val repository = FakeTransactionRepository(listOf(old, new, mid))

    @Test
    fun getTransactions_defaultsToEverythingNewestFirst() = runTest {
        assertEquals(listOf(new, mid, old), GetTransactionsUseCase(repository)().first())
    }

    @Test
    fun getTransactions_includesStartTimestampAndLater() = runTest {
        assertEquals(listOf(new, mid), GetTransactionsUseCase(repository)(200L).first())
    }

    @Test
    fun getTransactions_startAfterEverything_isEmpty() = runTest {
        assertTrue(GetTransactionsUseCase(repository)(301L).first().isEmpty())
    }

    @Test
    fun getTransactionById_returnsMatch() = runTest {
        assertEquals(mid, GetTransactionByIdUseCase(repository)(2))
    }

    @Test
    fun getTransactionById_unknownId_isNull() = runTest {
        assertNull(GetTransactionByIdUseCase(repository)(99))
    }
}
