package com.example.expensetrackerpro.domain.models

data class Category(
    val id: Int = 0,
    val name: String,
    val type: TransactionType,
    val icon: CategoryIcon,
    val isCustom: Boolean = true,
)
