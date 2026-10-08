package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRecentTransactionsUseCaseTest {

    private fun useCaseWith(count: Int) = GetRecentTransactionsUseCase(
        FakeTransactionRepository((1..count).map { transaction(id = it, dateMillis = it.toLong()) })
    )

    @Test
    fun keepsTheFiveNewestByDefault() = runTest {
        val recent = useCaseWith(8)().first()

        assertEquals(listOf(8, 7, 6, 5, 4), recent.map { it.id })
    }

    @Test
    fun fewerThanLimit_returnsAll() = runTest {
        val recent = useCaseWith(3)().first()

        assertEquals(listOf(3, 2, 1), recent.map { it.id })
    }

    @Test
    fun respectsCustomLimit() = runTest {
        val recent = useCaseWith(8)(limit = 2).first()

        assertEquals(listOf(8, 7), recent.map { it.id })
    }

    @Test
    fun keepsTheNewestWithinRange() = runTest {
        val recent = useCaseWith(8)(DateRange(startMillis = 2L, endMillis = 5L), limit = 2).first()

        assertEquals(listOf(4, 3), recent.map { it.id })
    }

    @Test
    fun noTransactions_isEmpty() = runTest {
        assertTrue(useCaseWith(0)().first().isEmpty())
    }
}
