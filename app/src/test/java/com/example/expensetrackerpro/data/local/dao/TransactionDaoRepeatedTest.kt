package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.repository.TransactionRepositoryImpl
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.quickadd.GetFrequentTransactionsUseCase
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.millisAt
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

/** The repeated-transactions query behind the frequent spends, and the all-or-nothing batch insert, on real SQLite. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TransactionDaoRepeatedTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TransactionDao
    private lateinit var repository: TransactionRepositoryImpl
    private var nextId = 1

    // Thursday 8 Oct 2026, 12:00 in Kolkata
    private val now = millisAt(2026, Calendar.OCTOBER, 8, hour = 12)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.transactionDao()
        repository = TransactionRepositoryImpl(dao)

        database.categoryDao().insertCategory(CategoryEntity(id = FOOD, name = "Food", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT))
        database.categoryDao().insertCategory(CategoryEntity(id = TRAVEL, name = "Travel", type = TransactionType.EXPENSE, icon = CategoryIcon.TRAIN))
        database.categoryDao().insertCategory(CategoryEntity(id = SALARY, name = "Salary", type = TransactionType.INCOME, icon = CategoryIcon.PAYMENTS))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insert(
        title: String,
        daysAgo: Int,
        rupees: Long = 100,
        categoryId: Int = FOOD,
        type: TransactionType = TransactionType.EXPENSE
    ) {
        dao.insertTransaction(
            TransactionEntity(
                id = nextId++,
                title = title,
                amountPaise = rupees * 100,
                note = "",
                dateMillis = now - daysAgo * DAY_MILLIS,
                categoryId = categoryId,
                transactionType = type
            )
        )
    }

    private suspend fun repeated(type: TransactionType = TransactionType.EXPENSE, minCount: Int = 3): List<String> =
        repository.getRepeatedTransactions(type, DateRange(now - 100 * DAY_MILLIS, now + DAY_MILLIS), minCount)
            .map { it.title }

    // --- The query

    @Test
    fun aTitleAndCategory_needAtLeastTheThreshold() = runTest {
        repeat(3) { insert("Swiggy", daysAgo = it) }
        repeat(2) { insert("Uber", daysAgo = it, categoryId = TRAVEL) }

        assertEquals(listOf("Swiggy", "Swiggy", "Swiggy"), repeated())
        assertEquals(5, repeated(minCount = 2).size)
    }

    @Test
    fun caseAndSurroundingSpaces_areOneTitle() = runTest {
        insert("Swiggy", daysAgo = 1)
        insert("swiggy ", daysAgo = 2)
        insert(" SWIGGY", daysAgo = 3)

        assertEquals(3, repeated().size)
    }

    @Test
    fun theSameTitleInAnotherCategory_isCountedApart() = runTest {
        repeat(2) { insert("Petrol", daysAgo = it) }
        repeat(2) { insert("Petrol", daysAgo = it, categoryId = TRAVEL) }

        assertEquals(emptyList<String>(), repeated())
    }

    @Test
    fun onlyTheTypeAskedFor() = runTest {
        repeat(3) { insert("Swiggy", daysAgo = it) }
        repeat(3) { insert("Acme payroll", daysAgo = it * 7, categoryId = SALARY, type = TransactionType.INCOME) }

        assertEquals(setOf("Swiggy"), repeated(TransactionType.EXPENSE).toSet())
        assertEquals(setOf("Acme payroll"), repeated(TransactionType.INCOME).toSet())
    }

    @Test
    fun onlyTransactionsInTheRange_countOrComeBack() = runTest {
        repeat(2) { insert("Swiggy", daysAgo = it) }
        insert("Swiggy", daysAgo = 200)

        assertEquals(emptyList<String>(), repeated())
    }

    @Test
    fun newestFirst_withItsCategorysNameAndIcon() = runTest {
        insert("Swiggy", daysAgo = 5, rupees = 1)
        insert("Swiggy", daysAgo = 1, rupees = 2)
        insert("Swiggy", daysAgo = 3, rupees = 3)

        val found = repository.getRepeatedTransactions(TransactionType.EXPENSE, DateRange(0, now + DAY_MILLIS), 3)

        assertEquals(listOf(2L, 3L, 1L), found.map { it.amount.minorUnits / 100 })
        assertTrue(found.all { it.categoryName == "Food" && it.categoryIcon == CategoryIcon.RESTAURANT })
    }

    // --- The window the frequent spends look at: today and the 59 days before it

    @Test
    fun frequentSpends_lookBack60DaysIncludingToday() = runTest {
        val frequent = GetFrequentTransactionsUseCase(repository, FakeTimeProvider(now))
        insert("Swiggy", daysAgo = 0)
        insert("Swiggy", daysAgo = 10)
        insert("Swiggy", daysAgo = 60)

        assertEquals(emptyList<String>(), frequent(TransactionType.EXPENSE).map { it.title })

        insert("Swiggy", daysAgo = 59)

        assertEquals(listOf("Swiggy"), frequent(TransactionType.EXPENSE).map { it.title })
        assertEquals(3, frequent(TransactionType.EXPENSE).single().count)
    }

    // --- Several saved at once

    @Test
    fun aBatch_isStoredWhole() = runTest {
        val result = repository.saveTransactions(listOf(transaction("Swiggy", FOOD), transaction("Uber", TRAVEL)))

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(setOf("Swiggy", "Uber"), repository.getAllTransactions().first().map { it.title }.toSet())
    }

    @Test
    fun aBatchWithOneBadRow_storesNothing() = runTest {
        insert("Already there", daysAgo = 1)

        // The second one's category is not stored, which breaks its foreign key
        val result = repository.saveTransactions(listOf(transaction("Swiggy", FOOD), transaction("Uber", 99)))

        assertEquals(Outcome.Failure(DomainError.NotFound), result)
        assertEquals(listOf("Already there"), repository.getAllTransactions().first().map { it.title })
    }

    private fun transaction(title: String, categoryId: Int) = Transaction(
        title = title,
        amount = Money.ofRupees(100),
        note = "",
        dateMillis = now,
        categoryId = CategoryId(categoryId),
        categoryName = "",
        transactionType = TransactionType.EXPENSE
    )

    private companion object {
        const val FOOD = 1
        const val TRAVEL = 2
        const val SALARY = 3
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
