package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.models.CurrencyPreview
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDashboardUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** The user's own Home figures for this month, which the Currency screen writes in each currency it previews. */
class GetCurrencyPreviewUseCase @Inject constructor(
    private val getDashboard: GetDashboardUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase
) {

    suspend operator fun invoke(): CurrencyPreview {
        val dashboard = getDashboard(getCurrentPeriod(PeriodGranularity.MONTH)).first()

        return CurrencyPreview(
            summary = dashboard.summary,
            topCategory = dashboard.categoryRanking.top.firstOrNull()?.category
        )
    }
}
