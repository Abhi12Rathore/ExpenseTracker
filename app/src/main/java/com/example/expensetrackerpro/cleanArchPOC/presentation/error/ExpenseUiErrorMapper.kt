package com.example.expensetrackerpro.cleanArchPOC.presentation.error

import com.example.expensetrackerpro.cleanArchPOC.domain.error.AppError

fun AppError.toUiError(): ExpenseUiError {
    return when (this) {
        AppError.NoInternet ->
            ExpenseUiError.NoInternet

        AppError.Unauthorized ->
            ExpenseUiError.Unauthorized

        AppError.ServerError ->
            ExpenseUiError.ServerError

        AppError.Unknown ->
            ExpenseUiError.Unknown
    }
}