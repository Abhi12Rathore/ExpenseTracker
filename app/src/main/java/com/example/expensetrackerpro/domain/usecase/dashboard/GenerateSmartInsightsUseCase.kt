package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight
import javax.inject.Inject

class GenerateSmartInsightsUseCase @Inject constructor() {

    /**
     * [categoryBreakdown] is expected highest-spending first and
     * [monthlyTrend] oldest month first.
     */
    operator fun invoke(
        summary: FinancialSummary,
        categoryBreakdown: List<CategoryBreakdown>,
        monthlyTrend: List<MonthlyTrend>
    ): List<SmartInsight> = buildList {
        if (summary.totalBalance > 0) {
            add(SmartInsight.Saved(summary.totalBalance))
        }

        categoryBreakdown.firstOrNull()?.let {
            add(SmartInsight.HighestSpending(it.categoryName))
        }

        if (monthlyTrend.size >= 2) {
            val current = monthlyTrend.last()
            val previous = monthlyTrend[monthlyTrend.size - 2]

            if (current.expense < previous.expense) {
                add(SmartInsight.SpendingImproved)
            } else {
                add(SmartInsight.SpendingIncreased)
            }
        }
    }
}
