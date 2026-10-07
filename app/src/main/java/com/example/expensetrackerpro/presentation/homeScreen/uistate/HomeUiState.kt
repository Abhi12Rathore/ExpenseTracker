package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.data.local.entity.Transaction
import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.FinancialInsight
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity

import com.example.expensetrackerpro.presentation.homeScreen.viewmodels.FinancialSummaryData



data class HomeUiState(

    val financialSummary: FinancialSummaryData = FinancialSummaryData(),

    val categoryExpense:
    List<CategoryExpenseEntity> = emptyList(),


    val dailyCashFlow:
    List<DailyCashFlowEntity> = emptyList(),


    val monthlyTrend:
    List<MonthlyTrendEntity> = emptyList(),


    val recentTransactions:
    List<Transaction> = emptyList(),


    val isLoading: Boolean = false,

    val insights:
    List<FinancialInsight> = emptyList()

)