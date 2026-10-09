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
    val net: Double,
    val transactions: List<Transaction>
)

/** The three figures shown above the list. Which three depends on what is listed. */
sealed interface TransactionListSummary {

    /** Every type is listed. */
    data class Overview(
        val income: Double = 0.0,
        val expense: Double = 0.0,
        val net: Double = 0.0
    ) : TransactionListSummary

    /** Only [type] is listed. [biggest] is the largest single amount, 0 when there is none. */
    data class SingleType(
        val type: TransactionType,
        val total: Double,
        val count: Int,
        val biggest: Double
    ) : TransactionListSummary

    /**
     * Search results. [total] is income minus expense of the results; [average] is the mean
     * size of one result whatever its type, 0 when there is none.
     */
    data class Search(
        val count: Int,
        val total: Double,
        val average: Double
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
