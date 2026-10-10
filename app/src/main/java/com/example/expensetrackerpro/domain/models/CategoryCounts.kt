package com.example.expensetrackerpro.domain.models

/** How many categories there are, and how many of them the user added. */
data class CategoryCounts(
    val total: Int = 0,
    val custom: Int = 0
)
