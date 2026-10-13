package com.example.expensetrackerpro.domain.time

fun interface HourFormatProvider {

    /** True when the user has set their phone to the 24-hour clock (`16:20`), false for `4:20 pm`. */
    fun uses24Hours(): Boolean
}
