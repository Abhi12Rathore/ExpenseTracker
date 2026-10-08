package com.example.expensetrackerpro.domain.models

/** A span of time from [startMillis] (inclusive) to [endMillis] (exclusive). */
data class DateRange(
    val startMillis: Long,
    val endMillis: Long
) {
    companion object {
        val ALL = DateRange(Long.MIN_VALUE, Long.MAX_VALUE)
    }
}
