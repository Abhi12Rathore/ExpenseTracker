package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionDayGroup
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.sumOfMoney
import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import javax.inject.Inject

/** Splits transactions into the local days they are dated on, newest day first. */
class GroupTransactionsByDayUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    /** Within a day the transactions keep the order they came in. */
    operator fun invoke(transactions: List<Transaction>): List<TransactionDayGroup> {
        // One calendar for every lookup; it carries the zone that decides where a day starts
        val calendar = timeProvider.now()
        val todayStart = calendar.startOfDay(calendar.timeInMillis)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val yesterdayStart = calendar.timeInMillis

        return transactions
            .groupBy { calendar.startOfDay(it.dateMillis) }
            .map { (dayStart, onThatDay) ->
                TransactionDayGroup(
                    dayStartMillis = dayStart,
                    label = when (dayStart) {
                        todayStart -> DayLabel.TODAY
                        yesterdayStart -> DayLabel.YESTERDAY
                        else -> DayLabel.OTHER
                    },
                    net = onThatDay.sumOfMoney { it.signedAmount },
                    transactions = onThatDay
                )
            }
            .sortedByDescending { it.dayStartMillis }
    }

    /** Moves this calendar to the midnight that starts [millis]'s day and returns it. */
    private fun Calendar.startOfDay(millis: Long): Long {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        return timeInMillis
    }
}

/** The amount as it moves the balance: positive for income, negative for an expense. */
internal val Transaction.signedAmount: Money
    get() = if (transactionType == TransactionType.INCOME) amount else -amount
