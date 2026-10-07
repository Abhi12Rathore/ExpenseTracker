package com.example.expensetrackerpro.cleanArchPOC.domain.repository

import com.example.expensetrackerpro.cleanArchPOC.domain.model.Expense
import com.example.expensetrackerpro.cleanArchPOC.domain.result.RefreshExpensesResult
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<Expense>>
    suspend fun refreshExpenses(): RefreshExpensesResult
}