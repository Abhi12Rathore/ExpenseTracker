package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.FinancialInsight
import com.example.expensetrackerpro.domain.models.MonthlyTrend

import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.FinancialSummaryData



data class HomeUiState(

    val financialSummary: FinancialSummaryData = FinancialSummaryData(),

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
    List<FinancialInsight> = emptyList()

)