package com.example.expensetrackerpro.domain.models

/** Which transaction types the Transactions list is narrowed to. */
enum class TransactionTypeFilter { ALL, EXPENSE, INCOME }

/** How a day is named in its header, relative to the current day. */
enum class DayLabel { TODAY, YESTERDAY, OTHER }

/** One local day's transactions, newest first, with what they add up to. */
data class TransactionDayGroup(
    val dayStartMillis: Long,
    val label: DayLabel,
    // Income minus expense of the transactions listed
    val net: Money,
    val transactions: List<Transaction>
)

/** The three figures shown above the list. Which three depends on what is listed. */
sealed interface TransactionListSummary {

    /** Every type is listed. */
    data class Overview(
        val income: Money = Money.ZERO,
        val expense: Money = Money.ZERO,
        val net: Money = Money.ZERO
    ) : TransactionListSummary

    /** Only [type] is listed. [biggest] is the largest single amount, 0 when there is none. */
    data class SingleType(
        val type: TransactionType,
        val total: Money,
        val count: Int,
        val biggest: Money
    ) : TransactionListSummary

    /**
     * Search results. [total] is income minus expense of the results and [average] is that
     * total per result, so both are negative when the results cost more than they brought in.
     * [average] is 0 when there are no results.
     */
    data class Search(
        val count: Int,
        val total: Money,
        val average: Money
    ) : TransactionListSummary
}

/** How many transactions each type chip would list. */
data class TransactionTypeCounts(
    val all: Int = 0,
    val expense: Int = 0,
    val income: Int = 0
)

/** Everything the Transactions list shows for one combination of filters. */
data class TransactionListing(
    val counts: TransactionTypeCounts = TransactionTypeCounts(),
    val summary: TransactionListSummary = TransactionListSummary.Overview(),
    val days: List<TransactionDayGroup> = emptyList()
)
