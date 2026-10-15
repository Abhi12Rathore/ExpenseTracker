package com.example.expensetrackerpro.data.local.dao

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.category.ResourceDefaultCategoryNames
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * An app updated over an older schema: the database is emptied (pre-release, no migrations) and
 * must come back with the default categories, not crash or open with none.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class DestructiveMigrationSeedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(NAME)
    }

    @Test
    fun anOlderSchema_isEmptied_andTheDefaultsAreSeededAgain() = runTest {
        // What version 4 left on the phone: a category and an old version number
        context.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY, name TEXT)")
            old.execSQL("INSERT INTO categories (id, name) VALUES (1, 'Old')")
            old.version = 4
        }

        val opened = Room.databaseBuilder(context, AppDatabase::class.java, NAME)
            .fallbackToDestructiveMigration()
            .addCallback(ExpenseDatabaseCallback(ResourceDefaultCategoryNames(context), FakeAppLocaleProvider()))
            .allowMainThreadQueries()
            .build()
            .also { database = it }

        val categories = opened.categoryDao().getAllCategories().first()
        assertEquals(DefaultCategories.all.size, categories.size)
        assertTrue(categories.none { it.name == "Old" })
    }

    private fun SQLiteDatabase.use(block: (SQLiteDatabase) -> Unit) {
        try {
            block(this)
        } finally {
            close()
        }
    }

    private companion object {
        const val NAME = "destructive_migration_test.db"
    }
}
