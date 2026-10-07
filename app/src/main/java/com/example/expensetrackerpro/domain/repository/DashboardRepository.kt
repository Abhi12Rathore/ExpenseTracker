package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.data.local.entity.Transaction
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight

interface DashboardRepository {

    suspend fun getFinancialSummary(
        range: DateRange
    ): FinancialSummary

    suspend fun getCashFlow(
        range: DateRange
    ): List<CashFlowPoint>

    suspend fun getCategoryBreakdown(
        range: DateRange
    ): List<CategoryBreakdown>

    suspend fun getMonthlyTrend(): List<MonthlyTrend>

    suspend fun getRecentTransactions(
        limit: Int = 5
    ): List<Transaction>

    suspend fun getSmartInsights(
        range: DateRange
    ): List<SmartInsight>
}