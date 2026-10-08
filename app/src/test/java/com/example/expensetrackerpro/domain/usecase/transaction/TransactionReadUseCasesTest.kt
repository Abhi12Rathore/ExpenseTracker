package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.DateRange
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
    fun getTransactions_includesRangeStart() = runTest {
        val fromMid = DateRange(startMillis = 200L, endMillis = Long.MAX_VALUE)

        assertEquals(listOf(new, mid), GetTransactionsUseCase(repository)(fromMid).first())
    }

    @Test
    fun getTransactions_excludesRangeEnd() = runTest {
        val beforeNew = DateRange(startMillis = 100L, endMillis = 300L)

        assertEquals(listOf(mid, old), GetTransactionsUseCase(repository)(beforeNew).first())
    }

    @Test
    fun getTransactions_rangeAfterEverything_isEmpty() = runTest {
        val later = DateRange(startMillis = 301L, endMillis = 400L)

        assertTrue(GetTransactionsUseCase(repository)(later).first().isEmpty())
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
