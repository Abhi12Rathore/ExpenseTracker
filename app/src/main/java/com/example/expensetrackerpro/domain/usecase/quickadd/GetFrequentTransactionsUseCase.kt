package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.FrequentTransaction
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import javax.inject.Inject

/**
 * The user's most frequent transactions of a type over the last [WINDOW_DAYS] days, today
 * included: a title and category that came up at least [MIN_COUNT] times, with its median
 * amount. The most frequent come first, and of two as frequent the one entered last; at most
 * [LIMIT].
 */
class GetFrequentTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(type: TransactionType): List<FrequentTransaction> {
        val repeated = repository.getRepeatedTransactions(type, window(), MIN_COUNT)

        return repeated
            .groupBy { it.title.trim().lowercase() to it.categoryId }
            .values
            .filter { it.size >= MIN_COUNT }
            .map { group -> group.sortedByDescending(Transaction::dateMillis) }
            .sortedWith(compareByDescending<List<Transaction>> { it.size }.thenByDescending { it.first().dateMillis })
            .take(LIMIT)
            .map(::frequent)
    }

    private fun frequent(group: List<Transaction>): FrequentTransaction {
        val latest = group.first()
        return FrequentTransaction(
            title = latest.title.trim(),
            categoryId = latest.categoryId,
            categoryName = latest.categoryName,
            categoryIcon = latest.categoryIcon,
            type = latest.transactionType,
            usualAmount = median(group.map(Transaction::amount)),
            count = group.size
        )
    }

    /** The middle amount, or halfway between the two middle ones to the nearest minor unit. */
    private fun median(amounts: List<Money>): Money {
        val sorted = amounts.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }

    /** From the start of the day [WINDOW_DAYS] - 1 days back to the end of today. */
    private fun window(): DateRange {
        val startOfToday = timeProvider.now().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = (startOfToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -(WINDOW_DAYS - 1)) }
        val end = (startOfToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        return DateRange(start.timeInMillis, end.timeInMillis)
    }

    companion object {
        const val WINDOW_DAYS = 60
        const val MIN_COUNT = 3
        const val LIMIT = 3
    }
}
