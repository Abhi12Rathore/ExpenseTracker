package com.example.expensetrackerpro.domain.usecase.filter

import com.example.expensetrackerpro.domain.models.ComparableRange
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import javax.inject.Inject

/**
 * The part of the previous period to compare a period with. A period that is still under way
 * is compared with the previous one up to the same day (1–9 Oct against 1–9 Sep, Mon–Thu
 * against Mon–Thu, 1 Jan–today against the same dates last year), so a half-finished month is
 * not measured against a whole one. A finished period is compared with the whole previous one.
 */
class GetComparableRangeUseCase @Inject constructor(
    private val timeProvider: TimeProvider,
    private val getDateRange: GetDateRangeUseCase,
    private val stepPeriod: StepPeriodUseCase
) {

    /** Null only for a period that has no reachable previous one. */
    operator fun invoke(period: DateFilter.Period): ComparableRange? {
        val previousPeriod = stepPeriod(period, forward = false) ?: return null
        val currentRange = getDateRange(period)
        val previousRange = getDateRange(previousPeriod)

        val today = timeProvider.now()
        val nowMillis = today.timeInMillis
        val isOngoing = nowMillis >= currentRange.startMillis && nowMillis < currentRange.endMillis
        if (!isOngoing) {
            return ComparableRange(range = previousRange, wholePeriod = previousRange, isPartial = false)
        }

        today.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // The same day one period back. A day the shorter month or year doesn't have
            // lands on its last day: 31 Mar on 28 or 29 Feb, 29 Feb on 28 Feb
            add(period.granularity.calendarField, -1)
            // The range ends where the following day begins
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val endMillis = minOf(today.timeInMillis, previousRange.endMillis)

        return ComparableRange(
            range = DateRange(previousRange.startMillis, endMillis),
            wholePeriod = previousRange,
            isPartial = true
        )
    }
}
