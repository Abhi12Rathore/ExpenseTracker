package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.repository.DashboardRepositoryImpl
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.rupees
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Runs the dashboard's report queries against a real SQLite database, through the repository.
 * The queries group by SQLite's local day and month, so every transaction here sits around
 * midday and mid-month, where the grouping is the same in whichever zone the tests run.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DashboardDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: DashboardRepositoryImpl

    private val food = CategoryEntity(id = 1, name = "Food", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT)
    private val bills = CategoryEntity(id = 2, name = "Bills", type = TransactionType.EXPENSE, icon = CategoryIcon.RECEIPT_LONG)
    private val pets = CategoryEntity(id = 3, name = "Pets", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
    private val salary = CategoryEntity(id = 4, name = "Salary", type = TransactionType.INCOME, icon = CategoryIcon.PAYMENTS)

    private val october = DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val year2026 = DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))

    private val oct10Noon = millisAt(2026, Calendar.OCTOBER, 10, hour = 12)
    private val oct10Afternoon = millisAt(2026, Calendar.OCTOBER, 10, hour = 13)
    private val oct12Noon = millisAt(2026, Calendar.OCTOBER, 12, hour = 12)
    private val oct15Noon = millisAt(2026, Calendar.OCTOBER, 15, hour = 12)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = DashboardRepositoryImpl(database.dashboardDao())

        listOf(food, bills, pets, salary).forEach { database.categoryDao().insertCategory(it) }

        val dao = database.transactionDao()
        // August and September, for the months before the one the reports are asked for
        dao.insertTransaction(transaction(1, salary, rupees(80000), millisAt(2026, Calendar.AUGUST, 15, hour = 12)))
        dao.insertTransaction(transaction(2, food, rupees(700), millisAt(2026, Calendar.SEPTEMBER, 15, hour = 12)))
        // October: two expenses and the salary on the 10th, one expense on the 12th and one on the 15th
        dao.insertTransaction(transaction(3, food, rupees(250), oct10Noon))
        dao.insertTransaction(transaction(4, bills, rupees(1200), oct10Afternoon))
        dao.insertTransaction(transaction(5, salary, rupees(90000), oct10Afternoon))
        dao.insertTransaction(transaction(6, food, Money(400_50), oct12Noon))
        dao.insertTransaction(transaction(7, bills, rupees(300), oct15Noon))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun transaction(id: Int, category: CategoryEntity, amount: Money, dateMillis: Long) =
        TransactionEntity(
            id = id,
            title = "Transaction $id",
            amountPaise = amount.paise,
            note = "",
            dateMillis = dateMillis,
            categoryId = category.id,
            transactionType = category.type
        )

    // --- Category breakdown

    @Test
    fun categoryBreakdown_totalsTheRangesExpensesPerCategory_highestFirst() = runTest {
        // No income, nothing from September, and no category without an expense
        assertEquals(
            listOf(
                CategoryBreakdown(bills.id.toLong(), "Bills", CategoryIcon.RECEIPT_LONG, rupees(1500)),
                CategoryBreakdown(food.id.toLong(), "Food", CategoryIcon.RESTAURANT, Money(650_50))
            ),
            repository.getCategoryBreakdown(october).first()
        )
    }

    @Test
    fun categoryBreakdown_usesTheCategorysCurrentNameAndIcon() = runTest {
        database.categoryDao().update(food.copy(name = "Eating out", icon = CategoryIcon.FASTFOOD))

        val eatingOut = repository.getCategoryBreakdown(october).first().single { it.categoryId == food.id.toLong() }

        assertEquals("Eating out", eatingOut.categoryName)
        assertEquals(CategoryIcon.FASTFOOD, eatingOut.icon)
    }

    @Test
    fun aRangeWithNothingInIt_isEmptyInEveryReport() = runTest {
        val january = DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2026, Calendar.FEBRUARY, 1))

        assertTrue(repository.getCategoryBreakdown(january).first().isEmpty())
        assertTrue(repository.getCashFlow(january).first().isEmpty())
        assertTrue(repository.getDailyExpenses(january).first().isEmpty())
        assertTrue(repository.getMonthlyTrend(january).first().isEmpty())
    }

    // --- Per day

    @Test
    fun cashFlow_sumsIncomeAndExpensePerDay_oldestFirst() = runTest {
        val days = repository.getCashFlow(october).first()

        assertEquals(listOf(rupees(90000), Money.ZERO, Money.ZERO), days.map { it.income })
        assertEquals(listOf(rupees(1450), Money(400_50), rupees(300)), days.map { it.expense })
        // Each point is dated within its own day
        assertTrue(days[0].dateMillis in oct10Noon..oct10Afternoon)
        assertEquals(listOf(oct12Noon, oct15Noon), days.drop(1).map { it.dateMillis })
    }

    @Test
    fun dailyExpenses_sumAndCountOnlyExpenses_datedAtTheDaysFirstOne() = runTest {
        // The salary on the 10th is neither summed nor counted
        assertEquals(
            listOf(
                DailyExpense(dateMillis = oct10Noon, amount = rupees(1450), transactionCount = 2),
                DailyExpense(dateMillis = oct12Noon, amount = Money(400_50), transactionCount = 1),
                DailyExpense(dateMillis = oct15Noon, amount = rupees(300), transactionCount = 1)
            ),
            repository.getDailyExpenses(october).first()
        )
    }

    // --- Per month

    @Test
    fun monthlyTrend_sumsIncomeAndExpensePerMonth_oldestFirst() = runTest {
        // August and September are months 8 and 9: read as text, "08" and "09" would come back as 0
        assertEquals(
            listOf(
                MonthlyTrend(year = 2026, month = 8, income = rupees(80000), expense = Money.ZERO),
                MonthlyTrend(year = 2026, month = 9, income = Money.ZERO, expense = rupees(700)),
                MonthlyTrend(year = 2026, month = 10, income = rupees(90000), expense = Money(2150_50))
            ),
            repository.getMonthlyTrend(year2026).first()
        )
    }

    @Test
    fun monthlyTrend_keepsToTheRangeItIsAskedFor() = runTest {
        assertEquals(
            listOf(MonthlyTrend(year = 2026, month = 10, income = rupees(90000), expense = Money(2150_50))),
            repository.getMonthlyTrend(october).first()
        )
    }
}
