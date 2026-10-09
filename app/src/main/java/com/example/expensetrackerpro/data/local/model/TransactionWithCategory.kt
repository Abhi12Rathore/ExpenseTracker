package com.example.expensetrackerpro.data.local.model

import androidx.room.Embedded
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.models.CategoryIcon

/** A transaction with what is read from its category rather than stored on it. */
data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    val categoryIcon: CategoryIcon
)
