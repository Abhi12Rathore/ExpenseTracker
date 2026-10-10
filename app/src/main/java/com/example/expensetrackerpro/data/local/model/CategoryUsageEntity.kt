package com.example.expensetrackerpro.data.local.model

import androidx.room.Embedded
import com.example.expensetrackerpro.data.local.entity.CategoryEntity

data class CategoryUsageEntity(
    @Embedded val category: CategoryEntity,
    val totalCount: Int,
    val periodCount: Int,
    val periodAmount: Double
)
