package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.MonthlyTrend

import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState



data class HomeUiState(

    // Time of day for the greeting
    val dayPeriod: DayPeriod = DayPeriod.MORNING,

    // The period every section below is filtered to
    val filter: PeriodFilterState = PeriodFilterState(),

    val financialSummary: FinancialSummary = FinancialSummary(),

    val categoryExpense:
    List<CategoryBreakdown> = emptyList(),


    val dailyCashFlow:
    List<CashFlowPoint> = emptyList(),


    val monthlyTrend:
    List<MonthlyTrend> = emptyList(),


    val recentTransactions:
    List<Transaction> = emptyList(),


    val isLoading: Boolean = false,

    val insights:
    List<SmartInsight> = emptyList()

)