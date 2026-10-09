package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.DateSelection
import com.example.expensetrackerpro.domain.models.TransactionDate
import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * Turns the date chosen in the transaction form into the instant to store. Shortcuts and picked
 * dates keep the current time of day, and nothing resolves to later than now.
 */
class ResolveTransactionDateUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(selection: DateSelection): TransactionDate {
        val now = timeProvider.now()
        val millis = when (selection) {
            DateSelection.Today -> now.timeInMillis
            DateSelection.Yesterday -> daysAgo(1).timeInMillis
            is DateSelection.Picked -> minOf(localDate(selection.utcDayMillis), now.timeInMillis)
            is DateSelection.Stored -> selection.millis
        }

        return TransactionDate(
            millis = millis,
            choice = when {
                isSameDay(millis, now) -> DateChoice.TODAY
                isSameDay(millis, daysAgo(1)) -> DateChoice.YESTERDAY
                else -> DateChoice.OTHER
            },
            pickerUtcMillis = utcDay(timeProvider.now().apply { timeInMillis = millis }),
            maxPickerUtcMillis = utcDay(now)
        )
    }

    // Midnight UTC of the calendar date [local] falls on
    private fun utcDay(local: Calendar): Long =
        utc().apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis

    private fun daysAgo(days: Int): Calendar =
        timeProvider.now().apply { add(Calendar.DAY_OF_MONTH, -days) }

    // Pickers report a date as midnight UTC, so it is re-read as a calendar date in the local zone
    private fun localDate(utcDayMillis: Long): Long {
        val picked = utc().apply { timeInMillis = utcDayMillis }

        return timeProvider.now().apply {
            set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    private fun isSameDay(millis: Long, day: Calendar): Boolean {
        val other = timeProvider.now().apply { timeInMillis = millis }

        return other.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
            other.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    }

    private fun utc(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
}
