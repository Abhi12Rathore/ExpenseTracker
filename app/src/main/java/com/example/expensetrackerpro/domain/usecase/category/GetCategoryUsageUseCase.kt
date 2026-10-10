package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Every category with how often it has been used, ever and in the month we are in. */
class GetCategoryUsageUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getDateRange: GetDateRangeUseCase
) {

    operator fun invoke(): Flow<List<CategoryUsage>> =
        categoryRepository.getCategoryUsage(getDateRange(getCurrentPeriod(PeriodGranularity.MONTH)))
}
