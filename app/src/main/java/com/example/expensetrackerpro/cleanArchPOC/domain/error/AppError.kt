package com.example.expensetrackerpro.cleanArchPOC.domain.error

sealed interface AppError {

    data object NoInternet : AppError

    data object Unauthorized : AppError

    data object ServerError : AppError

    data object Unknown : AppError
}