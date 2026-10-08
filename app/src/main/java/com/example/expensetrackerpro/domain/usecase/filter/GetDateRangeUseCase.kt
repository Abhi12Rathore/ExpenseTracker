package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

/** Resolves a [DateFilter] to the whole local days it covers. */
class GetDateRangeUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(filter: DateFilter): DateRange = when (filter) {
        is DateFilter.Custom -> filter.range
        is DateFilter.Period -> {
            // The provider's calendar supplies the time zone and the first day of the week
            val calendar = timeProvider.now().apply {
                timeInMillis = filter.anchorMillis
                moveToStartOf(filter.granularity)
            }
            val startMillis = calendar.timeInMillis
            calendar.add(filter.granularity.calendarField, 1)

            DateRange(startMillis, calendar.timeInMillis)
        }
    }
}
