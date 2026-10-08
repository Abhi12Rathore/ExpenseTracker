package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {

    /** Total expense per category within [range], highest first. */
    fun getCategoryBreakdown(range: DateRange): Flow<List<CategoryBreakdown>>

    /** Income and expense per day within [range], oldest first. */
    fun getCashFlow(range: DateRange): Flow<List<CashFlowPoint>>

    /** Income and expense per calendar month within [range], oldest first. */
    fun getMonthlyTrend(range: DateRange): Flow<List<MonthlyTrend>>
}
