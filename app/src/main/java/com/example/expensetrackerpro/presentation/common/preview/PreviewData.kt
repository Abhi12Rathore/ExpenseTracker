package com.example.expensetrackerpro.presentation.common.preview

import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType

/** Sample data for @Preview composables only. */
internal object PreviewData {

    // 9 Oct 2025, fixed so previews render the same every time
    const val DATE_MILLIS = 1_759_968_000_000L
    private const val DAY_MILLIS = 86_400_000L

    val categories = listOf(
        Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "Food", isCustom = false),
        Category(id = 2, name = "Transport", type = TransactionType.EXPENSE, iconName = "Transport", isCustom = false),
        Category(id = 3, name = "Shopping", type = TransactionType.EXPENSE, iconName = "Shopping", isCustom = false),
        Category(id = 4, name = "Bills", type = TransactionType.EXPENSE, iconName = "Bills", isCustom = false),
        Category(id = 5, name = "Pets", type = TransactionType.EXPENSE, iconName = "Pets")
    )

    val transactions = listOf(
        Transaction(1, "Groceries", 1850.0, "Groceries", DATE_MILLIS, 1, "Food", TransactionType.EXPENSE),
        Transaction(2, "Salary", 85000.0, "Salary", DATE_MILLIS - DAY_MILLIS, 6, "Payments", TransactionType.INCOME),
        Transaction(3, "Metro card", 500.0, "Metro card", DATE_MILLIS - 2 * DAY_MILLIS, 2, "Transport", TransactionType.EXPENSE),
        Transaction(4, "Electricity", 2300.0, "Electricity", DATE_MILLIS - 3 * DAY_MILLIS, 4, "Bills", TransactionType.EXPENSE)
    )

    val financialSummary = FinancialSummary(
        totalBalance = 80350.0,
        totalIncome = 85000.0,
        totalExpense = 4650.0,
        savingsRate = 94f
    )

    val categoryExpense = listOf(
        CategoryBreakdown(1, "Food", 1850.0),
        CategoryBreakdown(2, "Transport", 500.0),
        CategoryBreakdown(4, "Bills", 2300.0)
    )

    val dailyCashFlow = listOf(
        CashFlowPoint(DATE_MILLIS - 3 * DAY_MILLIS, 0.0, 2300.0),
        CashFlowPoint(DATE_MILLIS - 2 * DAY_MILLIS, 0.0, 500.0),
        CashFlowPoint(DATE_MILLIS - DAY_MILLIS, 85000.0, 0.0),
        CashFlowPoint(DATE_MILLIS, 0.0, 1850.0)
    )

    val monthlyTrend = listOf(
        MonthlyTrend(2025, 8, 82000.0, 41000.0),
        MonthlyTrend(2025, 9, 85000.0, 38500.0),
        MonthlyTrend(2025, 10, 85000.0, 4650.0)
    )

    val insights = listOf(
        SmartInsight.Saved(80350.0),
        SmartInsight.HighestSpending("Bills")
    )
}
