package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMonthlyTrendUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {

    operator fun invoke(range: DateRange = DateRange.ALL): Flow<List<MonthlyTrend>> =
        dashboardRepository.getMonthlyTrend(range)
}
