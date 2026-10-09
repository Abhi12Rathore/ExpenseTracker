package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.MonthlyNet
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GetNetSavingsByMonthUseCaseTest {

    private val repository = FakeDashboardRepository()

    // Thursday 8 Oct 2026
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 8, hour = 12))

    private val getNetSavingsByMonth = GetNetSavingsByMonthUseCase(
        GetMonthlyTrendUseCase(repository),
        GetDateRangeUseCase(timeProvider),
        timeProvider
    )

    private fun year(year: Int) =
        DateFilter.Period(PeriodGranularity.YEAR, millisAt(year, Calendar.JANUARY, 1))

    private suspend fun netSavings(filter: DateFilter): List<MonthlyNet> = getNetSavingsByMonth(filter).first()

    @Test
    fun year_hasOneEntryPerCalendarMonth_januaryFirst() = runTest {
        val months = netSavings(year(2026))

        assertEquals(12, months.size)
        assertEquals(
            DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2026, Calendar.FEBRUARY, 1)),
            months.first().range
        )
        assertEquals(
            DateRange(millisAt(2026, Calendar.DECEMBER, 1), millisAt(2027, Calendar.JANUARY, 1)),
            months.last().range
        )
        months.zipWithNext { earlier, later ->
            assertEquals(earlier.range.endMillis, later.range.startMillis)
        }
        assertEquals(
            DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1)),
            repository.monthlyTrendRange
        )
    }

    @Test
    fun netIsIncomeMinusExpense_negativeWhenOverspent_zeroWhenNothingHappened() = runTest {
        repository.monthlyTrend.value = listOf(
            MonthlyTrend(year = 2026, month = 1, income = 5000.0, expense = 3000.0),
            MonthlyTrend(year = 2026, month = 3, income = 1000.0, expense = 2500.0),
            MonthlyTrend(year = 2026, month = 10, income = 0.0, expense = 400.0)
        )

        val nets = netSavings(year(2026)).map { it.net }

        assertEquals(
            listOf(2000.0, 0.0, -1500.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -400.0, 0.0, 0.0),
            nets
        )
    }

    @Test
    fun monthsOfAnotherYear_areNotMixedIn() = runTest {
        repository.monthlyTrend.value = listOf(
            MonthlyTrend(year = 2025, month = 2, income = 900.0, expense = 0.0),
            MonthlyTrend(year = 2026, month = 2, income = 100.0, expense = 0.0)
        )

        assertEquals(100.0, netSavings(year(2026))[1].net, 0.001)
    }

    @Test
    fun monthsThatHaveNotStarted_areFuture_theCurrentOneIsNot() = runTest {
        val months = netSavings(year(2026))

        // October is under way; November and December are not
        assertEquals(List(10) { false } + listOf(true, true), months.map { it.isFuture })
    }

    @Test
    fun pastYear_hasNoFutureMonths() = runTest {
        assertTrue(netSavings(year(2025)).none { it.isFuture })
    }

    @Test
    fun anythingShorterThanAYear_hasNoMonthlyBreakdown() = runTest {
        repository.monthlyTrend.value =
            listOf(MonthlyTrend(year = 2026, month = 10, income = 500.0, expense = 100.0))

        assertTrue(
            netSavings(DateFilter.Period(PeriodGranularity.MONTH, millisAt(2026, Calendar.OCTOBER, 1))).isEmpty()
        )
        assertTrue(
            netSavings(DateFilter.Period(PeriodGranularity.WEEK, millisAt(2026, Calendar.OCTOBER, 4))).isEmpty()
        )
        assertTrue(
            netSavings(
                DateFilter.Custom(
                    DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))
                )
            ).isEmpty()
        )
    }
}
