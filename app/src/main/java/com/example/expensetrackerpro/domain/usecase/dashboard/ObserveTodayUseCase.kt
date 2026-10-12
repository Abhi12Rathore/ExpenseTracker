package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.time.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject

/** The start of today in epoch millis, and again at each midnight, for as long as it is collected. */
class ObserveTodayUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(): Flow<Long> = flow {
        while (true) {
            // Read again after every wait, so waking late or after a clock change still gives the right day
            val now = timeProvider.now()
            emit(now.startOfDay(daysAhead = 0))
            delay(now.startOfDay(daysAhead = 1) - now.timeInMillis)
        }
    }.distinctUntilChanged()

    private fun Calendar.startOfDay(daysAhead: Int): Long =
        (clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, daysAhead)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
