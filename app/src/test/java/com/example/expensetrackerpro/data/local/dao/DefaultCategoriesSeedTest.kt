package com.example.expensetrackerpro.data.local.dao

import java.util.Locale
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.data.category.ResourceDefaultCategoryNames
import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.data.mapper.toDomain
import com.example.expensetrackerpro.domain.models.CategoryId
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
        ).allowMainThreadQueries().addCallback(ExpenseDatabaseCallback(ResourceDefaultCategoryNames(ApplicationProvider.getApplicationContext()), FakeAppLocaleProvider())).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aNewDatabase_holdsTheDefaults_withTheirKeysIconsAndTypes_namedInTheAppsLanguage() = runTest {
        val seeded = database.categoryDao().getAllCategories().first().map { it.toDomain() }

        // Ids are assigned in order, starting at 1
        val names = ResourceDefaultCategoryNames(ApplicationProvider.getApplicationContext())
        val expected = DefaultCategories.all.mapIndexed { index, builtIn ->
            Category(
                id = CategoryId(index + 1),
                name = names.nameOf(builtIn, Locale.ENGLISH),
                type = builtIn.type,
                icon = builtIn.icon,
                isCustom = false,
                builtInKey = builtIn
            )
        }
        assertEquals("Food & Dining", expected.first().name)
        assertEquals(expected, seeded)
    }
}
