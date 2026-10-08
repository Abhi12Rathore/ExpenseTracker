package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardChartUseCasesTest {

    private val repository = FakeDashboardRepository()

    @Test
    fun getCashFlow_returnsRepositoryPoints() = runTest {
        val points = listOf(
            CashFlowPoint(dateMillis = 1L, income = 100.0, expense = 20.0),
            CashFlowPoint(dateMillis = 2L, income = 0.0, expense = 50.0)
        )
        repository.cashFlow.value = points

        assertEquals(points, GetCashFlowUseCase(repository)().first())
    }

    @Test
    fun getCategoryBreakdown_returnsRepositoryOrder() = runTest {
        val breakdown = listOf(
            CategoryBreakdown(categoryId = 2, categoryName = "Bills", amount = 900.0),
            CategoryBreakdown(categoryId = 1, categoryName = "Food", amount = 300.0)
        )
        repository.categoryBreakdown.value = breakdown

        assertEquals(breakdown, GetCategoryBreakdownUseCase(repository)().first())
    }

    @Test
    fun getMonthlyTrend_returnsRepositoryMonths() = runTest {
        val trend = listOf(
            MonthlyTrend(year = 2026, month = 6, income = 5000.0, expense = 3000.0),
            MonthlyTrend(year = 2026, month = 7, income = 5200.0, expense = 2800.0)
        )
        repository.monthlyTrend.value = trend

        assertEquals(trend, GetMonthlyTrendUseCase(repository)().first())
    }

    @Test
    fun eachChart_asksTheRepositoryForItsRange() = runTest {
        val range = DateRange(startMillis = 10L, endMillis = 20L)

        GetCashFlowUseCase(repository)(range).first()
        assertEquals(range, repository.cashFlowRange)
        GetCategoryBreakdownUseCase(repository)(range).first()
        assertEquals(range, repository.categoryBreakdownRange)
        GetMonthlyTrendUseCase(repository)(range).first()
        assertEquals(range, repository.monthlyTrendRange)
    }

    @Test
    fun noRange_asksForEverything() = runTest {
        GetCashFlowUseCase(repository)().first()

        assertEquals(DateRange.ALL, repository.cashFlowRange)
    }

    @Test
    fun emptyRepository_emitsEmptyLists() = runTest {
        assertTrue(GetCashFlowUseCase(repository)().first().isEmpty())
        assertTrue(GetCategoryBreakdownUseCase(repository)().first().isEmpty())
        assertTrue(GetMonthlyTrendUseCase(repository)().first().isEmpty())
    }
}
