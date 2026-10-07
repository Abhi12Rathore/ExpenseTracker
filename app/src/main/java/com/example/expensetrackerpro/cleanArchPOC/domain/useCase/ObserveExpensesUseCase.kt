package com.example.expensetrackerpro.cleanArchPOC.domain.useCase

import com.example.expensetrackerpro.cleanArchPOC.domain.model.Expense
import com.example.expensetrackerpro.cleanArchPOC.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExpensesUseCase @Inject constructor(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<Expense>> {
        return repository.observeExpenses()
    }
}