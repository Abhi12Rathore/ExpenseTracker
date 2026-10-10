package com.example.expensetrackerpro.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The transaction writes against a real SQLite database, foreign keys and all. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TransactionRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TransactionRepositoryImpl

    private val food = CategoryEntity(id = 1, name = "Food", type = TransactionType.EXPENSE, icon = CategoryIcon.RESTAURANT)

    // Filed under Food, the only category there is
    private val lunch = transaction(id = 5, amount = rupees(120), note = "", title = "Lunch")
        .copy(categoryIcon = CategoryIcon.RESTAURANT)

    private val success = Outcome.Success(Unit)
    private val categoryGone = Outcome.Failure(DomainError.NotFound)

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TransactionRepositoryImpl(database.transactionDao())

        database.categoryDao().insertCategory(food)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun stored(): List<Transaction> = repository.getAllTransactions().first()

    @Test
    fun save_storesTheTransaction() = runTest {
        assertEquals(success, repository.saveTransaction(lunch))

        assertEquals(listOf(lunch), stored())
    }

    @Test
    fun save_underACategoryThatIsGone_isNotFound_andStoresNothing() = runTest {
        assertEquals(categoryGone, repository.saveTransaction(lunch.copy(categoryId = CategoryId(99))))

        assertTrue(stored().isEmpty())
    }

    @Test
    fun update_changesTheStoredTransaction() = runTest {
        repository.saveTransaction(lunch)
        val edited = lunch.copy(amount = rupees(150), title = "Team lunch")

        assertEquals(success, repository.updateTransaction(edited))

        assertEquals(listOf(edited), stored())
    }

    @Test
    fun update_toACategoryThatIsGone_isNotFound_andKeepsWhatWasStored() = runTest {
        repository.saveTransaction(lunch)

        assertEquals(categoryGone, repository.updateTransaction(lunch.copy(categoryId = CategoryId(99))))

        assertEquals(listOf(lunch), stored())
    }

    @Test
    fun delete_removesTheTransaction_andReportsItAsDeleted() = runTest {
        repository.saveTransaction(lunch)
        val deleted = mutableListOf<Transaction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.deletions.toList(deleted)
        }

        assertEquals(success, repository.deleteTransaction(lunch))

        assertTrue(stored().isEmpty())
        assertEquals(listOf(lunch), deleted)
    }

    @Test
    fun aDeletedTransaction_canBeSavedBackUnderItsId() = runTest {
        repository.saveTransaction(lunch)
        repository.deleteTransaction(lunch)

        assertEquals(success, repository.saveTransaction(lunch))

        assertEquals(lunch, repository.getTransactionById(lunch.id))
    }

    @Test
    fun aDeletedTransactionWhoseCategoryWentToo_cannotBeSavedBack() = runTest {
        repository.saveTransaction(lunch)
        repository.deleteTransaction(lunch)
        database.categoryDao().deleteCategory(food)

        assertEquals(categoryGone, repository.saveTransaction(lunch))

        assertNull(repository.getTransactionById(lunch.id))
    }
}
