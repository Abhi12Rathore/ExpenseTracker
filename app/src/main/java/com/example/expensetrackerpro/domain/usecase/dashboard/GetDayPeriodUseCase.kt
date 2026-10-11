package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DayPeriod
import javax.inject.Inject

/** Morning from 5 am until noon, afternoon until 5 pm, evening from then through the small hours. */
class GetDayPeriodUseCase @Inject constructor() {

    operator fun invoke(hourOfDay: Int): DayPeriod =
        DayPeriod.entries.lastOrNull { hourOfDay >= it.startHour } ?: DayPeriod.EVENING
}
