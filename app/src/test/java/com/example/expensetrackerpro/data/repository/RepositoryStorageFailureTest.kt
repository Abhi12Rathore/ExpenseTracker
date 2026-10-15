package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeDefaultCategoryNames
import android.app.Application
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteFullException
import com.example.expensetrackerpro.data.sample.SampleDataSeeder
import com.example.expensetrackerpro.data.sample.SampleTransactionGenerator
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.ThrowingAppDataDao
import com.example.expensetrackerpro.fakes.ThrowingCategoryDao
import com.example.expensetrackerpro.fakes.ThrowingCategoryMemoryDao
import com.example.expensetrackerpro.fakes.ThrowingSampleDataDao
import com.example.expensetrackerpro.fakes.ThrowingTransactionDao
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Every write that returns an Outcome, on a database that can't carry it out. SQLite's
 * exceptions come back as a StorageFailure; anything else is a bug and is thrown on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RepositoryStorageFailureTest {

    private val full = SQLiteFullException("database or disk is full (code 13 SQLITE_FULL)")
    private val storageFailure = Outcome.Failure(DomainError.StorageFailure(full.message))

    private val bug = IllegalStateException("a bug")

    private val food = category(id = 1, name = "Food")
    private val groceries = category(id = 2, name = "Groceries")

    private fun sampleDataSeeder(error: Exception) = SampleDataSeeder(
        ThrowingSampleDataDao(error),
        SampleTransactionGenerator(FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 10, hour = 15))),
        FakeDefaultCategoryNames(),
        FakeAppLocaleProvider()
    )

    private suspend fun assertThrowsTheBug(write: suspend () -> Any) {
        try {
            write()
        } catch (e: IllegalStateException) {
            assertSame(bug, e)
            return
        }
        fail("The exception was swallowed")
    }

    // --- Transactions

    @Test
    fun transactionWrites_areStorageFailures() = runTest {
        val repository = TransactionRepositoryImpl(ThrowingTransactionDao(full))

        assertEquals(storageFailure, repository.saveTransaction(transaction()))
        assertEquals(storageFailure, repository.updateTransaction(transaction()))
        assertEquals(storageFailure, repository.deleteTransaction(transaction()))
    }

    @Test
    fun aDeleteThatFailed_isNotReportedAsDeleted() = runTest {
        val repository = TransactionRepositoryImpl(
            ThrowingTransactionDao(SQLiteDiskIOException("disk I/O error"))
        )
        val deleted = mutableListOf<Transaction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.deletions.toList(deleted)
        }

        assertEquals(
            Outcome.Failure(DomainError.StorageFailure("disk I/O error")),
            repository.deleteTransaction(transaction())
        )

        assertTrue(deleted.isEmpty())
    }

    @Test
    fun transactionWrites_throwAnythingThatIsNotSqlites() = runTest {
        val repository = TransactionRepositoryImpl(ThrowingTransactionDao(bug))

        assertThrowsTheBug { repository.saveTransaction(transaction()) }
        assertThrowsTheBug { repository.updateTransaction(transaction()) }
        assertThrowsTheBug { repository.deleteTransaction(transaction()) }
    }

    // --- Categories

    @Test
    fun categorySaves_areStorageFailures() = runTest {
        val repository = CategoryRepositoryImpl(ThrowingCategoryDao(full))

        assertEquals(storageFailure, repository.insertCategory(food))
        assertEquals(storageFailure, repository.updateCategory(food))
        assertEquals(storageFailure, repository.renameBuiltInCategories(mapOf(food.id to "Khana")))
    }

    @Test
    fun categorySaves_throwAnythingThatIsNotSqlites() = runTest {
        val repository = CategoryRepositoryImpl(ThrowingCategoryDao(bug))

        assertThrowsTheBug { repository.insertCategory(food) }
        assertThrowsTheBug { repository.updateCategory(food) }
    }

    @Test
    fun categoryDeletes_areStorageFailures() = runTest {
        val repository = CategoryRepositoryImpl(ThrowingCategoryDao(full))

        assertEquals(storageFailure, repository.deleteCategory(food))
        assertEquals(storageFailure, repository.deleteCategoryReassigning(food, groceries))
    }

    @Test
    fun categoryDeletes_throwAnythingThatIsNotSqlites() = runTest {
        val repository = CategoryRepositoryImpl(ThrowingCategoryDao(bug))

        assertThrowsTheBug { repository.deleteCategory(food) }
        assertThrowsTheBug { repository.deleteCategoryReassigning(food, groceries) }
    }

    // --- What quick add remembers

    @Test
    fun rememberingACategory_isAStorageFailure() = runTest {
        val repository = CategoryMemoryRepositoryImpl(ThrowingCategoryMemoryDao(full))
        assertEquals(storageFailure, repository.remember(CategoryMemory("dmart", TransactionType.EXPENSE, CategoryId(1))))
    }

    @Test
    fun rememberingACategory_throwsAnythingThatIsNotSqlites() = runTest {
        val repository = CategoryMemoryRepositoryImpl(ThrowingCategoryMemoryDao(bug))
        assertThrowsTheBug { repository.remember(CategoryMemory("dmart", TransactionType.EXPENSE, CategoryId(1))) }
    }

    // --- Delete all data, load sample data

    @Test
    fun deleteAllData_isAStorageFailure() = runTest {
        assertEquals(storageFailure, AppDataRepositoryImpl(ThrowingAppDataDao(full)).deleteAllData())
    }

    @Test
    fun deleteAllData_throwsAnythingThatIsNotSqlites() = runTest {
        assertThrowsTheBug { AppDataRepositoryImpl(ThrowingAppDataDao(bug)).deleteAllData() }
    }

    @Test
    fun loadSampleData_isAStorageFailure() = runTest {
        assertEquals(storageFailure, sampleDataSeeder(full).loadSampleData())
    }

    @Test
    fun loadSampleData_throwsAnythingThatIsNotSqlites() = runTest {
        assertThrowsTheBug { sampleDataSeeder(bug).loadSampleData() }
    }
}
