package com.example.expensetrackerpro.cleanArchPOC.data.error

import com.example.expensetrackerpro.cleanArchPOC.domain.error.AppError
import java.io.IOException

fun Throwable.toAppError(): AppError {
    return when (this) {
        is IOException -> AppError.NoInternet
        else -> AppError.Unknown
    }
}