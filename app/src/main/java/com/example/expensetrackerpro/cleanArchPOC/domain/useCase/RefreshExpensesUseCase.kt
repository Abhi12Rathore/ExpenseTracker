package com.example.expensetrackerpro.cleanArchPOC.domain.useCase

import com.example.expensetrackerpro.cleanArchPOC.domain.repository.ExpenseRepository
import com.example.expensetrackerpro.cleanArchPOC.domain.result.RefreshExpensesResult
import javax.inject.Inject

class RefreshExpensesUseCase @Inject constructor(private val repository: ExpenseRepository) {
    suspend operator fun invoke(): RefreshExpensesResult {
       return repository.refreshExpenses()
    }

}