package com.example.expensetrackerpro.cleanArchPOC.data.repository

import com.example.expensetrackerpro.cleanArchPOC.data.error.toAppError
import com.example.expensetrackerpro.cleanArchPOC.data.local.ExpenseDao
import com.example.expensetrackerpro.cleanArchPOC.data.mapper.toDomain
import com.example.expensetrackerpro.cleanArchPOC.data.mapper.toEntity
import com.example.expensetrackerpro.cleanArchPOC.data.remote.ExpenseApi
import com.example.expensetrackerpro.cleanArchPOC.domain.model.Expense
import com.example.expensetrackerpro.cleanArchPOC.domain.repository.ExpenseRepository
import com.example.expensetrackerpro.cleanArchPOC.domain.result.RefreshExpensesResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseApi: ExpenseApi,
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override fun observeExpenses(): Flow<List<Expense>> {
        return expenseDao.observeExpenses().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun refreshExpenses(): RefreshExpensesResult {
        return try {
            val remoteExpenses = expenseApi.getExpenses()
            val entities = remoteExpenses.map { dto ->
                dto.toEntity()
            }
            expenseDao.insertExpenses(entities)
            RefreshExpensesResult.Success
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            }
            RefreshExpensesResult.Failure(
                error = throwable.toAppError()
            )
        }
    }
}
