package com.example.expensetrackerpro.domain.models

data class CategoryShare(
    val category: CategoryBreakdown,
    /** This category's part of the total spend, from 0 to 1. */
    val fraction: Float
)

/** Spending per category, highest first: the leaders in [top] and the tail grouped in [others]. */
data class CategoryRanking(
    val total: Money = Money.ZERO,
    val top: List<CategoryShare> = emptyList(),
    val others: List<CategoryShare> = emptyList()
) {
    val categoryCount: Int get() = top.size + others.size

    val othersAmount: Money get() = others.sumOfMoney { it.category.amount }

    val othersFraction: Float get() = othersAmount.ratioTo(total)
}
