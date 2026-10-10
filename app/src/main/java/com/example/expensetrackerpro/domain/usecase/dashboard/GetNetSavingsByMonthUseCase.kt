package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyNet
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * Income minus expense for every month of a year, January first, including the months with no
 * transactions. Emits an empty list for anything other than a year.
 */
class GetNetSavingsByMonthUseCase @Inject constructor(
    private val getMonthlyTrend: GetMonthlyTrendUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val timeProvider: TimeProvider
) {

    operator fun invoke(filter: DateFilter): Flow<List<MonthlyNet>> {
        val year = (filter as? DateFilter.Period)
            ?.takeIf { it.granularity == PeriodGranularity.YEAR }
            ?: return flowOf(emptyList())
        val range = getDateRange(year)

        return getMonthlyTrend(range).map { trend ->
            val nowMillis = timeProvider.now().timeInMillis
            val cursor = timeProvider.now().apply { timeInMillis = range.startMillis }

            buildList {
                while (cursor.timeInMillis < range.endMillis) {
                    val startMillis = cursor.timeInMillis
                    val calendarYear = cursor.get(Calendar.YEAR)
                    // The trend counts months from 1, the calendar from 0
                    val monthOfYear = cursor.get(Calendar.MONTH) + 1
                    cursor.add(Calendar.MONTH, 1)

                    val month = trend.find { it.year == calendarYear && it.month == monthOfYear }
                    add(
                        MonthlyNet(
                            range = DateRange(startMillis, minOf(cursor.timeInMillis, range.endMillis)),
                            net = if (month == null) Money.ZERO else month.income - month.expense,
                            isFuture = startMillis > nowMillis
                        )
                    )
                }
            }
        }
    }
}
