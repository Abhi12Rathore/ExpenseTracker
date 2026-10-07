package com.example.expensetrackerpro.cleanArchPOC.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses_poc")
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val category: String,
    val createdAtMillis: Long
)
