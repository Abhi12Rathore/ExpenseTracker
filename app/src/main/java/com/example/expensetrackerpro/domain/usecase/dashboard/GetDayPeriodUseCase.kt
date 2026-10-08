package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DayPeriod
import javax.inject.Inject

/** Morning until noon, afternoon until 5 pm, evening for the rest of the day. */
class GetDayPeriodUseCase @Inject constructor() {

    operator fun invoke(hourOfDay: Int): DayPeriod = when (hourOfDay) {
        in 0..11 -> DayPeriod.MORNING
        in 12..16 -> DayPeriod.AFTERNOON
        else -> DayPeriod.EVENING
    }
}
