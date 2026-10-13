package com.example.expensetrackerpro.data.local.dao

import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.data.category.ResourceDefaultCategoryNames
import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.data.local.model.StoredData
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Replaces everything in a real database, created and seeded the way the app's is. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppDataDaoRestoreTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDataDao

    // What a backup brings: ids that have nothing to do with the ones in use
    private val backedUpFood = CategoryEntity(id = 3, name = "Eating out", type = TransactionType.EXPENSE, icon = CategoryIcon.LOCAL_CAFE, isCustom = false)
    private val backedUpPay = CategoryEntity(id = 40, name = "Freelance", type = TransactionType.INCOME, icon = CategoryIcon.LAPTOP, isCustom = true)
    private val backedUpTransactions = listOf(
        TransactionEntity(title = "Coffee", amountPaise = 180_00, note = "", dateMillis = 300L, categoryId = 3, transactionType = TransactionType.EXPENSE),
        TransactionEntity(title = "Invoice 12", amountPaise = 40_000_00, note = "paid late", dateMillis = 200L, categoryId = 40, transactionType = TransactionType.INCOME)
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().addCallback(ExpenseDatabaseCallback(ResourceDefaultCategoryNames(ApplicationProvider.getApplicationContext()), FakeAppLocaleProvider())).build()
        dao = database.appDataDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** The seeded categories plus a custom one, with a transaction under each of two of them. */
    private suspend fun fillWithCurrentData(): StoredData {
        val customId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Pottery", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
        ).toInt()
        val builtIn = dao.getAllCategories().first { !it.isCustom && it.type == TransactionType.EXPENSE }
        listOf(customId to "Clay", builtIn.id to "Lunch").forEach { (categoryId, title) ->
            database.transactionDao().insertTransaction(
                TransactionEntity(title = title, amountPaise = 400_00, note = "", dateMillis = 50L, categoryId = categoryId, transactionType = TransactionType.EXPENSE)
            )
        }

        return dao.snapshot()
    }

    @Test
    fun snapshot_isEveryCategoryAndTransaction() = runTest {
        val current = fillWithCurrentData()

        assertEquals(DefaultCategories.all.size + 1, current.categories.size)
        assertEquals(listOf("Clay", "Lunch"), current.transactions.map { it.title }.sorted())
        assertEquals(current.categories, database.categoryDao().getAllCategories().first().sortedBy { it.id })
    }

    @Test
    fun replaceAll_leavesOnlyWhatItWasGiven_underTheGivenCategoryIds() = runTest {
        fillWithCurrentData()

        dao.replaceAll(listOf(backedUpFood, backedUpPay), backedUpTransactions)

        val after = dao.snapshot()
        assertEquals(listOf(backedUpFood, backedUpPay), after.categories)
        // Newest first, each under a new id of its own
        assertEquals(backedUpTransactions, after.transactions.map { it.copy(id = 0) })
        assertTrue(after.transactions.all { it.id > 0 })
    }

    @Test
    fun replaceAll_removesTheBuiltInCategoriesToo_whenTheBackupHasNone() = runTest {
        fillWithCurrentData()

        dao.replaceAll(emptyList(), emptyList())

        assertEquals(StoredData(emptyList(), emptyList()), dao.snapshot())
    }

    @Test
    fun afterReplaceAll_theRestOfTheAppReadsTheNewData_andNewCategoriesGetFreshIds() = runTest {
        fillWithCurrentData()

        dao.replaceAll(listOf(backedUpFood, backedUpPay), backedUpTransactions)

        val listed = database.transactionDao().getAllTransactions().first()
        assertEquals(listOf("Coffee" to "Eating out", "Invoice 12" to "Freelance"), listed.map { it.transaction.title to it.categoryName })
        val newId = database.categoryDao().insertCategory(
            CategoryEntity(name = "New", type = TransactionType.EXPENSE, icon = CategoryIcon.DEFAULT)
        )
        assertTrue(newId > backedUpPay.id)
    }

    @Test
    fun replaceAll_thatFailsOnTheLastStep_leavesTheOldDataUntouched() = runTest {
        val before = fillWithCurrentData()
        // The second transaction names a category that is not being restored
        val broken = backedUpTransactions + backedUpTransactions.first().copy(categoryId = 999)

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.replaceAll(listOf(backedUpFood, backedUpPay), broken) }
        }

        assertEquals(before, dao.snapshot())
    }

    @Test
    fun replaceAll_thatFailsOnTheCategories_leavesTheOldDataUntouched() = runTest {
        val before = fillWithCurrentData()
        // Two categories under one id
        val clashing = listOf(backedUpFood, backedUpPay.copy(id = backedUpFood.id))

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.replaceAll(clashing, emptyList()) }
        }

        assertEquals(before, dao.snapshot())
    }
}
