package com.example.expensetrackerpro.data.local.model

import com.example.expensetrackerpro.domain.models.CategoryIcon

data class CategoryExpenseEntity(
    val categoryId: Long,
    val categoryName: String,
    val icon: CategoryIcon,
    val totalPaise: Long
)
