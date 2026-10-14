package com.example.expensetrackerpro.data.sample

import com.example.expensetrackerpro.fakes.FakeDefaultCategoryNames
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.sumOfMoney
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.KOLKATA
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class SampleTransactionGeneratorTest {

    // Saturday 10 Oct 2026, mid-afternoon: the six months are May to October
    private val now = millisAt(2026, Calendar.OCTOBER, 10, hour = 15, minute = 30)
    private val timeProvider = FakeTimeProvider(now)
    private val generate = SampleTransactionGenerator(timeProvider)
    private val samples = generate()

    private val months = listOf(
        Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER
    )

    private fun SampleTransaction.calendar() =
        GregorianCalendar(KOLKATA, Locale.US).also { it.timeInMillis = dateMillis }

    private val SampleTransaction.month get() = calendar().get(Calendar.MONTH)
    private val SampleTransaction.day get() = calendar().get(Calendar.DAY_OF_MONTH)

    private fun inCategory(name: String) = samples.filter { it.categoryName == name }

    // Named in English here, so the expectations below read like the sample data
    private val SampleTransaction.categoryName get() = FakeDefaultCategoryNames.english.getValue(category)

    private fun SampleTransaction.signed() = if (type == TransactionType.INCOME) amount else -amount

    // --- Deterministic

    @Test
    fun sameDay_alwaysGivesTheSameTransactions() {
        assertEquals(samples, generate())
        assertEquals(samples, SampleTransactionGenerator(FakeTimeProvider(now))())
    }

    @Test
    fun laterInTheMonth_thePastIsUnchanged() {
        val fifteenDaysOn = SampleTransactionGenerator(
            FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 25, hour = 9))
        )()
        // Today's entries are left out: some had their time capped at the current moment
        val beforeToday = millisAt(2026, Calendar.OCTOBER, 10)

        assertEquals(
            samples.filter { it.dateMillis < beforeToday },
            fifteenDaysOn.filter { it.dateMillis < beforeToday }
        )
        assertTrue(fifteenDaysOn.size > samples.size)
    }

    // --- Dates

    @Test
    fun nothingIsInTheFuture_orOlderThanSixMonths_andTheListIsOldestFirst() {
        assertTrue(samples.all { it.dateMillis <= now })
        assertTrue(samples.all { it.dateMillis >= millisAt(2026, Calendar.MAY, 1) })
        assertEquals(samples.sortedBy { it.dateMillis }, samples)
        assertEquals(months.toSet(), samples.map { it.month }.toSet())
    }

    @Test
    fun earlyOnTheFirst_theMonthsSalaryIsAlreadyThere_andNotInTheFuture() {
        val earlyNow = millisAt(2026, Calendar.OCTOBER, 1, hour = 0, minute = 5)
        val early = SampleTransactionGenerator(FakeTimeProvider(earlyNow))()
        val octoberSalary = early.single { it.categoryName == "Salary" && it.month == Calendar.OCTOBER }

        assertEquals(earlyNow, octoberSalary.dateMillis)
        assertTrue(early.all { it.dateMillis <= earlyNow })
    }

    @Test
    fun timesOfDay_vary() {
        val hours = samples.map { it.calendar().get(Calendar.HOUR_OF_DAY) }.toSet()

        assertTrue(hours.size >= 10)
        assertTrue(hours.all { it in 7..22 })
    }

    // --- Categories

    @Test
    fun everyDefaultCategory_isUsed_withItsOwnType() {
        assertEquals(
            DefaultCategories.all.map { it to it.type }.toSet(),
            samples.map { it.category to it.type }.toSet()
        )
    }

    @Test
    fun everyDefaultCategory_isStillUsed_onTheFirstOfAMonth_andInOtherYears() {
        val expected = DefaultCategories.all.toSet()
        val days = listOf(
            millisAt(2026, Calendar.OCTOBER, 1, hour = 0, minute = 5),
            millisAt(2025, Calendar.NOVEMBER, 15, hour = 12),
            millisAt(2027, Calendar.MARCH, 31, hour = 23),
            millisAt(2028, Calendar.FEBRUARY, 29, hour = 8)
        )

        days.forEach { day ->
            val used = SampleTransactionGenerator(FakeTimeProvider(day))().map { it.category }.toSet()
            assertEquals(expected, used)
        }
    }

    // --- Income

    @Test
    fun salary_isCreditedOnTheFirstOfEveryMonth() {
        val salaries = inCategory("Salary")

        assertEquals(months, salaries.map { it.month })
        assertTrue(salaries.all { it.day == 1 && it.amount == rupees(85_000) && it.type == TransactionType.INCOME })
    }

    @Test
    fun freelance_comesOnceOrTwiceAMonth_withinItsRange() {
        // October is still under way, so only the finished months are counted
        val perMonth = inCategory("Freelance").filter { it.month != Calendar.OCTOBER }.groupBy { it.month }

        assertEquals(months.dropLast(1).toSet(), perMonth.keys)
        assertTrue(perMonth.values.all { it.size in 1..2 })
        assertTrue(inCategory("Freelance").all { it.amount in rupees(8_000)..rupees(25_000) })
    }

    @Test
    fun interest_isQuarterly() {
        val interest = inCategory("Interest")

        // June and September end a quarter inside the six months
        assertEquals(listOf(Calendar.JUNE, Calendar.SEPTEMBER), interest.map { it.month })
        assertTrue(interest.all { it.amount in rupees(1_200)..rupees(2_500) })
    }

    // --- Expenses

    @Test
    fun rentAndEmi_fallOnTheirDaysEveryFinishedMonth() {
        val rent = inCategory("Rent & Home")
        val emi = inCategory("EMI & Loans")

        assertEquals(months, rent.map { it.month })
        assertTrue(rent.all { it.day == 5 && it.amount == rupees(22_000) })
        assertEquals(months, emi.map { it.month })
        assertTrue(emi.all { it.day == 10 && it.amount == rupees(12_500) })
    }

    @Test
    fun amounts_stayWithinTheirRanges() {
        assertTrue(inCategory("Groceries").all { it.amount in rupees(1_200)..rupees(3_500) })
        assertTrue(inCategory("Food & Dining").all { it.amount in rupees(180)..rupees(2_200) })
        assertTrue(inCategory("Transport").all { it.amount in rupees(60)..rupees(450) })
        assertTrue(inCategory("Fuel").all { it.amount in rupees(1_500)..rupees(3_000) })
        assertTrue(inCategory("Shopping").all { it.amount in rupees(500)..rupees(6_500) })
        assertTrue(inCategory("Refunds & Cashback").all { it.amount in rupees(150)..rupees(1_800) })
        assertEquals(setOf(rupees(649), rupees(119)), inCategory("Subscriptions").map { it.amount }.toSet())
        // Whole rupees throughout
        assertTrue(samples.all { it.amount.isPositive && it.amount.minorUnits % Money.MINOR_PER_UNIT == 0L })
    }

    @Test
    fun finishedMonths_haveTheirUsualSpending() {
        months.dropLast(1).forEach { month ->
            val inMonth = samples.filter { it.month == month }
            fun count(category: String) = inMonth.count { it.categoryName == category }

            assertTrue(count("Groceries") in 4..5)
            assertTrue(count("Food & Dining") in 8..14)
            assertTrue(count("Transport") in 10..18)
            assertTrue(count("Fuel") in 2..3)
            assertTrue(count("Shopping") in 2..4)
            assertEquals(3, count("Bills & Utilities"))
        }
    }

    @Test
    fun thereIsOneTrip_ofThreeOrFourEntries_withinItsBudget() {
        val trip = inCategory("Travel")

        assertTrue(trip.size in 3..4)
        assertEquals(1, trip.map { it.month }.toSet().size)
        assertTrue(trip.sumOfMoney { it.amount } in rupees(18_000)..rupees(35_000))
    }

    @Test
    fun atLeastOneMonth_spendsMoreThanItEarns_whileMostDoNot() {
        val netByMonth = samples.groupBy { it.month }.mapValues { (_, list) -> list.sumOfMoney { it.signed() } }

        assertTrue(netByMonth.values.any { it.isNegative })
        assertTrue(netByMonth.values.count { it.isPositive } >= 4)
    }

    @Test
    fun theOverspentMonth_isThere_whateverDayTheDataIsMadeOn() {
        val days = listOf(
            millisAt(2026, Calendar.OCTOBER, 1, hour = 0, minute = 5),
            millisAt(2025, Calendar.NOVEMBER, 15, hour = 12),
            millisAt(2027, Calendar.MARCH, 31, hour = 23),
            millisAt(2028, Calendar.FEBRUARY, 29, hour = 8)
        )

        days.forEach { day ->
            val netByMonth = SampleTransactionGenerator(FakeTimeProvider(day))()
                .groupBy { it.calendar().get(Calendar.YEAR) to it.month }
                .mapValues { (_, list) -> list.sumOfMoney { it.signed() } }

            assertTrue(netByMonth.values.any { it.isNegative })
        }
    }

    // --- Festival

    @Test
    fun whenDiwaliIsOutsideTheSixMonths_theGiftsFallInTheThirdMonth_underNeutralNames() {
        // Diwali 2026 is on 8 November, after today
        val received = inCategory("Gifts Received").single()
        val given = inCategory("Gifts & Donations")

        assertEquals(rupees(5_000), received.amount)
        assertEquals(Calendar.JULY, received.month)
        assertTrue(given.size in 2..3)
        assertTrue(given.all { it.month == Calendar.JULY })
        assertFalse((given + received).any { it.title.contains("Diwali") })
    }

    @Test
    fun whenDiwaliIsInsideTheSixMonths_theGiftsFallAroundIt() {
        // Diwali 2025 was on 20 October
        val november = SampleTransactionGenerator(
            FakeTimeProvider(millisAt(2025, Calendar.NOVEMBER, 15, hour = 12))
        )()
        val received = november.single { it.categoryName == "Gifts Received" }
        val given = november.filter { it.categoryName == "Gifts & Donations" }

        assertEquals(Calendar.OCTOBER, received.month)
        assertEquals(20, received.day)
        assertTrue(received.title.contains("Diwali"))
        assertTrue(given.all { it.month == Calendar.OCTOBER && it.day in 18..20 && it.title.contains("Diwali") })
    }

    // --- Text

    @Test
    fun aboutAQuarter_haveNotes_andEveryTitleIsFilledIn() {
        val withNotes = samples.count { it.note.isNotEmpty() }.toDouble() / samples.size

        assertTrue("$withNotes", withNotes in 0.18..0.32)
        assertTrue(samples.all { it.title.isNotBlank() })
        assertTrue(samples.map { it.title }.toSet().size > 40)
    }
}
