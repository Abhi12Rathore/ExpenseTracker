package com.example.expensetrackerpro.domain.models

/** [startHour] is the hour of the day the period begins; the evening runs on through the small hours. */
enum class DayPeriod(val startHour: Int) {
    MORNING(5), AFTERNOON(12), EVENING(17)
}
