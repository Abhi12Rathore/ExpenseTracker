package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.rupees
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
            CashFlowPoint(dateMillis = 1L, income = rupees(100), expense = rupees(20)),
            CashFlowPoint(dateMillis = 2L, income = Money.ZERO, expense = rupees(50))
        )
        repository.cashFlow.value = points

        assertEquals(points, GetCashFlowUseCase(repository)().first())
    }

    @Test
    fun getCategoryBreakdown_returnsRepositoryOrder() = runTest {
        val breakdown = listOf(
            CategoryBreakdown(categoryId = 2, categoryName = "Bills", icon = CategoryIcon.RECEIPT_LONG, amount = rupees(900)),
            CategoryBreakdown(categoryId = 1, categoryName = "Food", icon = CategoryIcon.RESTAURANT, amount = rupees(300))
        )
        repository.categoryBreakdown.value = breakdown

        assertEquals(breakdown, GetCategoryBreakdownUseCase(repository)().first())
    }

    @Test
    fun getMonthlyTrend_returnsRepositoryMonths() = runTest {
        val trend = listOf(
            MonthlyTrend(year = 2026, month = 6, income = rupees(5000), expense = rupees(3000)),
            MonthlyTrend(year = 2026, month = 7, income = rupees(5200), expense = rupees(2800))
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
