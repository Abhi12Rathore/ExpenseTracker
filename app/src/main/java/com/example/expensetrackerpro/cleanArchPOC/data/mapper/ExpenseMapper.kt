package com.example.expensetrackerpro.cleanArchPOC.data.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.expensetrackerpro.cleanArchPOC.data.local.ExpenseEntity
import com.example.expensetrackerpro.cleanArchPOC.data.remote.ExpenseDto
import com.example.expensetrackerpro.cleanArchPOC.domain.model.Expense
import java.time.Instant

fun ExpenseDto.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        amount = amount,
        category = category,
        createdAtMillis = createdAtMillis
    )
}

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        amount = amount,
        category = category,
        createdAtMillis = createdAtMillis
    )
}