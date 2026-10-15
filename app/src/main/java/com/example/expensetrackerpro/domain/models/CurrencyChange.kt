package com.example.expensetrackerpro.domain.models

import com.example.expensetrackerpro.domain.currency.AppCurrency

/** A currency change that has just been applied and can still be undone. */
data class CurrencyChange(
    val previous: AppCurrency,
    val current: AppCurrency
)

/** What the Currency screen lists. */
data class CurrencyOptions(
    val current: AppCurrency,
    /** The current currency, then the phone region's when it differs. */
    val suggested: List<AppCurrency>,
    /** Every other currency, by name in the app's language. */
    val others: List<AppCurrency>
)

/** The user's own figures for this month, to show them in another currency before switching. */
data class CurrencyPreview(
    val summary: FinancialSummary,
    /** This month's biggest expense category, or null when there is none. */
    val topCategory: CategoryBreakdown?
)

/** The user's latest transaction, as the example the change of currency is explained with. */
data class CurrencyChangeExample(
    val transactionCount: Int,
    val latestAmount: Money
)
