package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {

    /** Total expense per category, highest first. */
    fun getCategoryBreakdown(): Flow<List<CategoryBreakdown>>

    /** Income and expense per day, oldest first. */
    fun getCashFlow(): Flow<List<CashFlowPoint>>

    /** Income and expense per calendar month, oldest first. */
    fun getMonthlyTrend(): Flow<List<MonthlyTrend>>
}
