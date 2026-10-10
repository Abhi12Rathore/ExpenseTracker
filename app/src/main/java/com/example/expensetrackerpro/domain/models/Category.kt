package com.example.expensetrackerpro.domain.models

data class Category(
    val id: CategoryId = CategoryId.UNSAVED,
    val name: String,
    val type: TransactionType,
    val icon: CategoryIcon,
    val isCustom: Boolean = true,
)
