package com.example.expensetrackerpro.domain.models

/**
 * A transaction the user enters again and again: the same title and category, at least a few
 * times lately. Offered on a new transaction so that a repeat takes one tap.
 */
data class FrequentTransaction(
    /** As the user wrote it the last time. */
    val title: String,
    val categoryId: CategoryId,
    val categoryName: String,
    val categoryIcon: CategoryIcon,
    val type: TransactionType,
    /** The median of its amounts, so one unusual amount doesn't move it. */
    val usualAmount: Money,
    /** How many times it came up in the period looked at. */
    val count: Int
)
