package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * The running balance (income minus expense since the start of the range) at the end of each
 * day, oldest first. Days that haven't started are left out, so the trend stops at today.
 */
class GetBalanceTrendUseCase @Inject constructor(
    private val getCashFlow: GetCashFlowUseCase,
    private val timeProvider: TimeProvider
) {

    /** [range] must be bounded and start at a local midnight. */
    operator fun invoke(range: DateRange): Flow<List<Double>> =
        getCashFlow(range).map { points -> cumulativeBalance(range, points) }

    private fun cumulativeBalance(range: DateRange, points: List<CashFlowPoint>): List<Double> {
        val nowMillis = timeProvider.now().timeInMillis
        val cursor = timeProvider.now().apply { timeInMillis = range.startMillis }

        return buildList {
            var balance = 0.0
            var dayStart = range.startMillis
            while (dayStart < range.endMillis && dayStart <= nowMillis) {
                cursor.add(Calendar.DAY_OF_MONTH, 1)
                val dayEnd = cursor.timeInMillis

                balance += points
                    .filter { it.dateMillis >= dayStart && it.dateMillis < dayEnd }
                    .sumOf { it.income - it.expense }
                add(balance)
                dayStart = dayEnd
            }
        }
    }
}
