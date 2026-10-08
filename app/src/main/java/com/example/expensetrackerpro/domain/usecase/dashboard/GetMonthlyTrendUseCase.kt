package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMonthlyTrendUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {

    operator fun invoke(): Flow<List<MonthlyTrend>> = dashboardRepository.getMonthlyTrend()
}
