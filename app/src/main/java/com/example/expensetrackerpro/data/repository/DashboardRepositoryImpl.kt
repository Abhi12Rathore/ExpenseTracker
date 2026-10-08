package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : DashboardRepository {

    override fun getCategoryBreakdown(): Flow<List<CategoryBreakdown>> =
        transactionDao.getCategoryExpense().map { list -> list.map { it.toDomain() } }

    override fun getCashFlow(): Flow<List<CashFlowPoint>> =
        transactionDao.getDailyCashFlow().map { list -> list.map { it.toDomain() } }

    override fun getMonthlyTrend(): Flow<List<MonthlyTrend>> =
        transactionDao.getMonthlyTrend().map { list -> list.map { it.toDomain() } }
}
