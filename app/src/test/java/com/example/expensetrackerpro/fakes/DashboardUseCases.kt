package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.repository.DashboardRepository
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import com.example.expensetrackerpro.domain.repository.UserProfileRepository
import com.example.expensetrackerpro.domain.time.TimeProvider
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateSavingsRateUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GenerateSmartInsightsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetBalanceTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCashFlowUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCategoryBreakdownUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDashboardUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDayPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetMonthlyTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetNetSavingsByMonthUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetPeriodComparisonUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetRecentTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetSpendingBucketsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetSpendingPaceUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.ObserveDayPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.ObserveTodayUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.RankCategorySpendingUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetComparableRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.profile.GetUserProfileUseCase

/** The real dashboard facade and every use case under it, reading from the given repositories. */
fun getDashboardUseCase(
    transactions: TransactionRepository,
    dashboard: DashboardRepository,
    timeProvider: TimeProvider,
    profile: UserProfileRepository = FakeUserProfileRepository()
): GetDashboardUseCase {
    val getDateRange = GetDateRangeUseCase(timeProvider)
    val getComparableRange =
        GetComparableRangeUseCase(timeProvider, getDateRange, StepPeriodUseCase(timeProvider))
    val getFinancialSummary = GetFinancialSummaryUseCase(
        transactions,
        CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())
    )
    val getMonthlyTrend = GetMonthlyTrendUseCase(dashboard)

    return GetDashboardUseCase(
        getFinancialSummary = getFinancialSummary,
        getPeriodComparison = GetPeriodComparisonUseCase(getFinancialSummary, getDateRange, getComparableRange),
        getBalanceTrend = GetBalanceTrendUseCase(GetCashFlowUseCase(dashboard), timeProvider),
        getRecentTransactions = GetRecentTransactionsUseCase(transactions),
        getCategoryBreakdown = GetCategoryBreakdownUseCase(dashboard),
        rankCategorySpending = RankCategorySpendingUseCase(),
        getSpendingBuckets = GetSpendingBucketsUseCase(dashboard, timeProvider),
        getSpendingPace = GetSpendingPaceUseCase(dashboard, getDateRange, getComparableRange, timeProvider),
        getNetSavingsByMonth = GetNetSavingsByMonthUseCase(getMonthlyTrend, getDateRange, timeProvider),
        getMonthlyTrend = getMonthlyTrend,
        generateSmartInsights = GenerateSmartInsightsUseCase(),
        observeDayPeriod = ObserveDayPeriodUseCase(timeProvider, GetDayPeriodUseCase()),
        observeToday = ObserveTodayUseCase(timeProvider),
        getUserProfile = GetUserProfileUseCase(profile),
        getDateRange = getDateRange
    )
}
