package com.example.expensetrackerpro.domain.models

/** Everything the Home dashboard shows for one selected period. */
data class Dashboard(
    // Time of day for the greeting; null until it has been read
    val dayPeriod: DayPeriod? = null,

    // Who the greeting is for; blank until a name has been set
    val userName: String = "",

    val summary: FinancialSummary = FinancialSummary(),

    // Null for a custom range, which has no previous period to compare with
    val comparison: PeriodComparison? = null,

    // Running balance at the end of each day so far, oldest first
    val balanceTrend: List<Money> = emptyList(),

    val spending: SpendingChart = SpendingChart(),

    val categoryRanking: CategoryRanking = CategoryRanking(),

    // Only for a week or a month
    val pace: SpendingPace? = null,

    // One entry per month of a year; empty for anything shorter
    val netSavings: List<MonthlyNet> = emptyList(),

    val recentTransactions: List<Transaction> = emptyList(),

    val insights: List<SmartInsight> = emptyList()
)
