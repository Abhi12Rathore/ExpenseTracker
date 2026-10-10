package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.domain.models.DefaultCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Creates a real database the way the app does and reads back what it was seeded with. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DefaultCategoriesSeedTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().addCallback(ExpenseDatabaseCallback()).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aNewDatabase_holdsTheDefaults_withTheirIconsAndTypes() = runTest {
        val seeded = database.categoryDao().getAllCategories().first().map { it.toDomain() }

        // Ids are assigned in order, starting at 1
        val expected = DefaultCategories.all.mapIndexed { index, category -> category.copy(id = index + 1) }
        assertEquals(expected, seeded)
    }
}
