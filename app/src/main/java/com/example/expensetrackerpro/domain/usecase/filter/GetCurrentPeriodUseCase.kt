package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

/** The week, month or year we are in right now, anchored at its first moment. */
class GetCurrentPeriodUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(granularity: PeriodGranularity): DateFilter.Period {
        val start = timeProvider.now().apply { moveToStartOf(granularity) }

        return DateFilter.Period(granularity, start.timeInMillis)
    }
}
