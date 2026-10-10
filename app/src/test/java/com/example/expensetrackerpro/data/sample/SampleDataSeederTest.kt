package com.example.expensetrackerpro.data.sample

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.data.repository.TransactionRepositoryImpl
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.sample.LoadSampleDataUseCase
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

/** Loads sample data into a real database, created and seeded the way the app's is. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SampleDataSeederTest {

    private val timeProvider = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 10, hour = 15))
    private val generator = SampleTransactionGenerator(timeProvider)

    private lateinit var database: AppDatabase
    private lateinit var loadSampleData: LoadSampleDataUseCase

    private val dao get() = database.transactionDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().addCallback(ExpenseDatabaseCallback()).build()

        val seeder = SampleDataSeeder(database.sampleDataDao(), generator)
        loadSampleData = LoadSampleDataUseCase(seeder)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun transactions() = TransactionRepositoryImpl(dao).getAllTransactions().first()

    private suspend fun categories() = dao.getAllCategories().first()

    private suspend fun addCustomCategoryWithATransaction() {
        val id = dao.insertCategory(
            CategoryEntity(name = "Pottery", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
        ).toInt()
        dao.insertTransaction(
            TransactionEntity(
                title = "Clay",
                amount = 400.0,
                note = "",
                dateMillis = 0L,
                categoryId = id,
                categoryName = "Pottery",
                transactionType = TransactionType.EXPENSE
            )
        )
    }

    @Test
    fun load_writesEveryGeneratedTransaction_underItsCategory() = runTest {
        val expected = generator()

        assertEquals(expected.size, loadSampleData())

        val stored = transactions().sortedBy { it.dateMillis }
        assertEquals(expected.map { it.title to it.amount }.sortedBy { it.first }, stored.map { it.title to it.amount }.sortedBy { it.first })
        assertEquals(expected.map { it.categoryName }.toSet(), stored.map { it.categoryName }.toSet())

        // The icon shown comes from the category each one was filed under
        val iconByName = DefaultCategories.all.associate { it.name to it.icon }
        assertTrue(stored.all { it.categoryIcon == iconByName[it.categoryName] })
    }

    @Test
    fun load_replacesWhatWasThere_andLoadingAgainDoesNotDouble() = runTest {
        addCustomCategoryWithATransaction()

        val count = loadSampleData()
        loadSampleData()

        assertEquals(count, transactions().size)
        assertTrue(transactions().none { it.title == "Clay" })
        // Categories are left alone
        assertTrue(categories().any { it.name == "Pottery" })
    }

    @Test
    fun load_putsBackADefaultCategoryThatWasDeleted() = runTest {
        val fuel = categories().single { it.name == "Fuel" }
        dao.deleteCategory(fuel)

        val count = loadSampleData()

        assertEquals(generator().size, count)
        assertEquals(1, categories().count { it.name == "Fuel" })
        assertTrue(transactions().any { it.categoryName == "Fuel" })
        assertEquals(DefaultCategories.all.size, categories().size)
    }
}
