package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.MonthlyTrend

import com.example.expensetrackerpro.domain.models.FinancialSummary



data class HomeUiState(

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