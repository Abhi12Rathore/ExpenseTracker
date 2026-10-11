package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.time.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject

/** The time of day now, and again each time a new period begins, for as long as it is collected. */
class ObserveDayPeriodUseCase @Inject constructor(
    private val timeProvider: TimeProvider,
    private val getDayPeriod: GetDayPeriodUseCase
) {

    operator fun invoke(): Flow<DayPeriod> = flow {
        while (true) {
            // Read again after every wait, so waking late or after a clock change still gives the right period
            val now = timeProvider.now()
            emit(getDayPeriod(now.get(Calendar.HOUR_OF_DAY)))
            delay(millisUntilNextPeriod(now))
        }
    }.distinctUntilChanged()

    private fun millisUntilNextPeriod(now: Calendar): Long {
        val todaysStarts = DayPeriod.entries.map { now.atHour(it.startHour) }
        // After the last start of today, the next one is tomorrow's first
        val tomorrowsFirst = now.atHour(DayPeriod.entries.first().startHour, daysAhead = 1)

        val next = (todaysStarts + tomorrowsFirst).first { it > now.timeInMillis }
        return next - now.timeInMillis
    }

    private fun Calendar.atHour(hour: Int, daysAhead: Int = 0): Long =
        (clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, daysAhead)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
