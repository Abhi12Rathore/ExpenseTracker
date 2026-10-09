package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PacePoint
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SpendingPace
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.filter.GetComparableRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Cumulative spending through a week or month next to the one before it. Emits null for a
 * year or a custom range, where a day-by-day pace says little.
 */
class GetSpendingPaceUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val getDateRange: GetDateRangeUseCase,
    private val getComparableRange: GetComparableRangeUseCase,
    private val timeProvider: TimeProvider
) {

    operator fun invoke(filter: DateFilter): Flow<SpendingPace?> {
        val period = (filter as? DateFilter.Period)
            ?.takeIf { it.granularity != PeriodGranularity.YEAR }
            ?: return flowOf(null)
        val comparable = getComparableRange(period) ?: return flowOf(null)
        val currentRange = getDateRange(period)

        return combine(
            dashboardRepository.getDailyExpenses(currentRange),
            dashboardRepository.getDailyExpenses(comparable.wholePeriod)
        ) { currentDays, previousDays ->
            val nowMillis = timeProvider.now().timeInMillis
            val currentDayRanges = timeProvider.now().dayRanges(currentRange)
            val previousDayRanges = timeProvider.now().dayRanges(comparable.wholePeriod)

            val current = cumulative(currentDayRanges.filter { it.startMillis <= nowMillis }, currentDays)
            val spentByTheSameDay = previousDays
                .filter { it.dateMillis in comparable.range }
                .sumOf { it.amount }

            SpendingPace(
                granularity = period.granularity,
                current = current,
                previous = cumulative(previousDayRanges, previousDays),
                dayCount = maxOf(currentDayRanges.size, previousDayRanges.size),
                difference = (current.lastOrNull()?.total ?: 0.0) - spentByTheSameDay,
                isPartial = comparable.isPartial
            )
        }
    }

    private fun cumulative(days: List<DateRange>, expenses: List<DailyExpense>): List<PacePoint> {
        var total = 0.0
        return days.map { day ->
            total += expenses.filter { it.dateMillis in day }.sumOf { it.amount }
            PacePoint(dateMillis = day.startMillis, total = total)
        }
    }
}
