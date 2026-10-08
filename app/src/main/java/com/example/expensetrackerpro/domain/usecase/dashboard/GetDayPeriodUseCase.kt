package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DayPeriod
import javax.inject.Inject

/** Morning from 5 am until noon, afternoon until 5 pm, evening from then through the small hours. */
class GetDayPeriodUseCase @Inject constructor() {

    operator fun invoke(hourOfDay: Int): DayPeriod = when (hourOfDay) {
        in 5..11 -> DayPeriod.MORNING
        in 12..16 -> DayPeriod.AFTERNOON
        else -> DayPeriod.EVENING
    }
}
