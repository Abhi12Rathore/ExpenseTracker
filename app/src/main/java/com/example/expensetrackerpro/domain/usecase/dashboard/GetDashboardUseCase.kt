package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.Dashboard
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyNet
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.domain.models.SpendingPace
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.profile.GetUserProfileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

/** Gathers every section of the Home dashboard for one filter, so the screen has one source. */
class GetDashboardUseCase @Inject constructor(
    private val getFinancialSummary: GetFinancialSummaryUseCase,
    private val getPeriodComparison: GetPeriodComparisonUseCase,
    private val getBalanceTrend: GetBalanceTrendUseCase,
    private val getRecentTransactions: GetRecentTransactionsUseCase,
    private val getCategoryBreakdown: GetCategoryBreakdownUseCase,
    private val rankCategorySpending: RankCategorySpendingUseCase,
    private val getSpendingBuckets: GetSpendingBucketsUseCase,
    private val getSpendingPace: GetSpendingPaceUseCase,
    private val getNetSavingsByMonth: GetNetSavingsByMonthUseCase,
    private val getMonthlyTrend: GetMonthlyTrendUseCase,
    private val generateSmartInsights: GenerateSmartInsightsUseCase,
    private val getDayPeriod: GetDayPeriodUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val timeProvider: TimeProvider
) {

    operator fun invoke(filter: DateFilter): Flow<Dashboard> {
        val range = getDateRange(filter)
        val dayPeriod = getDayPeriod(timeProvider.now().get(Calendar.HOUR_OF_DAY))

        val totals = combine(
            getFinancialSummary(range),
            getPeriodComparison(filter),
            getBalanceTrend(range),
            getRecentTransactions(range),
            getUserProfile(),
            ::Totals
        )
        val charts = combine(
            getCategoryBreakdown(range),
            getSpendingBuckets(range),
            getSpendingPace(filter),
            getNetSavingsByMonth(filter),
            getMonthlyTrend(range),
            ::Charts
        )

        return combine(totals, charts) { totals, charts ->
            Dashboard(
                dayPeriod = dayPeriod,
                userName = totals.profile.name,
                summary = totals.summary,
                comparison = totals.comparison,
                balanceTrend = totals.balanceTrend,
                spending = charts.spending,
                categoryRanking = rankCategorySpending(charts.categories),
                pace = charts.pace,
                netSavings = charts.netSavings,
                recentTransactions = totals.recentTransactions,
                insights = generateSmartInsights(totals.summary, charts.categories, charts.monthlyTrend)
            )
        }
    }

    // Two groups, since a single combine takes at most five flows
    private data class Totals(
        val summary: FinancialSummary,
        val comparison: PeriodComparison?,
        val balanceTrend: List<Double>,
        val recentTransactions: List<Transaction>,
        val profile: UserProfile
    )

    private data class Charts(
        val categories: List<CategoryBreakdown>,
        val spending: SpendingChart,
        val pace: SpendingPace?,
        val netSavings: List<MonthlyNet>,
        val monthlyTrend: List<MonthlyTrend>
    )
}
