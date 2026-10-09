package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : DashboardRepository {

    override fun getCategoryBreakdown(range: DateRange): Flow<List<CategoryBreakdown>> =
        transactionDao.getCategoryExpense(range.startMillis, range.endMillis)
            .map { list -> list.map { it.toDomain() } }

    override fun getCashFlow(range: DateRange): Flow<List<CashFlowPoint>> =
        transactionDao.getDailyCashFlow(range.startMillis, range.endMillis)
            .map { list -> list.map { it.toDomain() } }

    override fun getDailyExpenses(range: DateRange): Flow<List<DailyExpense>> =
        transactionDao.getDailyExpense(range.startMillis, range.endMillis)
            .map { list -> list.map { it.toDomain() } }

    override fun getMonthlyTrend(range: DateRange): Flow<List<MonthlyTrend>> =
        transactionDao.getMonthlyTrend(range.startMillis, range.endMillis)
            .map { list -> list.map { it.toDomain() } }
}
