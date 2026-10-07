package com.example.expensetrackerpro.cleanArchPOC.presentation.error

sealed interface ExpenseUiError {

    data object NoInternet : ExpenseUiError

    data object Unauthorized : ExpenseUiError

    data object ServerError : ExpenseUiError

    data object Unknown : ExpenseUiError
}