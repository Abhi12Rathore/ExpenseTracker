package com.example.expensetrackerpro.data.sample

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.sumOfMoney
import com.example.expensetrackerpro.domain.time.TimeProvider
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random

/**
 * Six months of believable Indian household finances, ending today: a salary, rent and an EMI
 * on their days, weekly groceries, a trip, a festival. The same day always produces the same
 * list, so screenshots and bug reports can be compared.
 */
class SampleTransactionGenerator @Inject constructor(
    private val timeProvider: TimeProvider
) {

    /** Oldest first. Nothing is dated after the current moment. */
    operator fun invoke(): List<SampleTransaction> {
        val random = Random(SEED)
        val now = timeProvider.now()
        val nowMillis = now.timeInMillis
        val tomorrowStart = timeProvider.now().apply {
            moveToStartOfDay()
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val firstMonth = timeProvider.now().apply {
            moveToStartOfDay()
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -(MONTH_COUNT - 1))
        }

        val months = List(MONTH_COUNT) { index ->
            val monthStart = (firstMonth.clone() as Calendar).apply { add(Calendar.MONTH, index) }
            MonthBuilder(random, monthStart).also { it.addMonth(index) }
        }
        addFestival(months, firstMonth.timeInMillis, nowMillis, random)
        // After the festival, so the month still ends in the red whatever the festival brought in
        months[OVERSPEND_MONTH].addCourseFeeThatOverspends()

        return months.flatMap { it.entries }
            // Every month is generated whole, so the past reads the same on any day of the month
            .filter { it.dateMillis < tomorrowStart }
            // Today's entries can't be later than the current time
            .map { it.copy(dateMillis = minOf(it.dateMillis, nowMillis)) }
            .sortedBy { it.dateMillis }
    }

    private fun MonthBuilder.addMonth(index: Int) {
        addIncome(index)
        addFixedExpenses()
        addEverydayExpenses()
        addOccasionalExpenses(index)
        if (index == OVERSPEND_MONTH) addTrip()
    }

    private fun MonthBuilder.addIncome(index: Int) {
        income(1, "Salary credit", 85_000, SALARY, INCOME_NOTES)
        income(7, "Parking space rent", 2_500, RENTAL_INCOME, INCOME_NOTES)

        repeat(random.nextInt(1, 3)) {
            income(random.nextInt(8, 27), FREELANCE_TITLES.pick(), amount(8_000, 25_000, 500), FREELANCE, INCOME_NOTES)
        }
        if (isQuarterEnd) {
            income(daysInMonth, "Savings account interest", amount(1_200, 2_500, 1), INTEREST, INCOME_NOTES)
        }
        // The first month always has one, so the category is never missing
        repeat(maxOf(random.nextInt(0, 3), if (index == 0) 1 else 0)) {
            income(randomDay(), REFUND_TITLES.pick(), amount(150, 1_800), REFUNDS, INCOME_NOTES)
        }
        if (index % 2 == 1) {
            income(random.nextInt(10, 25), INVESTMENT_TITLES.pick(), amount(900, 3_200), INVESTMENTS, INCOME_NOTES)
        }
        if (index % 3 == 0) {
            income(random.nextInt(12, 28), BUSINESS_TITLES.pick(), amount(2_000, 9_000, 100), BUSINESS, INCOME_NOTES)
        }
        if (index == 1) {
            income(random.nextInt(5, 25), "Sold old cycle on OLX", 3_500, OTHER_INCOME, INCOME_NOTES)
        }
    }

    private fun MonthBuilder.addFixedExpenses() {
        expense(5, "House rent", 22_000, RENT, FIXED_NOTES)
        expense(10, "Car loan EMI", 12_500, EMI, FIXED_NOTES)
        expense(random.nextInt(12, 19), "Electricity bill", amount(1_800, 4_200), BILLS, ELECTRICITY_NOTES)
        expense(8, "Airtel postpaid", 799, BILLS, FIXED_NOTES)
        expense(15, "ACT Fibernet broadband", 1_179, BILLS, FIXED_NOTES)
        expense(3, "Netflix", 649, SUBSCRIPTIONS, FIXED_NOTES)
        expense(20, "Spotify Premium", 119, SUBSCRIPTIONS, FIXED_NOTES)
    }

    private fun MonthBuilder.addEverydayExpenses() {
        // Weekly, from a random day in the first week
        var groceryDay = random.nextInt(1, 8)
        while (groceryDay <= daysInMonth) {
            expense(groceryDay, GROCERY_TITLES.pick(), amount(1_200, 3_500), GROCERIES, SPENDING_NOTES)
            groceryDay += 7
        }
        repeat(random.nextInt(8, 15)) {
            expense(randomDay(), DINING_TITLES.pick(), amount(180, 2_200), FOOD, SPENDING_NOTES)
        }
        repeat(random.nextInt(10, 19)) {
            expense(randomDay(), TRANSPORT_TITLES.pick(), amount(60, 450), TRANSPORT, SPENDING_NOTES)
        }
        repeat(random.nextInt(2, 4)) {
            expense(randomDay(), FUEL_TITLES.pick(), amount(1_500, 3_000, 50), FUEL, FIXED_NOTES)
        }
        repeat(random.nextInt(2, 5)) {
            expense(randomDay(), SHOPPING_TITLES.pick(), amount(500, 6_500, 50), SHOPPING, SPENDING_NOTES)
        }
    }

    // Each of these is certain in at least one month that is already over, so no category is
    // left unused whatever day the data is generated on
    private fun MonthBuilder.addOccasionalExpenses(index: Int) {
        repeat(maxOf(random.nextInt(0, 2), if (index == 0) 1 else 0)) {
            expense(randomDay(), HEALTH_TITLES.pick(), amount(250, 2_800), HEALTH, SPENDING_NOTES)
        }
        repeat(maxOf(random.nextInt(0, 3), if (index == 1) 1 else 0)) {
            expense(randomDay(), ENTERTAINMENT_TITLES.pick(), amount(300, 2_400), ENTERTAINMENT, SPENDING_NOTES)
        }
        repeat(maxOf(random.nextInt(0, 3), if (index == 0) 1 else 0)) {
            expense(randomDay(), PERSONAL_CARE_TITLES.pick(), amount(250, 1_800), PERSONAL_CARE, SPENDING_NOTES)
        }
        if (index == 4) {
            expense(random.nextInt(4, 24), "Udemy course", amount(450, 3_500, 50), EDUCATION, FIXED_NOTES)
        }
        if (index % 2 == 0) {
            expense(randomDay(), OTHER_TITLES.pick(), amount(100, 1_500), OTHER, SPENDING_NOTES)
        }
    }

    /** One trip of three or four entries over consecutive days, ₹18,000 to ₹35,000 in all. */
    private fun MonthBuilder.addTrip() {
        val total = amount(18_000, 35_000, 500)
        val legs = if (random.nextBoolean()) TRIP_IN_FOUR else TRIP_IN_THREE
        val firstDay = random.nextInt(14, 21)
        var spent = 0

        legs.forEachIndexed { index, (title, percent) ->
            // The last leg takes what is left, so the parts add up to the total exactly
            val cost = if (index == legs.lastIndex) total - spent else shareOf(total, percent)
            spent += cost
            expense(firstDay + index, title, cost, TRAVEL, SPENDING_NOTES)
        }
    }

    /**
     * A course fee sized to leave this month a few thousand rupees short, so the charts have a
     * month of negative net savings to show.
     */
    private fun MonthBuilder.addCourseFeeThatOverspends() {
        val income = entries.filter { it.type == TransactionType.INCOME }.sumOfMoney { it.amount }
        val expense = entries.filter { it.type == TransactionType.EXPENSE }.sumOfMoney { it.amount }
        // Every sample amount is a whole number of rupees
        val surplus = ((income - expense).paise / Money.PAISE_PER_RUPEE).toInt()
        val shortfall = amount(4_000, 9_000, 500)
        val fee = maxOf(15_000, roundUpTo(surplus + shortfall, 500))

        expense(random.nextInt(2, 9), "Data analytics course fee", fee, EDUCATION, FIXED_NOTES)
    }

    /**
     * Gifts in and out around Diwali when it falls inside the six months, otherwise around a
     * day in the third month under names that fit any festival.
     */
    private fun addFestival(months: List<MonthBuilder>, windowStart: Long, nowMillis: Long, random: Random) {
        val diwali = DIWALI_DATES
            .map { (year, month, day) -> timeProvider.now().apply { clear(); set(year, month, day) } }
            .firstOrNull { it.timeInMillis in windowStart..nowMillis }
        val month = months.firstOrNull { diwali != null && it.isMonthOf(diwali) } ?: months[FALLBACK_FESTIVAL_MONTH]
        val day = diwali?.get(Calendar.DAY_OF_MONTH) ?: FALLBACK_FESTIVAL_DAY
        val titles = if (diwali != null) DIWALI_TITLES else FESTIVAL_TITLES

        with(month) {
            income(day, titles.received, 5_000, GIFTS_RECEIVED, INCOME_NOTES)
            // The shopping happens in the days before
            expense(day - 2, titles.gifts, amount(2_500, 6_000, 50), GIFTS, SPENDING_NOTES)
            expense(day - 1, titles.sweets, amount(800, 2_000), GIFTS, SPENDING_NOTES)
            if (random.nextBoolean()) {
                expense(day, titles.donation, listOf(501, 1_100, 2_100).pick(), GIFTS, FIXED_NOTES)
            }
        }
    }

    private class FestivalTitles(val received: String, val gifts: String, val sweets: String, val donation: String)

    /** Collects one calendar month's entries, drawing times and notes from the shared [random]. */
    private class MonthBuilder(val random: Random, monthStart: Calendar) {

        private val calendar = monthStart.clone() as Calendar
        private val year = calendar.get(Calendar.YEAR)
        private val month = calendar.get(Calendar.MONTH)

        val daysInMonth: Int = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isQuarterEnd: Boolean = month % 3 == 2
        val entries = mutableListOf<SampleTransaction>()

        fun isMonthOf(other: Calendar): Boolean =
            other.get(Calendar.YEAR) == year && other.get(Calendar.MONTH) == month

        fun randomDay(): Int = random.nextInt(1, daysInMonth + 1)

        /** A whole number of rupees from [min] to [max], on a multiple of [step]. */
        fun amount(min: Int, max: Int, step: Int = 10): Int =
            random.nextInt(min / step, max / step + 1) * step

        fun <T> List<T>.pick(): T = this[random.nextInt(size)]

        fun income(day: Int, title: String, rupees: Int, category: BuiltInCategory, notes: List<String>) =
            add(day, title, rupees, category, TransactionType.INCOME, notes)

        fun expense(day: Int, title: String, rupees: Int, category: BuiltInCategory, notes: List<String>) =
            add(day, title, rupees, category, TransactionType.EXPENSE, notes)

        // Always draws the same four numbers, so one entry's note never shifts the next entry
        private fun add(
            day: Int,
            title: String,
            rupees: Int,
            category: BuiltInCategory,
            type: TransactionType,
            notes: List<String>
        ) {
            calendar.set(
                year,
                month,
                day.coerceIn(1, daysInMonth),
                random.nextInt(FIRST_HOUR, LAST_HOUR + 1),
                random.nextInt(60),
                0
            )
            val hasNote = random.nextFloat() < NOTE_CHANCE
            val note = notes.pick()

            entries += SampleTransaction(
                title = title,
                amount = Money.ofRupees(rupees.toLong()),
                note = if (hasNote) note else "",
                dateMillis = calendar.timeInMillis,
                category = category,
                type = type
            )
        }
    }

    private fun Calendar.moveToStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        // Resolve the fields now so later add() calls start from midnight
        timeInMillis
    }

    /** [percent] of [total], to the nearest ₹10. */
    private fun shareOf(total: Int, percent: Int): Int = (total * percent + 500) / 1000 * 10

    /** The smallest multiple of [step] at or above [value]. */
    private fun roundUpTo(value: Int, step: Int): Int = -Math.floorDiv(-value, step) * step

    private companion object {
        const val SEED = 20_261_010
        const val MONTH_COUNT = 6
        // The third month: over by the time the data is generated, with months either side
        const val OVERSPEND_MONTH = 2
        const val FALLBACK_FESTIVAL_MONTH = 2
        const val FALLBACK_FESTIVAL_DAY = 14
        const val NOTE_CHANCE = 0.25f
        const val FIRST_HOUR = 7
        const val LAST_HOUR = 22

        // The built-in categories, under the short names used above
        val FOOD = BuiltInCategory.FOOD_DINING
        val GROCERIES = BuiltInCategory.GROCERIES
        val TRANSPORT = BuiltInCategory.TRANSPORT
        val FUEL = BuiltInCategory.FUEL
        val SHOPPING = BuiltInCategory.SHOPPING
        val BILLS = BuiltInCategory.BILLS_UTILITIES
        val RENT = BuiltInCategory.RENT_HOME
        val HEALTH = BuiltInCategory.HEALTH
        val EDUCATION = BuiltInCategory.EDUCATION
        val ENTERTAINMENT = BuiltInCategory.ENTERTAINMENT
        val TRAVEL = BuiltInCategory.TRAVEL
        val SUBSCRIPTIONS = BuiltInCategory.SUBSCRIPTIONS
        val PERSONAL_CARE = BuiltInCategory.PERSONAL_CARE
        val GIFTS = BuiltInCategory.GIFTS_DONATIONS
        val EMI = BuiltInCategory.EMI_LOANS
        val OTHER = BuiltInCategory.OTHER
        val SALARY = BuiltInCategory.SALARY
        val FREELANCE = BuiltInCategory.FREELANCE
        val BUSINESS = BuiltInCategory.BUSINESS
        val INVESTMENTS = BuiltInCategory.INVESTMENTS
        val INTEREST = BuiltInCategory.INTEREST
        val RENTAL_INCOME = BuiltInCategory.RENTAL_INCOME
        val REFUNDS = BuiltInCategory.REFUNDS_CASHBACK
        val GIFTS_RECEIVED = BuiltInCategory.GIFTS_RECEIVED
        val OTHER_INCOME = BuiltInCategory.OTHER_INCOME

        // Year, Calendar month, day
        val DIWALI_DATES = listOf(
            Triple(2024, Calendar.NOVEMBER, 1),
            Triple(2025, Calendar.OCTOBER, 20),
            Triple(2026, Calendar.NOVEMBER, 8),
            Triple(2027, Calendar.OCTOBER, 29),
            Triple(2028, Calendar.OCTOBER, 17),
            Triple(2029, Calendar.NOVEMBER, 5),
            Triple(2030, Calendar.OCTOBER, 26)
        )

        val DIWALI_TITLES = FestivalTitles(
            received = "Diwali gift from parents",
            gifts = "Diwali gifts for family",
            sweets = "Diwali sweets — Haldiram's",
            donation = "Diwali donation — temple"
        )
        val FESTIVAL_TITLES = FestivalTitles(
            received = "Festival gift from family",
            gifts = "Gifts for family",
            sweets = "Sweets for relatives — Haldiram's",
            donation = "Donation — Akshaya Patra"
        )

        // Title, then its share of the trip's cost in percent
        val TRIP_IN_THREE = listOf(
            "Flight tickets to Goa — IndiGo" to 48,
            "Hotel in Goa — 3 nights" to 38,
            "Scooter rental and sightseeing" to 14
        )
        val TRIP_IN_FOUR = listOf(
            "Flight tickets to Goa — IndiGo" to 44,
            "Hotel in Goa — 3 nights" to 34,
            "Scooter rental and sightseeing" to 12,
            "Airport cab and meals" to 10
        )

        val FREELANCE_TITLES = listOf(
            "Logo design — client payment",
            "Website maintenance retainer",
            "App UI review — freelance",
            "Content writing project"
        )
        val REFUND_TITLES = listOf("Amazon refund", "Swiggy refund", "Credit card cashback", "Paytm cashback")
        val INVESTMENT_TITLES = listOf("Mutual fund dividend", "Stock dividend — ITC")
        val BUSINESS_TITLES = listOf("Weekend stall sales", "Reselling — old camera gear")
        val GROCERY_TITLES =
            listOf("BigBasket", "Blinkit", "DMart", "Zepto", "Reliance Fresh", "Local kirana store")
        val DINING_TITLES = listOf(
            "Swiggy order",
            "Zomato order",
            "Dinner at Barbeque Nation",
            "Chai and samosa",
            "Lunch at office canteen",
            "Cafe Coffee Day",
            "Domino's pizza",
            "Biryani — Paradise",
            "Starbucks",
            "Dosa at Saravana Bhavan"
        )
        val TRANSPORT_TITLES = listOf(
            "Uber to office",
            "Ola cab",
            "Metro card recharge",
            "Rapido bike",
            "Auto rickshaw",
            "Uber from airport"
        )
        val FUEL_TITLES = listOf("HP petrol pump", "Indian Oil", "Bharat Petroleum", "Shell fuel")
        val SHOPPING_TITLES = listOf(
            "Amazon order",
            "Flipkart order",
            "Myntra — clothes",
            "Decathlon",
            "Croma — headphones",
            "Westside",
            "IKEA"
        )
        val HEALTH_TITLES = listOf(
            "Apollo Pharmacy",
            "Doctor consultation",
            "1mg medicines",
            "Dental check-up",
            "Lab tests — Thyrocare"
        )
        val ENTERTAINMENT_TITLES =
            listOf("PVR movie tickets", "BookMyShow — comedy show", "Bowling with friends", "Concert tickets")
        val PERSONAL_CARE_TITLES = listOf("Salon haircut", "Urban Company — spa", "Nykaa order")
        val OTHER_TITLES = listOf("Cash expenses", "Courier charges", "Key duplication", "Miscellaneous")

        val SPENDING_NOTES = listOf(
            "Split with Rahul",
            "Paid via UPI",
            "Paid in cash",
            "With family",
            "Office reimbursement pending",
            "Paid by credit card"
        )
        val FIXED_NOTES = listOf("Paid via UPI", "Auto-debit", "Paid by credit card")
        val ELECTRICITY_NOTES = listOf("Includes ₹350 late fee", "Paid via UPI", "Higher because of the AC")
        val INCOME_NOTES = listOf("Credited to HDFC account", "Received via UPI", "After TDS")
    }
}
