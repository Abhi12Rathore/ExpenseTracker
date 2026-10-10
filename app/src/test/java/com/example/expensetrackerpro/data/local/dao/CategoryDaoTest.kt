package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.data.repository.CategoryRepositoryImpl
import com.example.expensetrackerpro.data.repository.TransactionRepositoryImpl
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.millisAt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/** Runs the usage query and the reassigning delete against a real SQLite database, through the repository. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CategoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl

    private val food = CategoryEntity(id = 1, name = "Food", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT, isCustom = false)
    private val groceries = CategoryEntity(id = 2, name = "Groceries", type = TransactionType.EXPENSE, icon = CategoryIcon.SHOPPING_BASKET)
    private val pets = CategoryEntity(id = 3, name = "Pets", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
    private val salary = CategoryEntity(id = 4, name = "Salary", type = TransactionType.INCOME, icon = CategoryIcon.PAYMENTS, isCustom = false)

    private val october = DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = database.transactionDao()
        repository = CategoryRepositoryImpl(database.categoryDao())
        transactions = TransactionRepositoryImpl(dao)

        listOf(food, groceries, pets, salary).forEach { database.categoryDao().insertCategory(it) }

        // Food: one before October, two in it (the first at its very first moment), one after
        dao.insertTransaction(transaction(1, food, 100.0, millisAt(2026, Calendar.SEPTEMBER, 30, hour = 23, minute = 59)))
        dao.insertTransaction(transaction(2, food, 250.0, october.startMillis))
        dao.insertTransaction(transaction(3, food, 400.5, millisAt(2026, Calendar.OCTOBER, 20)))
        dao.insertTransaction(transaction(4, food, 999.0, october.endMillis))
        // Groceries: only last month
        dao.insertTransaction(transaction(5, groceries, 60.0, millisAt(2026, Calendar.SEPTEMBER, 12)))
        dao.insertTransaction(transaction(6, salary, 90000.0, millisAt(2026, Calendar.OCTOBER, 1, hour = 9)))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun transaction(id: Int, category: CategoryEntity, amount: Double, dateMillis: Long) =
        TransactionEntity(
            id = id,
            title = "Transaction $id",
            amount = amount,
            note = "",
            dateMillis = dateMillis,
            categoryId = category.id,
            categoryName = category.name,
            transactionType = category.type
        )

    private suspend fun usage(range: DateRange = october) =
        repository.getCategoryUsage(range).first().associateBy { it.category.id }

    private suspend fun categoryIds() = repository.getAllCategories().first().map { it.id }

    // --- Usage

    @Test
    fun usage_countsEveryTransaction_andSumsOnlyThoseInTheRange() = runTest {
        val usage = usage()

        // The range starts inclusive and ends exclusive
        assertEquals(
            CategoryUsage(food.toDomain(), totalCount = 4, periodCount = 2, periodAmount = 650.5),
            usage[food.id]
        )
        assertEquals(
            CategoryUsage(salary.toDomain(), totalCount = 1, periodCount = 1, periodAmount = 90000.0),
            usage[salary.id]
        )
    }

    @Test
    fun usage_includesCategoriesNotUsedInTheRange_orEver_withZeros() = runTest {
        val usage = usage()

        assertEquals(setOf(food.id, groceries.id, pets.id, salary.id), usage.keys)
        assertEquals(CategoryUsage(groceries.toDomain(), totalCount = 1), usage[groceries.id])
        assertEquals(CategoryUsage(pets.toDomain()), usage[pets.id])
    }

    @Test
    fun usage_followsTheRangeItIsAskedFor() = runTest {
        val september = DateRange(millisAt(2026, Calendar.SEPTEMBER, 1), october.startMillis)

        val usage = usage(september)

        assertEquals(1, usage.getValue(food.id).periodCount)
        assertEquals(100.0, usage.getValue(food.id).periodAmount, 0.0)
        assertEquals(60.0, usage.getValue(groceries.id).periodAmount, 0.0)
        assertEquals(0, usage.getValue(salary.id).periodCount)
    }

    // --- Rename

    @Test
    fun renaming_updatesTheNameOnTheCategorysTransactions_andOnNoOthers() = runTest {
        repository.updateCategory(food.toDomain().copy(name = "Eating out", icon = CategoryIcon.FASTFOOD))

        val stored = repository.getAllCategories().first().single { it.id == food.id }
        assertEquals("Eating out", stored.name)
        assertEquals(CategoryIcon.FASTFOOD, stored.icon)
        // Still built in, and still an expense
        assertFalse(stored.isCustom)
        assertEquals(TransactionType.EXPENSE, stored.type)

        val all = transactions.getAllTransactions().first()
        val renamed = all.filter { it.categoryId == food.id }
        assertEquals(4, renamed.size)
        assertTrue(renamed.all { it.categoryName == "Eating out" })
        assertEquals(listOf("Groceries", "Salary"), all.filter { it.categoryId != food.id }.map { it.categoryName }.sorted())
        // Usage is untouched by a rename
        assertEquals(4, usage().getValue(food.id).totalCount)
    }

    @Test
    fun updatingOnlyTheIcon_leavesTheTransactionsNamesAsTheyWere() = runTest {
        repository.updateCategory(groceries.toDomain().copy(icon = CategoryIcon.STOREFRONT))

        assertEquals("Groceries", transactions.getTransactionById(5)?.categoryName)
        assertEquals(CategoryIcon.STOREFRONT, transactions.getTransactionById(5)?.categoryIcon)
    }

    // --- Reassign, then delete

    @Test
    fun reassign_movesEveryTransactionToTheTarget_thenDeletesTheCategory() = runTest {
        assertTrue(repository.deleteCategoryReassigning(food.toDomain(), groceries.toDomain()))

        assertEquals(listOf(groceries.id, pets.id, salary.id), categoryIds())

        val usage = usage()
        assertEquals(
            CategoryUsage(groceries.toDomain(), totalCount = 5, periodCount = 2, periodAmount = 650.5),
            usage[groceries.id]
        )

        // Each moved transaction now reads as the target, by id, name and icon
        val moved = transactions.getAllTransactions().first().filter { it.id in 1..4 }
        assertEquals(4, moved.size)
        assertTrue(moved.all { it.categoryId == groceries.id })
        assertTrue(moved.all { it.categoryName == "Groceries" })
        assertTrue(moved.all { it.categoryIcon == CategoryIcon.SHOPPING_BASKET })
        // Nothing else about them changed
        assertEquals(listOf(100.0, 250.0, 400.5, 999.0), moved.sortedBy { it.id }.map { it.amount })
    }

    @Test
    fun reassign_leavesOtherCategoriesTransactionsAlone() = runTest {
        repository.deleteCategoryReassigning(food.toDomain(), groceries.toDomain())

        val salaryTransaction = transactions.getTransactionById(6)
        assertEquals(salary.id, salaryTransaction?.categoryId)
        assertEquals("Salary", salaryTransaction?.categoryName)
    }

    @Test
    fun reassign_usesTheTargetsStoredName_notTheOneHandedIn() = runTest {
        val staleTarget = groceries.toDomain().copy(name = "Old name")

        repository.deleteCategoryReassigning(food.toDomain(), staleTarget)

        assertEquals("Groceries", transactions.getTransactionById(1)?.categoryName)
    }

    @Test
    fun reassign_toACategoryOfAnotherType_changesNothing() = runTest {
        assertFalse(repository.deleteCategoryReassigning(food.toDomain(), salary.toDomain()))

        assertEquals(listOf(food.id, groceries.id, pets.id, salary.id), categoryIds())
        assertEquals(4, usage().getValue(food.id).totalCount)
        assertEquals(1, usage().getValue(salary.id).totalCount)
    }

    @Test
    fun reassign_toItselfOrToAMissingCategory_changesNothing() = runTest {
        val missing = pets.toDomain().copy(id = 99)

        assertFalse(repository.deleteCategoryReassigning(food.toDomain(), food.toDomain()))
        assertFalse(repository.deleteCategoryReassigning(food.toDomain(), missing))
        assertFalse(repository.deleteCategoryReassigning(missing, groceries.toDomain()))

        assertEquals(listOf(food.id, groceries.id, pets.id, salary.id), categoryIds())
        assertEquals(4, usage().getValue(food.id).totalCount)
        assertEquals(1, usage().getValue(groceries.id).totalCount)
    }

    @Test
    fun aPlainDelete_isStillRefusedWhileTransactionsRemain_andWorksOnceTheyAreGone() = runTest {
        assertFalse(repository.deleteCategory(groceries.toDomain()))
        assertTrue(repository.deleteCategory(pets.toDomain()))

        assertEquals(listOf(food.id, groceries.id, salary.id), categoryIds())
    }
}
