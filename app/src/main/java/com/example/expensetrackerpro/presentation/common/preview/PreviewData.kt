package com.example.expensetrackerpro.presentation.common.preview

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.Dashboard
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyNet
import com.example.expensetrackerpro.domain.models.PacePoint
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.SpendingBucket
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.domain.models.SpendingPace
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionDayGroup
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.dashboard.RankCategorySpendingUseCase

/** Sample data for @Preview composables only. */
internal object PreviewData {

    // 9 Oct 2025, fixed so previews render the same every time
    const val DATE_MILLIS = 1_759_968_000_000L
    private const val DAY_MILLIS = 86_400_000L

    val categories = listOf(
        Category(id = 1, name = "Food", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT, isCustom = false),
        Category(id = 2, name = "Transport", type = TransactionType.EXPENSE, icon = CategoryIcon.DIRECTIONS_CAR, isCustom = false),
        Category(id = 3, name = "Shopping", type = TransactionType.EXPENSE, icon = CategoryIcon.SHOPPING_BAG, isCustom = false),
        Category(id = 4, name = "Bills", type = TransactionType.EXPENSE, icon = CategoryIcon.RECEIPT_LONG, isCustom = false),
        Category(id = 5, name = "Pets", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
    )

    // This month's use of each, the last two not used at all
    val categoryUsages = listOf(
        CategoryUsage(categories[0], totalCount = 48, periodCount = 14, periodAmount = 7450.0),
        CategoryUsage(categories[3], totalCount = 21, periodCount = 4, periodAmount = 9800.0),
        CategoryUsage(categories[1], totalCount = 30, periodCount = 9, periodAmount = 2100.0),
        CategoryUsage(categories[2], totalCount = 6),
        CategoryUsage(categories[4])
    )

    val transactions = listOf(
        Transaction(1, "Groceries", 1850.0, "Weekly shop, plus the cake for Sunday", DATE_MILLIS, 1, "Food", TransactionType.EXPENSE, CategoryIcon.RESTAURANT),
        Transaction(2, "Salary", 85000.0, "", DATE_MILLIS - DAY_MILLIS, 6, "Payments", TransactionType.INCOME, CategoryIcon.PAYMENTS),
        Transaction(3, "Metro card", 500.0, "", DATE_MILLIS - 2 * DAY_MILLIS, 2, "Transport", TransactionType.EXPENSE, CategoryIcon.DIRECTIONS_CAR),
        Transaction(4, "Electricity", 2300.0, "", DATE_MILLIS - 3 * DAY_MILLIS, 4, "Bills", TransactionType.EXPENSE, CategoryIcon.RECEIPT_LONG)
    )

    // The same transactions as the Transactions list groups them, seen on the 9th
    val transactionDays = listOf(
        TransactionDayGroup(
            dayStartMillis = DATE_MILLIS,
            label = DayLabel.TODAY,
            net = -1850.0,
            transactions = listOf(transactions[0])
        ),
        TransactionDayGroup(
            dayStartMillis = DATE_MILLIS - DAY_MILLIS,
            label = DayLabel.YESTERDAY,
            net = 85000.0,
            transactions = listOf(transactions[1])
        ),
        TransactionDayGroup(
            dayStartMillis = DATE_MILLIS - 3 * DAY_MILLIS,
            label = DayLabel.OTHER,
            net = -2800.0,
            // Shown as one day so the preview has a card with more than one row
            transactions = listOf(transactions[2], transactions[3])
        )
    )

    val financialSummary = FinancialSummary(
        totalBalance = 80350.0,
        totalIncome = 85000.0,
        totalExpense = 4650.0,
        savingsRate = 94f
    )

    // Highest first, as the repository returns them
    val categoryExpense = listOf(
        CategoryBreakdown(4, "Bills", CategoryIcon.RECEIPT_LONG, 9800.0),
        CategoryBreakdown(1, "Food", CategoryIcon.RESTAURANT, 7450.0),
        CategoryBreakdown(3, "Shopping", CategoryIcon.SHOPPING_BAG, 4900.0),
        CategoryBreakdown(2, "Transport", CategoryIcon.DIRECTIONS_CAR, 2600.0),
        CategoryBreakdown(6, "Health", CategoryIcon.MEDICAL_SERVICES, 1500.0),
        CategoryBreakdown(5, "Pets", CategoryIcon.PETS, 800.0),
        CategoryBreakdown(7, "Gifts", CategoryIcon.CARD_GIFTCARD, 500.0)
    )

    val categoryRanking = RankCategorySpendingUseCase()(categoryExpense)

    // October 2025 as seen on the 9th: nine days of spending, the rest still to come
    private const val MONTH_START_MILLIS = DATE_MILLIS - 8 * DAY_MILLIS
    private val dailyAmounts = listOf(1200.0, 0.0, 2340.0, 860.0, 0.0, 3150.0, 450.0, 1980.0, 1850.0)

    val dailySpending = SpendingChart(
        bucketSize = BucketSize.DAY,
        buckets = List(31) { day ->
            val start = MONTH_START_MILLIS + day * DAY_MILLIS
            val amount = dailyAmounts.getOrElse(day) { 0.0 }
            SpendingBucket(
                range = DateRange(start, start + DAY_MILLIS),
                amount = amount,
                transactionCount = if (amount > 0) 3 else 0,
                isFuture = day >= dailyAmounts.size
            )
        },
        average = dailyAmounts.average()
    )

    // 2025 as seen in October. Thirty-day months are close enough for a preview
    private const val YEAR_START_MILLIS = 1_735_689_600_000L
    private val monthlyAmounts =
        listOf(31200.0, 28400.0, 35900.0, 30100.0, 42600.0, 38800.0, 33500.0, 41000.0, 38500.0, 11830.0)

    val monthlySpending = SpendingChart(
        bucketSize = BucketSize.MONTH,
        buckets = List(12) { month ->
            val start = YEAR_START_MILLIS + month * 30 * DAY_MILLIS
            val amount = monthlyAmounts.getOrElse(month) { 0.0 }
            SpendingBucket(
                range = DateRange(start, start + 30 * DAY_MILLIS),
                amount = amount,
                transactionCount = if (amount > 0) 24 else 0,
                isFuture = month >= monthlyAmounts.size
            )
        },
        average = monthlyAmounts.average()
    )

    // Salary on the 1st, then spending draws the balance down
    val balanceTrend = listOf(83800.0, 83800.0, 81460.0, 80600.0, 80600.0, 77450.0, 77000.0, 75020.0, 80350.0)

    val periodComparison = PeriodComparison(
        granularity = PeriodGranularity.MONTH,
        previousRange = DateRange(MONTH_START_MILLIS - 30 * DAY_MILLIS, MONTH_START_MILLIS),
        balanceDifference = 3200.0,
        isPartial = true
    )

    // Nine days into October against the whole of September
    private val septemberTotals = List(30) { day -> 1150.0 * (day + 1) + if (day >= 1) 4000.0 else 0.0 }

    val ongoingPace = SpendingPace(
        granularity = PeriodGranularity.MONTH,
        current = dailyAmounts.runningFold(0.0) { total, amount -> total + amount }.drop(1)
            .mapIndexed { day, total -> PacePoint(MONTH_START_MILLIS + day * DAY_MILLIS, total) },
        previous = septemberTotals
            .mapIndexed { day, total -> PacePoint(MONTH_START_MILLIS + (day - 30) * DAY_MILLIS, total) },
        dayCount = 31,
        difference = -2540.0,
        isPartial = true
    )

    // September seen once it is over, against August
    val finishedPace = SpendingPace(
        granularity = PeriodGranularity.MONTH,
        current = septemberTotals
            .mapIndexed { day, total -> PacePoint(MONTH_START_MILLIS + (day - 30) * DAY_MILLIS, total) },
        previous = List(31) { day -> PacePoint(MONTH_START_MILLIS + (day - 61) * DAY_MILLIS, 1320.0 * (day + 1)) },
        dayCount = 31,
        difference = -2420.0,
        isPartial = false
    )

    // 2025 as seen in October: two overspent months, two still to come
    private val monthlyNets =
        listOf(18400.0, 22100.0, -6200.0, 15800.0, 9300.0, -14750.0, 20500.0, 12900.0, 17600.0, 3200.0)

    val netSavings = List(12) { month ->
        val start = YEAR_START_MILLIS + month * 30 * DAY_MILLIS
        MonthlyNet(
            range = DateRange(start, start + 30 * DAY_MILLIS),
            net = monthlyNets.getOrElse(month) { 0.0 },
            isFuture = month >= monthlyNets.size
        )
    }

    val insights = listOf(
        SmartInsight.Saved(80350.0),
        SmartInsight.HighestSpending("Bills")
    )

    val dashboard = Dashboard(
        dayPeriod = DayPeriod.EVENING,
        summary = financialSummary,
        comparison = periodComparison,
        balanceTrend = balanceTrend,
        spending = dailySpending,
        categoryRanking = categoryRanking,
        pace = ongoingPace,
        recentTransactions = transactions,
        insights = insights
    )
}
