package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

/** Moves a period one week, month or year forward or back. */
class StepPeriodUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    /** The neighbouring period, or null when stepping [forward] would land on one that hasn't started. */
    operator fun invoke(period: DateFilter.Period, forward: Boolean): DateFilter.Period? {
        val now = timeProvider.now()
        val target = timeProvider.now().apply {
            timeInMillis = period.anchorMillis
            // Step from the period start, so a 31st can't spill into the following month
            moveToStartOf(period.granularity)
            add(period.granularity.calendarField, if (forward) 1 else -1)
        }

        if (target.timeInMillis > now.timeInMillis) return null

        return period.copy(anchorMillis = target.timeInMillis)
    }
}
