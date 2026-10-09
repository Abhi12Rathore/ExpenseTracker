package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.repository.TransactionRepositoryImpl
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs the Transactions search against a real SQLite database, through the repository that escapes it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TransactionDaoSearchTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = database.transactionDao()
        repository = TransactionRepositoryImpl(dao)

        dao.insertCategory(category(id = 1, name = "Food"))
        // Renamed since its transactions were saved: they still carry the old name
        dao.insertCategory(category(id = 2, name = "Commute", icon = CategoryIcon.TRAIN))

        dao.insertTransaction(transaction(1, title = "Team Lunch", note = "Paid for Riya", categoryId = 1, day = 5))
        dao.insertTransaction(transaction(2, title = "Metro card", note = "", categoryId = 2, day = 4, categoryName = "Transport"))
        dao.insertTransaction(transaction(3, title = "Groceries", note = "50% off deal", categoryId = 1, day = 3))
        dao.insertTransaction(transaction(4, title = "Snacks", note = "movie_night", categoryId = 1, day = 2))
        dao.insertTransaction(transaction(5, title = "Old lunch", note = "", categoryId = 1, day = -30))
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- The category's icon, read through the join

    @Test
    fun everyRead_carriesItsCategorysIcon() = runTest {
        val listed = repository.getTransactions(RECENT).first().associate { it.id to it.categoryIcon }

        assertEquals(
            mapOf(
                1 to CategoryIcon.RESTAURANT,
                2 to CategoryIcon.TRAIN,
                3 to CategoryIcon.RESTAURANT,
                4 to CategoryIcon.RESTAURANT
            ),
            listed
        )
        assertEquals(CategoryIcon.TRAIN, repository.getTransactionById(2)?.categoryIcon)
        assertEquals(CategoryIcon.TRAIN, repository.getAllTransactions().first().single { it.id == 2 }.categoryIcon)
        assertEquals(listOf(CategoryIcon.TRAIN), repository.getTransactions(RECENT, "metro").first().map { it.categoryIcon })
    }

    @Test
    fun changingACategorysIcon_changesItOnItsTransactions() = runTest {
        database.transactionDao().updateCategory(category(id = 2, name = "Commute", icon = CategoryIcon.DIRECTIONS_BUS))

        assertEquals(CategoryIcon.DIRECTIONS_BUS, repository.getTransactionById(2)?.categoryIcon)
    }

    @Test
    fun anIconKeyThisVersionDoesNotKnow_isReadAsTheDefault() = runTest {
        database.openHelper.writableDatabase.execSQL("UPDATE categories SET icon = 'hoverboard' WHERE id = 2")

        assertEquals(CategoryIcon.DEFAULT, repository.getTransactionById(2)?.categoryIcon)
    }

    private fun category(id: Int, name: String, icon: CategoryIcon = CategoryIcon.RESTAURANT) =
        CategoryEntity(id = id, name = name, type = TransactionType.EXPENSE, icon = icon)

    private fun transaction(
        id: Int,
        title: String,
        note: String,
        categoryId: Int,
        day: Int,
        categoryName: String = "Food"
    ) = TransactionEntity(
        id = id,
        title = title,
        amount = 100.0,
        note = note,
        dateMillis = day * DAY_MILLIS,
        categoryId = categoryId,
        categoryName = categoryName,
        transactionType = TransactionType.EXPENSE
    )

    private suspend fun search(query: String, range: DateRange = RECENT): List<Int> =
        repository.getTransactions(range, query).first().map { it.id }

    @Test
    fun blankQuery_returnsEverythingInRangeNewestFirst() = runTest {
        assertEquals(listOf(1, 2, 3, 4), search(""))
        assertEquals(listOf(1, 2, 3, 4), search("   "))
    }

    @Test
    fun matchesTitle_ignoringCase() = runTest {
        assertEquals(listOf(1), search("lunch"))
        assertEquals(listOf(1), search("LUNCH"))
        assertEquals(listOf(2), search("metro"))
    }

    @Test
    fun matchesNote() = runTest {
        assertEquals(listOf(1), search("riya"))
    }

    @Test
    fun matchesTheCategorysCurrentName_notTheStaleCopy() = runTest {
        assertEquals(listOf(2), search("commute"))
        assertEquals(emptyList<Int>(), search("transport"))
        assertEquals(listOf(1, 3, 4), search("food"))
    }

    @Test
    fun matchesPartOfAWord_andTrimsTheQuery() = runTest {
        assertEquals(listOf(3), search("  rocer "))
    }

    @Test
    fun percentAndUnderscore_areSearchedLiterally() = runTest {
        assertEquals(listOf(3), search("%"))
        assertEquals(listOf(3), search("50%"))
        assertEquals(listOf(4), search("_"))
        assertEquals(listOf(4), search("movie_night"))
        assertEquals(emptyList<Int>(), search("movie%night"))
        assertEquals(emptyList<Int>(), search("\\"))
    }

    @Test
    fun noMatch_isEmpty() = runTest {
        assertEquals(emptyList<Int>(), search("pharmacy"))
    }

    @Test
    fun staysWithinTheDateRange() = runTest {
        assertEquals(listOf(1), search("lunch"))
        assertEquals(listOf(1, 5), search("lunch", DateRange.ALL))
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000L
        val RECENT = DateRange(0L, 10 * DAY_MILLIS)
    }
}
