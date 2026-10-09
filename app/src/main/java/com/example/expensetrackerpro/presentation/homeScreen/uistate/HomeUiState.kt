package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.domain.models.CategoryRanking
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState

data class HomeUiState(

    // Time of day for the greeting
    val dayPeriod: DayPeriod = DayPeriod.MORNING,

    // The period every section below is filtered to
    val filter: PeriodFilterState = PeriodFilterState(),

    // True until the first data for the dashboard has arrived
    val isLoading: Boolean = false,

    val financialSummary: FinancialSummary = FinancialSummary(),

    // Null for a custom range, which has no previous period to compare with
    val periodComparison: PeriodComparison? = null,

    // Running balance at the end of each day so far, oldest first
    val balanceTrend: List<Double> = emptyList(),

    val spending: SpendingChart = SpendingChart(),

    val categoryRanking: CategoryRanking = CategoryRanking(),

    val monthlyTrend: List<MonthlyTrend> = emptyList(),

    val recentTransactions: List<Transaction> = emptyList(),

    val insights: List<SmartInsight> = emptyList()
)
