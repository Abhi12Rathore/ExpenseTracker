package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.FieldConfidence.GUESS
import com.example.expensetrackerpro.domain.models.FieldConfidence.MISSING
import com.example.expensetrackerpro.domain.models.FieldConfidence.SURE
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.TransactionType.EXPENSE
import com.example.expensetrackerpro.domain.models.TransactionType.INCOME
import com.example.expensetrackerpro.fakes.FakeCurrencyProvider
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.utcDay
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Calendar

/** One sentence each: what quick add should fill in, and how sure it should say it is. */
@RunWith(Parameterized::class)
class ParseQuickAddUseCaseTest(private val case: Case) {

    data class Case(
        val text: String,
        val amount: Money? = null,
        val amountConfidence: FieldConfidence = if (amount != null) SURE else MISSING,
        val title: String? = null,
        val category: CategoryId? = null,
        val categoryConfidence: FieldConfidence = if (category != null) GUESS else MISSING,
        val date: Long? = null,
        val note: String? = null,
        val type: TransactionType? = null,
        val currentType: TransactionType = EXPENSE,
        val learned: List<CategoryMemory> = emptyList()
    ) {
        override fun toString(): String = "\"$text\""
    }

    @Test
    fun parsesTheSentence() {
        val result = parse(case.text, case.currentType, case.learned)
        val draft = result.draft

        assertEquals("amount", case.amount, draft.amount)
        assertEquals("amount confidence", case.amountConfidence, result.confidenceOf(QuickAddField.AMOUNT))
        assertEquals("title", case.title, draft.title)
        assertEquals("title confidence", if (case.title != null) SURE else MISSING, result.confidenceOf(QuickAddField.TITLE))
        assertEquals("category", case.category, draft.categoryId)
        assertEquals("category confidence", case.categoryConfidence, result.confidenceOf(QuickAddField.CATEGORY))
        assertEquals("date", case.date, draft.dateMillis)
        assertEquals("date confidence", case.date?.let { SURE }, result.confidenceOf(QuickAddField.DATE))
        assertEquals("note", case.note, draft.note)
        assertEquals("type", case.type, draft.type)
        assertEquals("type confidence", case.type?.let { SURE }, result.confidenceOf(QuickAddField.TYPE))
    }

