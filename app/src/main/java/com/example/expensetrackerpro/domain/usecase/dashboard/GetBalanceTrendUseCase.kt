package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.sumOfMoney
import com.example.expensetrackerpro.domain.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    operator fun invoke(range: DateRange): Flow<List<Money>> =
        getCashFlow(range).map { points -> cumulativeBalance(range, points) }

    private fun cumulativeBalance(range: DateRange, points: List<CashFlowPoint>): List<Money> {
        val nowMillis = timeProvider.now().timeInMillis
        var balance = Money.ZERO

        return timeProvider.now().dayRanges(range)
            .filter { it.startMillis <= nowMillis }
            .map { day ->
                balance += points.filter { it.dateMillis in day }.sumOfMoney { it.income - it.expense }
                balance
            }
    }
}
