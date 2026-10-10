package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionListSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionTypeCounts
import com.example.expensetrackerpro.domain.models.TransactionTypeFilter
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TransactionListingUseCasesTest {

    // Friday 9 Oct 2026, mid-afternoon
    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 9, hour = 15))

    private val filterTransactions = FilterTransactionsUseCase()
    private val groupByDay = GroupTransactionsByDayUseCase(timeProvider)
    private val summarize = SummarizeTransactionsUseCase()
    private val getListing = GetTransactionListingUseCase(filterTransactions, groupByDay, summarize)

    private val today = millisAt(2026, Calendar.OCTOBER, 9)
    private val yesterday = millisAt(2026, Calendar.OCTOBER, 8)
    private val monday = millisAt(2026, Calendar.OCTOBER, 5)

    private val coffee = transaction(id = 1, amount = rupees(120), dateMillis = millisAt(2026, Calendar.OCTOBER, 9, hour = 9))
    private val salary = transaction(
        id = 2,
        amount = rupees(5000),
        type = TransactionType.INCOME,
        dateMillis = millisAt(2026, Calendar.OCTOBER, 9, hour = 8)
    )
    private val dinner = transaction(id = 3, amount = rupees(800), dateMillis = millisAt(2026, Calendar.OCTOBER, 8, hour = 21))
    private val taxi = transaction(id = 4, amount = rupees(300), dateMillis = millisAt(2026, Calendar.OCTOBER, 5, hour = 7))
        .copy(categoryId = CategoryId(2), categoryName = "Transport")
    private val refund = transaction(
        id = 5,
        amount = rupees(200),
        type = TransactionType.INCOME,
        dateMillis = millisAt(2026, Calendar.OCTOBER, 5, hour = 6)
    ).copy(categoryId = CategoryId(2), categoryName = "Transport")

    // Newest first, as the repository returns them
    private val all = listOf(coffee, salary, dinner, taxi, refund)

    // --- Grouping

    @Test
    fun groups_areOnePerDay_newestFirst_withTodayAndYesterdayNamed() {
        val days = groupByDay(all)

        assertEquals(listOf(today, yesterday, monday), days.map { it.dayStartMillis })
        assertEquals(listOf(DayLabel.TODAY, DayLabel.YESTERDAY, DayLabel.OTHER), days.map { it.label })
        assertEquals(
            listOf(listOf(coffee, salary), listOf(dinner), listOf(taxi, refund)),
            days.map { it.transactions }
        )
    }

    @Test
    fun dayNet_isIncomeMinusExpenseOfThatDay() {
        val nets = groupByDay(all).map { it.net }

        assertEquals(listOf(rupees(4880), rupees(-800), rupees(-100)), nets)
    }

    @Test
    fun aMinuteEitherSideOfMidnight_isTwoDays() {
        val lastMinute = transaction(id = 6, dateMillis = millisAt(2026, Calendar.OCTOBER, 8, hour = 23, minute = 59))
        val firstMinute = transaction(id = 7, dateMillis = millisAt(2026, Calendar.OCTOBER, 9, hour = 0, minute = 1))

        val days = groupByDay(listOf(firstMinute, lastMinute))

        assertEquals(listOf(today, yesterday), days.map { it.dayStartMillis })
        assertEquals(listOf(listOf(firstMinute), listOf(lastMinute)), days.map { it.transactions })
    }

    @Test
    fun exactlyMidnight_belongsToTheDayItStarts() {
        val midnight = transaction(id = 6, dateMillis = today)

        assertEquals(DayLabel.TODAY, groupByDay(listOf(midnight)).single().label)
    }

    @Test
    fun dayBoundaries_followTheProvidersZone_notUtc() {
        // 01:00 on the 9th in Kolkata is still the 8th in UTC
        val earlyMorning = transaction(id = 6, dateMillis = millisAt(2026, Calendar.OCTOBER, 9, hour = 1))
        val utc = TimeZone.getTimeZone("UTC")
        val utcProvider = FakeTimeProvider(timeProvider.nowMillis, timeZone = utc)

        assertEquals(DayLabel.TODAY, groupByDay(listOf(earlyMorning)).single().label)
        assertEquals(
            DayLabel.YESTERDAY,
            GroupTransactionsByDayUseCase(utcProvider)(listOf(earlyMorning)).single().label
        )
    }

    @Test
    fun labels_moveOnWhenTheClockDoes() {
        timeProvider.nowMillis = millisAt(2026, Calendar.OCTOBER, 10, hour = 0, minute = 1)

        assertEquals(
            listOf(DayLabel.YESTERDAY, DayLabel.OTHER, DayLabel.OTHER),
            groupByDay(all).map { it.label }
        )
    }

    @Test
    fun aFutureDay_isNotToday() {
        val tomorrow = transaction(id = 6, dateMillis = millisAt(2026, Calendar.OCTOBER, 10, hour = 9))

        assertEquals(DayLabel.OTHER, groupByDay(listOf(tomorrow)).single().label)
    }

    @Test
    fun unsortedInput_isStillNewestDayFirst() {
        val days = groupByDay(listOf(taxi, coffee, dinner))

        assertEquals(listOf(today, yesterday, monday), days.map { it.dayStartMillis })
    }

    @Test
    fun noTransactions_isNoGroups() {
        assertTrue(groupByDay(emptyList()).isEmpty())
    }

    // --- Filtering

    @Test
    fun filter_byType() {
        assertEquals(all, filterTransactions(all, TransactionTypeFilter.ALL))
        assertEquals(listOf(coffee, dinner, taxi), filterTransactions(all, TransactionTypeFilter.EXPENSE))
        assertEquals(listOf(salary, refund), filterTransactions(all, TransactionTypeFilter.INCOME))
    }

    @Test
    fun filter_byCategory_aloneAndWithType() {
        assertEquals(listOf(taxi, refund), filterTransactions(all, TransactionTypeFilter.ALL, categoryId = CategoryId(2)))
        assertEquals(listOf(refund), filterTransactions(all, TransactionTypeFilter.INCOME, categoryId = CategoryId(2)))
        assertTrue(filterTransactions(all, TransactionTypeFilter.ALL, categoryId = CategoryId(99)).isEmpty())
    }

    // --- Summary

    @Test
    fun summary_ofAllTypes_isIncomeExpenseAndNet() {
        assertEquals(
            TransactionListSummary.Overview(income = rupees(5200), expense = rupees(1220), net = rupees(3980)),
            summarize(all, TransactionTypeFilter.ALL, isSearching = false)
        )
    }

    @Test
    fun summary_ofOneType_isTotalEntriesAndBiggest() {
        assertEquals(
            TransactionListSummary.SingleType(TransactionType.EXPENSE, total = rupees(1220), count = 3, biggest = rupees(800)),
            summarize(listOf(coffee, dinner, taxi), TransactionTypeFilter.EXPENSE, isSearching = false)
        )
        assertEquals(
            TransactionListSummary.SingleType(TransactionType.INCOME, total = rupees(5200), count = 2, biggest = rupees(5000)),
            summarize(listOf(salary, refund), TransactionTypeFilter.INCOME, isSearching = false)
        )
    }

    @Test
    fun summary_whileSearching_isResultsNetTotalAndThatTotalPerResult() {
        // Searching wins over the type filter
        assertEquals(
            TransactionListSummary.Search(count = 1, total = rupees(-300), average = rupees(-300)),
            summarize(listOf(taxi), TransactionTypeFilter.EXPENSE, isSearching = true)
        )
        assertEquals(
            TransactionListSummary.Search(count = 1, total = rupees(200), average = rupees(200)),
            summarize(listOf(refund), TransactionTypeFilter.INCOME, isSearching = true)
        )
    }

    @Test
    fun summary_whileSearching_averagesIncomeAndExpenseWithTheirSigns() {
        // 200 in, 300 out: a net of -100, which is -50 per result
        assertEquals(
            TransactionListSummary.Search(count = 2, total = rupees(-100), average = rupees(-50)),
            summarize(listOf(taxi, refund), TransactionTypeFilter.ALL, isSearching = true)
        )
        // 5000 and 200 in, 120 and 800 out: the average always times the count back to the total
        val mixed = summarize(listOf(coffee, salary, dinner, refund), TransactionTypeFilter.ALL, isSearching = true)
        assertEquals(TransactionListSummary.Search(count = 4, total = rupees(4280), average = rupees(1070)), mixed)
    }

    @Test
    fun summary_ofNothing_isAllZeros() {
        assertEquals(
            TransactionListSummary.Overview(),
            summarize(emptyList(), TransactionTypeFilter.ALL, isSearching = false)
        )
        assertEquals(
            TransactionListSummary.SingleType(TransactionType.INCOME, total = Money.ZERO, count = 0, biggest = Money.ZERO),
            summarize(emptyList(), TransactionTypeFilter.INCOME, isSearching = false)
        )
        assertEquals(
            TransactionListSummary.Search(count = 0, total = Money.ZERO, average = Money.ZERO),
            summarize(emptyList(), TransactionTypeFilter.ALL, isSearching = true)
        )
    }

    // --- Listing

    @Test
    fun listing_countsEveryType_whileListingOnlyTheSelectedOne() {
        val listing = getListing(all, TransactionTypeFilter.INCOME, categoryId = null, isSearching = false)

        assertEquals(TransactionTypeCounts(all = 5, expense = 3, income = 2), listing.counts)
        assertEquals(listOf(listOf(salary), listOf(refund)), listing.days.map { it.transactions })
        // Day totals cover only what is listed
        assertEquals(listOf(rupees(5000), rupees(200)), listing.days.map { it.net })
        assertEquals(
            TransactionListSummary.SingleType(TransactionType.INCOME, total = rupees(5200), count = 2, biggest = rupees(5000)),
            listing.summary
        )
    }

    @Test
    fun listing_countsOnlyTheCategory() {
        val listing = getListing(all, TransactionTypeFilter.ALL, categoryId = CategoryId(2), isSearching = false)

        assertEquals(TransactionTypeCounts(all = 2, expense = 1, income = 1), listing.counts)
        assertEquals(listOf(listOf(taxi, refund)), listing.days.map { it.transactions })
        assertEquals(
            TransactionListSummary.Overview(income = rupees(200), expense = rupees(300), net = rupees(-100)),
            listing.summary
        )
    }

    @Test
    fun listing_ofNothing_isEmpty() {
        val listing = getListing(emptyList(), TransactionTypeFilter.EXPENSE, categoryId = null, isSearching = false)

        assertEquals(TransactionTypeCounts(), listing.counts)
        assertTrue(listing.days.isEmpty())
    }
}
