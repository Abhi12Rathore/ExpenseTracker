package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.usecase.filter.GetComparableRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Compares the balance of a week, month or year with the period before it, like for like: a
 * period still under way is measured against the previous one up to the same day. Emits null
 * for a custom range, which has no previous period of the same kind, and when neither period
 * has any income or expense.
 */
class GetPeriodComparisonUseCase @Inject constructor(
    private val getFinancialSummary: GetFinancialSummaryUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val getComparableRange: GetComparableRangeUseCase
) {

    operator fun invoke(filter: DateFilter): Flow<PeriodComparison?> {
        val period = filter as? DateFilter.Period ?: return flowOf(null)
        val comparable = getComparableRange(period) ?: return flowOf(null)

        return combine(
            getFinancialSummary(getDateRange(period)),
            getFinancialSummary(comparable.range)
        ) { current, before ->
            // With nothing recorded in either period there is nothing worth comparing
            if (current.isEmpty() && before.isEmpty()) {
                null
            } else {
                PeriodComparison(
                    granularity = period.granularity,
                    previousRange = comparable.range,
                    balanceDifference = current.totalBalance - before.totalBalance,
                    isPartial = comparable.isPartial
                )
            }
        }
    }

    private fun FinancialSummary.isEmpty(): Boolean = totalIncome.isZero && totalExpense.isZero
}
