package com.example.expensetrackerpro.presentation.common.preview

import com.example.expensetrackerpro.domain.models.BucketSize
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.Dashboard
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Money
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
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.sum
import com.example.expensetrackerpro.domain.usecase.dashboard.RankCategorySpendingUseCase

/** Sample data for @Preview composables only. */
internal object PreviewData {

    // 9 Oct 2025, fixed so previews render the same every time
    const val DATE_MILLIS = 1_759_968_000_000L
    private const val DAY_MILLIS = 86_400_000L

    private fun rupees(amount: Long) = Money.ofRupees(amount)

    val categories = listOf(
        Category(id = CategoryId(1), name = "Food", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT, isCustom = false),
        Category(id = CategoryId(2), name = "Transport", type = TransactionType.EXPENSE, icon = CategoryIcon.DIRECTIONS_CAR, isCustom = false),
        Category(id = CategoryId(3), name = "Shopping", type = TransactionType.EXPENSE, icon = CategoryIcon.SHOPPING_BAG, isCustom = false),
        Category(id = CategoryId(4), name = "Bills", type = TransactionType.EXPENSE, icon = CategoryIcon.RECEIPT_LONG, isCustom = false),
        Category(id = CategoryId(5), name = "Pets", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
    )

    // This month's use of each, the last two not used at all
    val categoryUsages = listOf(
        CategoryUsage(categories[0], totalCount = 48, periodCount = 14, periodAmount = rupees(7450)),
        CategoryUsage(categories[3], totalCount = 21, periodCount = 4, periodAmount = rupees(9800)),
        CategoryUsage(categories[1], totalCount = 30, periodCount = 9, periodAmount = rupees(2100)),
        CategoryUsage(categories[2], totalCount = 6),
        CategoryUsage(categories[4])
    )

    val transactions = listOf(
        Transaction(TransactionId(1), "Groceries", rupees(1850), "Weekly shop, plus the cake for Sunday", DATE_MILLIS, CategoryId(1), "Food", TransactionType.EXPENSE, CategoryIcon.RESTAURANT),
        Transaction(TransactionId(2), "Salary", rupees(85000), "", DATE_MILLIS - DAY_MILLIS, CategoryId(6), "Payments", TransactionType.INCOME, CategoryIcon.PAYMENTS),
        Transaction(TransactionId(3), "Metro card", rupees(500), "", DATE_MILLIS - 2 * DAY_MILLIS, CategoryId(2), "Transport", TransactionType.EXPENSE, CategoryIcon.DIRECTIONS_CAR),
        Transaction(TransactionId(4), "Electricity", rupees(2300), "", DATE_MILLIS - 3 * DAY_MILLIS, CategoryId(4), "Bills", TransactionType.EXPENSE, CategoryIcon.RECEIPT_LONG)
    )

    // The same transactions as the Transactions list groups them, seen on the 9th
    val transactionDays = listOf(
        TransactionDayGroup(
            dayStartMillis = DATE_MILLIS,
            label = DayLabel.TODAY,
            net = rupees(-1850),
            transactions = listOf(transactions[0])
        ),
        TransactionDayGroup(
            dayStartMillis = DATE_MILLIS - DAY_MILLIS,
            label = DayLabel.YESTERDAY,
            net = rupees(85000),
            transactions = listOf(transactions[1])
        ),
        TransactionDayGroup(
            dayStartMillis = DATE_MILLIS - 3 * DAY_MILLIS,
            label = DayLabel.OTHER,
            net = rupees(-2800),
            // Shown as one day so the preview has a card with more than one row
            transactions = listOf(transactions[2], transactions[3])
        )
    )

    val financialSummary = FinancialSummary(
        totalBalance = rupees(80350),
        totalIncome = rupees(85000),
        totalExpense = rupees(4650),
        savingsRate = 94f
    )

    // Highest first, as the repository returns them
    val categoryExpense = listOf(
        CategoryBreakdown(CategoryId(4), "Bills", CategoryIcon.RECEIPT_LONG, rupees(9800)),
        CategoryBreakdown(CategoryId(1), "Food", CategoryIcon.RESTAURANT, rupees(7450)),
        CategoryBreakdown(CategoryId(3), "Shopping", CategoryIcon.SHOPPING_BAG, rupees(4900)),
        CategoryBreakdown(CategoryId(2), "Transport", CategoryIcon.DIRECTIONS_CAR, rupees(2600)),
        CategoryBreakdown(CategoryId(6), "Health", CategoryIcon.MEDICAL_SERVICES, rupees(1500)),
        CategoryBreakdown(CategoryId(5), "Pets", CategoryIcon.PETS, rupees(800)),
        CategoryBreakdown(CategoryId(7), "Gifts", CategoryIcon.CARD_GIFTCARD, rupees(500))
    )

    val categoryRanking = RankCategorySpendingUseCase()(categoryExpense)

    // October 2025 as seen on the 9th: nine days of spending, the rest still to come
    private const val MONTH_START_MILLIS = DATE_MILLIS - 8 * DAY_MILLIS
    private val dailyAmounts = listOf<Long>(1200, 0, 2340, 860, 0, 3150, 450, 1980, 1850).map(::rupees)

    val dailySpending = SpendingChart(
        bucketSize = BucketSize.DAY,
        buckets = List(31) { day ->
            val start = MONTH_START_MILLIS + day * DAY_MILLIS
            val amount = dailyAmounts.getOrElse(day) { Money.ZERO }
            SpendingBucket(
                range = DateRange(start, start + DAY_MILLIS),
                amount = amount,
                transactionCount = if (amount.isPositive) 3 else 0,
                isFuture = day >= dailyAmounts.size
            )
        },
        average = dailyAmounts.sum() / dailyAmounts.size
    )

    // 2025 as seen in October. Thirty-day months are close enough for a preview
    private const val YEAR_START_MILLIS = 1_735_689_600_000L
    private val monthlyAmounts =
        listOf<Long>(31200, 28400, 35900, 30100, 42600, 38800, 33500, 41000, 38500, 11830).map(::rupees)

    val monthlySpending = SpendingChart(
        bucketSize = BucketSize.MONTH,
        buckets = List(12) { month ->
            val start = YEAR_START_MILLIS + month * 30 * DAY_MILLIS
            val amount = monthlyAmounts.getOrElse(month) { Money.ZERO }
            SpendingBucket(
                range = DateRange(start, start + 30 * DAY_MILLIS),
                amount = amount,
                transactionCount = if (amount.isPositive) 24 else 0,
                isFuture = month >= monthlyAmounts.size
            )
        },
        average = monthlyAmounts.sum() / monthlyAmounts.size
    )

    // Salary on the 1st, then spending draws the balance down
    val balanceTrend = listOf<Long>(83800, 83800, 81460, 80600, 80600, 77450, 77000, 75020, 80350).map(::rupees)

    val periodComparison = PeriodComparison(
        granularity = PeriodGranularity.MONTH,
        previousRange = DateRange(MONTH_START_MILLIS - 30 * DAY_MILLIS, MONTH_START_MILLIS),
        balanceDifference = rupees(3200),
        isPartial = true
    )

    // Nine days into October against the whole of September
    private val septemberTotals = List(30) { day -> rupees(1150) * (day + 1) + if (day >= 1) rupees(4000) else Money.ZERO }

    val ongoingPace = SpendingPace(
        granularity = PeriodGranularity.MONTH,
        current = dailyAmounts.runningFold(Money.ZERO) { total, amount -> total + amount }.drop(1)
            .mapIndexed { day, total -> PacePoint(MONTH_START_MILLIS + day * DAY_MILLIS, total) },
        previous = septemberTotals
            .mapIndexed { day, total -> PacePoint(MONTH_START_MILLIS + (day - 30) * DAY_MILLIS, total) },
        dayCount = 31,
        difference = rupees(-2540),
        isPartial = true
    )

    // September seen once it is over, against August
    val finishedPace = SpendingPace(
        granularity = PeriodGranularity.MONTH,
        current = septemberTotals
            .mapIndexed { day, total -> PacePoint(MONTH_START_MILLIS + (day - 30) * DAY_MILLIS, total) },
        previous = List(31) { day -> PacePoint(MONTH_START_MILLIS + (day - 61) * DAY_MILLIS, rupees(1320) * (day + 1)) },
        dayCount = 31,
        difference = rupees(-2420),
        isPartial = false
    )

    // 2025 as seen in October: two overspent months, two still to come
    private val monthlyNets =
        listOf<Long>(18400, 22100, -6200, 15800, 9300, -14750, 20500, 12900, 17600, 3200).map(::rupees)

    val netSavings = List(12) { month ->
        val start = YEAR_START_MILLIS + month * 30 * DAY_MILLIS
        MonthlyNet(
            range = DateRange(start, start + 30 * DAY_MILLIS),
            net = monthlyNets.getOrElse(month) { Money.ZERO },
            isFuture = month >= monthlyNets.size
        )
    }

    val insights = listOf(
        SmartInsight.Saved(rupees(80350)),
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
