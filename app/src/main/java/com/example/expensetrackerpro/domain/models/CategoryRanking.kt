package com.example.expensetrackerpro.domain.models

data class CategoryShare(
    val category: CategoryBreakdown,
    /** This category's part of the total spend, from 0 to 1. */
    val fraction: Float
)

/** Spending per category, highest first: the leaders in [top] and the tail grouped in [others]. */
data class CategoryRanking(
    val total: Double = 0.0,
    val top: List<CategoryShare> = emptyList(),
    val others: List<CategoryShare> = emptyList()
) {
    val categoryCount: Int get() = top.size + others.size

    val othersAmount: Double get() = others.sumOf { it.category.amount }

    val othersFraction: Float get() = if (total > 0) (othersAmount / total).toFloat() else 0f
}
