package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateSavingsRateUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GenerateSmartInsightsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCashFlowUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCategoryBreakdownUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetMonthlyTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetRecentTransactionsUseCase
import com.example.expensetrackerpro.fakes.FakeDashboardRepository
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.HomeViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val transactions = FakeTransactionRepository()
    private val dashboard = FakeDashboardRepository()

    private fun viewModel() = HomeViewModel(
        getFinancialSummary = GetFinancialSummaryUseCase(
            transactions,
            CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())
        ),
        getRecentTransactions = GetRecentTransactionsUseCase(transactions),
        getCategoryBreakdown = GetCategoryBreakdownUseCase(dashboard),
        getCashFlow = GetCashFlowUseCase(dashboard),
        getMonthlyTrend = GetMonthlyTrendUseCase(dashboard),
        generateSmartInsights = GenerateSmartInsightsUseCase()
    )

    @Test
    fun noData_isEmptyState() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(HomeUiState(), viewModel.uiState.value)
    }

    @Test
    fun exposesSummaryRecentAndCharts() = runTest {
        (1..7).forEach {
            transactions.saveTransaction(
                transaction(id = it, amount = 100.0, type = TransactionType.EXPENSE, dateMillis = it.toLong())
            )
        }
        transactions.saveTransaction(
            transaction(id = 8, amount = 1400.0, type = TransactionType.INCOME, dateMillis = 8L)
        )
        val breakdown = listOf(CategoryBreakdown(categoryId = 1, categoryName = "Food", amount = 700.0))
        dashboard.categoryBreakdown.value = breakdown

        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(1400.0, state.financialSummary.totalIncome, 0.001)
        assertEquals(700.0, state.financialSummary.totalExpense, 0.001)
        assertEquals(50f, state.financialSummary.savingsRate, 0.001f)
        assertEquals(listOf(8, 7, 6, 5, 4), state.recentTransactions.map { it.id })
        assertEquals(breakdown, state.categoryExpense)
    }

    @Test
    fun insightsFollowTheData() = runTest {
        transactions.saveTransaction(transaction(id = 1, amount = 500.0, type = TransactionType.INCOME))
        dashboard.categoryBreakdown.value =
            listOf(CategoryBreakdown(categoryId = 1, categoryName = "Bills", amount = 90.0))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(
            listOf(SmartInsight.Saved(500.0), SmartInsight.HighestSpending("Bills")),
            viewModel.uiState.value.insights
        )

        dashboard.monthlyTrend.value = listOf(
            MonthlyTrend(year = 2026, month = 6, income = 0.0, expense = 100.0),
            MonthlyTrend(year = 2026, month = 7, income = 0.0, expense = 300.0)
        )

        assertEquals(
            listOf(
                SmartInsight.Saved(500.0),
                SmartInsight.HighestSpending("Bills"),
                SmartInsight.SpendingIncreased
            ),
            viewModel.uiState.value.insights
        )
    }
}
