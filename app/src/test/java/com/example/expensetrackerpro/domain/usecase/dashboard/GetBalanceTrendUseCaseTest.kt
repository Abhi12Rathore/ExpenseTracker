package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetBalanceTrendUseCaseTest {

    private val repository = FakeDashboardRepository()

    // Thursday 8 Oct 2026
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

    private val october =
        DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val september =
        DateRange(millisAt(2026, Calendar.SEPTEMBER, 1), millisAt(2026, Calendar.OCTOBER, 1))

    private suspend fun trend(range: DateRange): List<Double> =
        GetBalanceTrendUseCase(GetCashFlowUseCase(repository), FakeTimeProvider(now))(range).first()

    @Test
    fun accumulatesIncomeMinusExpenseDayByDay() = runTest {
        repository.cashFlow.value = listOf(
            CashFlowPoint(millisAt(2026, Calendar.OCTOBER, 1, hour = 10), income = 1000.0, expense = 0.0),
            CashFlowPoint(millisAt(2026, Calendar.OCTOBER, 3, hour = 23), income = 0.0, expense = 250.0),
            CashFlowPoint(millisAt(2026, Calendar.OCTOBER, 4), income = 100.0, expense = 400.0)
        )

        assertEquals(
            listOf(1000.0, 1000.0, 750.0, 450.0, 450.0, 450.0, 450.0, 450.0),
            trend(october)
        )
    }

    @Test
    fun currentPeriod_stopsAtToday() = runTest {
        assertEquals(8, trend(october).size)
        assertEquals(october, repository.cashFlowRange)
    }

    @Test
    fun pastPeriod_hasAPointForEveryDay() = runTest {
        repository.cashFlow.value = listOf(
            CashFlowPoint(millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23), income = 0.0, expense = 80.0)
        )

        val trend = trend(september)

        assertEquals(30, trend.size)
        assertEquals(0.0, trend[28], 0.001)
        assertEquals(-80.0, trend.last(), 0.001)
    }

    @Test
    fun noTransactions_isAFlatZeroLine() = runTest {
        assertEquals(List(8) { 0.0 }, trend(october))
    }

    @Test
    fun periodThatHasNotStarted_isEmpty() = runTest {
        val november =
            DateRange(millisAt(2026, Calendar.NOVEMBER, 1), millisAt(2026, Calendar.DECEMBER, 1))

        assertTrue(trend(november).isEmpty())
    }
}
