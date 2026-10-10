package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
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

    private suspend fun trend(range: DateRange): List<Money> =
        GetBalanceTrendUseCase(GetCashFlowUseCase(repository), FakeTimeProvider(now))(range).first()

    @Test
    fun accumulatesIncomeMinusExpenseDayByDay() = runTest {
        repository.cashFlow.value = listOf(
            CashFlowPoint(millisAt(2026, Calendar.OCTOBER, 1, hour = 10), income = rupees(1000), expense = Money.ZERO),
            CashFlowPoint(millisAt(2026, Calendar.OCTOBER, 3, hour = 23), income = Money.ZERO, expense = rupees(250)),
            CashFlowPoint(millisAt(2026, Calendar.OCTOBER, 4), income = rupees(100), expense = rupees(400))
        )

        assertEquals(
            listOf(rupees(1000), rupees(1000), rupees(750), rupees(450), rupees(450), rupees(450), rupees(450), rupees(450)),
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
            CashFlowPoint(millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23), income = Money.ZERO, expense = rupees(80))
        )

        val trend = trend(september)

        assertEquals(30, trend.size)
        assertEquals(Money.ZERO, trend[28])
        assertEquals(rupees(-80), trend.last())
    }

    @Test
    fun noTransactions_isAFlatZeroLine() = runTest {
        assertEquals(List(8) { Money.ZERO }, trend(october))
    }

    @Test
    fun periodThatHasNotStarted_isEmpty() = runTest {
        val november =
            DateRange(millisAt(2026, Calendar.NOVEMBER, 1), millisAt(2026, Calendar.DECEMBER, 1))

        assertTrue(trend(november).isEmpty())
    }
}