    companion object {
        // Thursday 8 Oct 2026, 12:00 in Kolkata
        private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

        private val TODAY = utcDay(2026, Calendar.OCTOBER, 8)
        private val YESTERDAY = utcDay(2026, Calendar.OCTOBER, 7)
        private val TWO_DAYS_AGO = utcDay(2026, Calendar.OCTOBER, 6)

        private val food = builtIn(1, "Food & Dining", BuiltInCategory.FOOD_DINING)
        private val groceries = builtIn(2, "Groceries", BuiltInCategory.GROCERIES)
        private val transport = builtIn(3, "Transport", BuiltInCategory.TRANSPORT)
        private val fuel = builtIn(4, "Fuel", BuiltInCategory.FUEL)
        private val shopping = builtIn(5, "Shopping", BuiltInCategory.SHOPPING)
        private val bills = builtIn(6, "Bills & Utilities", BuiltInCategory.BILLS_UTILITIES)
        private val health = builtIn(7, "Health", BuiltInCategory.HEALTH)
        private val entertainment = builtIn(8, "Entertainment", BuiltInCategory.ENTERTAINMENT)
        private val salary = builtIn(20, "Salary", BuiltInCategory.SALARY)
        private val freelance = builtIn(21, "Freelance", BuiltInCategory.FREELANCE)
        private val refunds = builtIn(22, "Refunds & Cashback", BuiltInCategory.REFUNDS_CASHBACK)
        private val interest = builtIn(23, "Interest", BuiltInCategory.INTEREST)
        private val gifts = builtIn(24, "Gifts Received", BuiltInCategory.GIFTS_RECEIVED)
        private val pets = category(id = 30, name = "Pets")

        private val categories = listOf(
            food, groceries, transport, fuel, shopping, bills, health, entertainment,
            salary, freelance, refunds, interest, gifts, pets
        )

        private fun builtIn(id: Int, name: String, key: BuiltInCategory) =
            category(id = id, name = name, type = key.type).copy(builtInKey = key)

        private val parseQuickAdd = ParseQuickAddUseCase(
            RuleBasedQuickAddParser(),
            FakeTimeProvider(now),
            FakeCurrencyProvider()
        )

        fun parse(text: String, currentType: TransactionType = EXPENSE, learned: List<CategoryMemory> = emptyList()) =
            parseQuickAdd(text, currentType, categories, learned)

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Case> = listOf(
            // The examples under the bar
            Case("450 swiggy yesterday", rupees(450), title = "Swiggy", category = food.id, date = YESTERDAY),
            Case("salary 1,05,000", rupees(105_000), title = "Salary", category = salary.id, type = INCOME),
            Case("kal 200 ka petrol", rupees(200), title = "Petrol", category = fuel.id, date = YESTERDAY),

            // English
            Case(
                "Spent 450 on swiggy yesterday, dinner with Rahul", rupees(450), title = "Swiggy",
                category = food.id, date = YESTERDAY, note = "dinner with Rahul", type = EXPENSE
            ),
            Case("swiggy 450 for dinner with Rahul", rupees(450), title = "Swiggy", category = food.id, note = "dinner with Rahul"),
            Case("450 Swiggy\ndinner", rupees(450), title = "Swiggy", category = food.id, note = "dinner"),
            Case("paid 500 for petrol", rupees(500), title = "Petrol", category = fuel.id, type = EXPENSE),
            Case("petrol 500 for bike", rupees(500), title = "Petrol", category = fuel.id, note = "bike"),
            Case("KFC 650", rupees(650), title = "KFC", category = food.id),
            Case("movie 300 at 9pm", rupees(300), title = "Movie", category = entertainment.id),
            Case("lunch 250 at 1:30", rupees(250), title = "Lunch", category = food.id),
            Case("electricity bill 2300 parso", rupees(2300), title = "Electricity", category = bills.id, date = TWO_DAYS_AGO),

            // Currency words and symbols
            Case("₹450 zomato", rupees(450), title = "Zomato", category = food.id),
            Case("rs 1200 uber", rupees(1200), title = "Uber", category = transport.id),
            Case("rs.1200 uber", rupees(1200), title = "Uber", category = transport.id),
            Case("1200 rs ola", rupees(1200), title = "Ola", category = transport.id),
            Case("450/- swiggy", rupees(450), title = "Swiggy", category = food.id),
            Case("twenty five rupees chai", rupees(25), title = "Chai", category = food.id),

            // Lakh, crore, k and words
            Case("4.5k amazon", rupees(4500), title = "Amazon", category = shopping.id),
            Case("1.05L salary", rupees(105_000), title = "Salary", category = salary.id, type = INCOME),
            Case("1.05 lakh bonus", rupees(105_000), title = "Bonus", category = salary.id, type = INCOME),
            Case(
                "got my salary one lakh five thousand today", rupees(105_000), title = "Salary",
                category = salary.id, date = TODAY, type = INCOME
            ),
            Case("do sau ka petrol", rupees(200), title = "Petrol", category = fuel.id),
            Case("paanch hazar groceries", rupees(5000), title = "Groceries", category = groceries.id),
            Case("interest credited 1,245.50", Money(1_245_50), title = "Interest", category = interest.id, type = INCOME),

            // Over the limit, or nothing at all: missing
            Case("2 crore flat", title = "Flat"),
            Case("Spent 15,00,000 on car", title = "Car", type = EXPENSE),
            Case("0 swiggy", title = "Swiggy", category = food.id),
            Case("chai", title = "Chai", category = food.id),
            Case("dmart shopping yesterday", title = "Dmart", category = groceries.id, date = YESTERDAY),

            // Several numbers: the one next to a currency word, else the largest, as a guess
            Case("coffee 120 and sandwich 250", rupees(250), GUESS, title = "Coffee Sandwich", category = food.id),
            Case("2 pizzas 600", rupees(600), GUESS, title = "Pizzas", category = food.id),
            Case("2 pizzas rs 600 table 12", rupees(600), title = "Pizzas Table", category = food.id),
            Case("one plate momos 120", rupees(120), title = "Plate Momos"),

            // Dates
            Case("aaj 300 auto", rupees(300), title = "Auto", category = transport.id, date = TODAY),
            Case("day before yesterday 700 metro", rupees(700), title = "Metro", category = transport.id, date = TWO_DAYS_AGO),
            Case("groceries 1500 last thursday", rupees(1500), title = "Groceries", category = groceries.id,
                date = utcDay(2026, Calendar.OCTOBER, 1)),
            Case("netflix 649 last monday", rupees(649), title = "Netflix", date = utcDay(2026, Calendar.OCTOBER, 5)),
            Case("medicines 340 on 3/10", rupees(340), title = "Medicines", category = health.id,
                date = utcDay(2026, Calendar.OCTOBER, 3)),
            Case("rent 18000 1/10/2026", rupees(18_000), title = "Rent", date = utcDay(2026, Calendar.OCTOBER, 1)),
            Case("zomato 380 5 oct", rupees(380), title = "Zomato", category = food.id, date = utcDay(2026, Calendar.OCTOBER, 5)),
            Case("dinner 800 sept 28th", rupees(800), title = "Dinner", category = food.id,
                date = utcDay(2026, Calendar.SEPTEMBER, 28)),
            Case("uber 250 12 dec", rupees(250), title = "Uber", category = transport.id,
                date = utcDay(2025, Calendar.DECEMBER, 12)),

            // A date in the future, or over a year back, is dropped
            Case("zomato 380 12 oct", rupees(380), title = "Zomato", category = food.id),
            Case("uber 250 5/11/2026", rupees(250), title = "Uber", category = transport.id),
            Case("uber 250 5 oct 2024", rupees(250), title = "Uber", category = transport.id),

            // Income
            Case("freelance payment 25000", rupees(25_000), title = "Freelance Payment", category = freelance.id, type = INCOME),
            Case("received 3000 from client", rupees(3000), title = "Client", category = freelance.id, type = INCOME),
            Case("refund 499 from amazon", rupees(499), title = "Refund Amazon", category = refunds.id, type = INCOME),
            Case("cashback 50", rupees(50), title = "Cashback", category = refunds.id, type = INCOME),
            Case("gift from mom 2000", rupees(2000), title = "Gift Mom", category = gifts.id, currentType = INCOME),
            Case("gift from mom 2000", rupees(2000), title = "Gift Mom"),
            Case("1000 from dad", rupees(1000), title = "Dad", currentType = INCOME),

            // Unknown merchants: a title, and the category left to the user
            Case("zxqv 999", rupees(999), title = "Zxqv"),
            Case("500 to rahul", rupees(500), title = "Rahul"),
            Case("500", rupees(500)),

            // What the user picked before beats a keyword, and is sure
            Case(
                "dmart 1240 yesterday", rupees(1240), title = "Dmart", category = shopping.id,
                categoryConfidence = SURE, date = YESTERDAY,
                learned = listOf(CategoryMemory("dmart", EXPENSE, shopping.id))
            ),
            Case(
                "Bark Box 799", rupees(799), title = "Bark Box", category = pets.id, categoryConfidence = SURE,
                learned = listOf(CategoryMemory("bark box", EXPENSE, pets.id))
            ),
            // Remembered for the other type, or for a category that is gone: not used
            Case(
                "dmart 1240", rupees(1240), title = "Dmart", category = groceries.id,
                learned = listOf(CategoryMemory("dmart", INCOME, salary.id), CategoryMemory("dmart", EXPENSE, CategoryId(99)))
            )
        )
    }
}
