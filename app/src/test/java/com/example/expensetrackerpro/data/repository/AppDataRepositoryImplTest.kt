package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.data.category.ResourceDefaultCategoryNames
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.data.DeleteAllDataUseCase
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

/** Deletes everything from a real database, created and seeded the way the app's is. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppDataRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var deleteAllData: DeleteAllDataUseCase

    private val dao get() = database.transactionDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().addCallback(ExpenseDatabaseCallback(ResourceDefaultCategoryNames(ApplicationProvider.getApplicationContext()), FakeAppLocaleProvider())).build()

        deleteAllData = DeleteAllDataUseCase(AppDataRepositoryImpl(database.appDataDao()))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun categories() = database.categoryDao().getAllCategories().first()

    private suspend fun addTransaction(category: CategoryEntity) {
        dao.insertTransaction(
            TransactionEntity(
                title = "Something",
                amountPaise = 400_00,
                note = "",
                dateMillis = 0L,
                categoryId = category.id,
                transactionType = category.type
            )
        )
    }

    @Test
    fun deletesTransactionsAndCustomCategories_andKeepsTheBuiltInOnes() = runTest {
        val potteryId = database.categoryDao().insertCategory(
            CategoryEntity(name = "Pottery", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
        ).toInt()
        // One under a custom category, one under a built-in one
        addTransaction(categories().single { it.id == potteryId })
        addTransaction(categories().first { !it.isCustom })

        assertEquals(Outcome.Success(Unit), deleteAllData())

        assertTrue(dao.getAllTransactions().first().isEmpty())
        assertEquals(DefaultCategories.all, categories().map { it.builtInKey })
    }

    @Test
    fun aBuiltInCategoryThatWasRenamed_keepsItsNewName() = runTest {
        val first = categories().first()
        database.categoryDao().update(first.copy(name = "Eating out"))

        assertEquals(Outcome.Success(Unit), deleteAllData())

        assertEquals("Eating out", categories().single { it.id == first.id }.name)
    }

    @Test
    fun onAnEmptyDatabase_isFine() = runTest {
        assertEquals(Outcome.Success(Unit), deleteAllData())
        assertEquals(DefaultCategories.all.size, categories().size)
    }
}
