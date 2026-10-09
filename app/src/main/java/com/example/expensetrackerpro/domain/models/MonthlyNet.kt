package com.example.expensetrackerpro.domain.models

/** Income minus expense for one calendar month: positive when money was saved. */
data class MonthlyNet(
    val range: DateRange,
    val net: Double,
    // Starts after the current moment, so it can't have any transactions yet
    val isFuture: Boolean
)
