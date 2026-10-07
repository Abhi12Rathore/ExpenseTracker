package com.example.expensetrackerpro.cleanArchPOC.domain.result

import com.example.expensetrackerpro.cleanArchPOC.domain.error.AppError

sealed interface RefreshExpensesResult {

    data object Success : RefreshExpensesResult

    data class Failure(val error: AppError) : RefreshExpensesResult
}