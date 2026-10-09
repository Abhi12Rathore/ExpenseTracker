package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Serves whatever the test sets, and records the range each chart was last asked for. Daily
 * expenses are narrowed to the range asked for, since two periods are read side by side.
 */
class FakeDashboardRepository : DashboardRepository {

    val categoryBreakdown = MutableStateFlow<List<CategoryBreakdown>>(emptyList())
    val cashFlow = MutableStateFlow<List<CashFlowPoint>>(emptyList())
    val dailyExpenses = MutableStateFlow<List<DailyExpense>>(emptyList())
    val monthlyTrend = MutableStateFlow<List<MonthlyTrend>>(emptyList())

    var categoryBreakdownRange: DateRange? = null
        private set
    var cashFlowRange: DateRange? = null
        private set
    var dailyExpensesRange: DateRange? = null
        private set
    var monthlyTrendRange: DateRange? = null
        private set

    override fun getCategoryBreakdown(range: DateRange): Flow<List<CategoryBreakdown>> {
        categoryBreakdownRange = range
        return categoryBreakdown
    }

    override fun getCashFlow(range: DateRange): Flow<List<CashFlowPoint>> {
        cashFlowRange = range
        return cashFlow
    }

    override fun getDailyExpenses(range: DateRange): Flow<List<DailyExpense>> {
        dailyExpensesRange = range
        return dailyExpenses.map { list ->
            list.filter { it.dateMillis >= range.startMillis && it.dateMillis < range.endMillis }
        }
    }

    override fun getMonthlyTrend(range: DateRange): Flow<List<MonthlyTrend>> {
        monthlyTrendRange = range
        return monthlyTrend
    }
}
