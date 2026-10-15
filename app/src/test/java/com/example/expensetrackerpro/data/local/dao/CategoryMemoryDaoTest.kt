package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.category.ResourceDefaultCategoryNames
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.CategoryMemoryEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
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

/** What quick add remembers, in a real database created and seeded the way the app's is. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CategoryMemoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CategoryMemoryDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().addCallback(
            ExpenseDatabaseCallback(
                ResourceDefaultCategoryNames(ApplicationProvider.getApplicationContext()),
                FakeAppLocaleProvider()
            )
        ).build()
        dao = database.categoryMemoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun builtInExpense(): Int =
        database.appDataDao().getAllCategories().first { !it.isCustom && it.type == TransactionType.EXPENSE }.id

    private suspend fun customCategory(name: String = "Pets"): Int =
        database.categoryDao().insertCategory(
            CategoryEntity(name = name, type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
        ).toInt()

    @Test
    fun aTitleIsRememberedOncePerType_theLatestChoiceWinning() = runTest {
        val first = builtInExpense()
        val second = customCategory()

        dao.upsert(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, first))
        dao.upsert(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, second))
        dao.upsert(CategoryMemoryEntity("dmart", TransactionType.INCOME, first))

        assertEquals(
            setOf(
                CategoryMemoryEntity("dmart", TransactionType.EXPENSE, second),
                CategoryMemoryEntity("dmart", TransactionType.INCOME, first)
            ),
            dao.observeAll().first().toSet()
        )
    }

    @Test
    fun deletingACategory_forgetsWhatWasRememberedForIt() = runTest {
        val pets = customCategory()
        val kept = builtInExpense()
        dao.upsert(CategoryMemoryEntity("bark box", TransactionType.EXPENSE, pets))
        dao.upsert(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, kept))

        database.categoryDao().deleteCategoryById(pets)

        assertEquals(listOf(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, kept)), dao.observeAll().first())
    }

    @Test
    fun aCategoryDeletedByMovingItsTransactions_isForgottenToo() = runTest {
        val pets = customCategory()
        val target = builtInExpense()
        dao.upsert(CategoryMemoryEntity("bark box", TransactionType.EXPENSE, pets))

        database.categoryDao().reassignAndDelete(fromId = pets, toId = target)

        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun deleteAllData_forgetsEverything_builtInCategoriesIncluded() = runTest {
        dao.upsert(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, builtInExpense()))
        dao.upsert(CategoryMemoryEntity("bark box", TransactionType.EXPENSE, customCategory()))

        database.appDataDao().deleteAll()

        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun aRestore_forgetsEverything() = runTest {
        dao.upsert(CategoryMemoryEntity("dmart", TransactionType.EXPENSE, builtInExpense()))
        val backedUp = CategoryEntity(id = 3, name = "Eating out", type = TransactionType.EXPENSE, icon = CategoryIcon.LOCAL_CAFE)

        database.appDataDao().replaceAll(listOf(backedUp), emptyList())

        assertTrue(dao.observeAll().first().isEmpty())
    }
}
