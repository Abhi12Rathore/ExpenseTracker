package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Compares the balance of a week, month or year with the whole period before it. Emits null
 * for a custom range, which has no previous period of the same kind, and when neither period
 * has any income or expense.
 */
class GetPeriodComparisonUseCase @Inject constructor(
    private val getFinancialSummary: GetFinancialSummaryUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val stepPeriod: StepPeriodUseCase
) {

    operator fun invoke(filter: DateFilter): Flow<PeriodComparison?> {
        val period = filter as? DateFilter.Period ?: return flowOf(null)
        val previous = stepPeriod(period, forward = false) ?: return flowOf(null)
        val previousRange = getDateRange(previous)

        return combine(
            getFinancialSummary(getDateRange(period)),
            getFinancialSummary(previousRange)
        ) { current, before ->
            // With nothing recorded in either period there is nothing worth comparing
            if (current.isEmpty() && before.isEmpty()) {
                null
            } else {
                PeriodComparison(
                    granularity = period.granularity,
                    previousRange = previousRange,
                    balanceDifference = current.totalBalance - before.totalBalance
                )
            }
        }
    }

    private fun FinancialSummary.isEmpty(): Boolean = totalIncome == 0.0 && totalExpense == 0.0
}
