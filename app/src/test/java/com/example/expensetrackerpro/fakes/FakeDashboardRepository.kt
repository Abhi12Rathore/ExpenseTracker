package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDashboardRepository : DashboardRepository {

    val categoryBreakdown = MutableStateFlow<List<CategoryBreakdown>>(emptyList())
    val cashFlow = MutableStateFlow<List<CashFlowPoint>>(emptyList())
    val monthlyTrend = MutableStateFlow<List<MonthlyTrend>>(emptyList())

    override fun getCategoryBreakdown(): Flow<List<CategoryBreakdown>> = categoryBreakdown

    override fun getCashFlow(): Flow<List<CashFlowPoint>> = cashFlow

    override fun getMonthlyTrend(): Flow<List<MonthlyTrend>> = monthlyTrend
}
