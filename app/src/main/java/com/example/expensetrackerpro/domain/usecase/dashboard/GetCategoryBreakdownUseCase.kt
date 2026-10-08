package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoryBreakdownUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {

    operator fun invoke(range: DateRange = DateRange.ALL): Flow<List<CategoryBreakdown>> =
        dashboardRepository.getCategoryBreakdown(range)
}
