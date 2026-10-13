package com.example.expensetrackerpro.data.category

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.data.repository.CategoryRepositoryImpl
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.LocalizeDefaultCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.HINDI
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** The built-in names as the app's own string resources give them, and the rename against a real database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ResourceDefaultCategoryNamesTest {

    private val names = ResourceDefaultCategoryNames(ApplicationProvider.getApplicationContext())

    @Test
    fun aName_comesFromTheLanguageAskedFor_whateverTheAppIsShownIn() {
        assertEquals("Food & Dining", names.nameOf(BuiltInCategory.FOOD_DINING, Locale.ENGLISH))
        assertEquals("खाना-पीना", names.nameOf(BuiltInCategory.FOOD_DINING, HINDI))
        assertEquals("खाना-पीना", names.nameOf(BuiltInCategory.FOOD_DINING, Locale.forLanguageTag("hi-IN")))
        // Asking for one language does not change what the next call gets
        assertEquals("Fuel", names.nameOf(BuiltInCategory.FUEL, Locale.forLanguageTag("en-IN")))
        assertEquals("EMI और लोन", names.nameOf(BuiltInCategory.EMI_LOANS, HINDI))
        assertEquals("Supermercado", names.nameOf(BuiltInCategory.GROCERIES, Locale.forLanguageTag("es-MX")))
        assertEquals("Courses", names.nameOf(BuiltInCategory.GROCERIES, Locale.FRANCE))
        assertEquals("Lebensmittel", names.nameOf(BuiltInCategory.GROCERIES, Locale.GERMANY))
        assertEquals("Mercado", names.nameOf(BuiltInCategory.GROCERIES, Locale.forLanguageTag("pt-BR")))
        // The app's Portuguese is Brazil's, and Portugal gets it too
        assertEquals("Mercado", names.nameOf(BuiltInCategory.GROCERIES, Locale.forLanguageTag("pt-PT")))
    }

    @Test
    fun aLanguageTheAppLacks_getsTheEnglishName() {
        assertEquals("Salary", names.nameOf(BuiltInCategory.SALARY, Locale.JAPANESE))
    }

    @Test
    fun everyBuiltIn_isTranslated_andNoTwoOfATypeShareAName() {
        val locales = AppLanguage.entries.map { Locale.forLanguageTag(it.tag) }
        locales.forEach { locale ->
            TransactionType.entries.forEach { type ->
                val ofType = BuiltInCategory.entries.filter { it.type == type }.map { names.nameOf(it, locale).lowercase() }

                assertEquals("$locale $type", ofType.size, ofType.toSet().size)
            }
        }
        BuiltInCategory.entries.forEach { builtIn ->
            // EMI is the only word the two languages share, and it is never the whole name
            assertNotEquals(builtIn.key, names.nameOf(builtIn, Locale.ENGLISH), names.nameOf(builtIn, HINDI))
            // No name runs past what a category may be called, or it could not be saved again unchanged
            locales.forEach { locale ->
                val name = names.nameOf(builtIn, locale)
                assertTrue("$locale ${builtIn.key}: $name", name.length <= ValidateCategoryNameUseCase.NAME_MAX_LENGTH)
            }
        }
    }

    @Test
    fun aSeededDatabase_isRenamedIntoHindi_exceptWhatTheUserRenamed() = runTest {
        val localeProvider = FakeAppLocaleProvider()
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .addCallback(ExpenseDatabaseCallback(names, localeProvider))
            .build()
        val repository = CategoryRepositoryImpl(database.categoryDao())
        val localize = LocalizeDefaultCategoriesUseCase(repository, names, localeProvider)

        suspend fun category(builtIn: BuiltInCategory): Category =
            repository.getAllCategories().first().single { it.id == CategoryId(builtIn.ordinal + 1) }

        // As the editor saves a rename: the key goes with the old name
        repository.updateCategory(category(BuiltInCategory.TRANSPORT).copy(name = "Office cab", builtInKey = null))

        localeProvider.locale = HINDI
        assertEquals(Outcome.Success(Unit), localize())

        assertEquals("खाना-पीना", category(BuiltInCategory.FOOD_DINING).name)
        assertEquals(BuiltInCategory.FOOD_DINING, category(BuiltInCategory.FOOD_DINING).builtInKey)
        assertEquals("सैलरी", category(BuiltInCategory.SALARY).name)
        assertEquals("Office cab", category(BuiltInCategory.TRANSPORT).name)
        assertNull(category(BuiltInCategory.TRANSPORT).builtInKey)
        assertTrue(repository.getAllCategories().first().none { it.isCustom })

        localeProvider.locale = Locale.ENGLISH
        localize()

        assertEquals("Food & Dining", category(BuiltInCategory.FOOD_DINING).name)
        assertEquals("Office cab", category(BuiltInCategory.TRANSPORT).name)
        database.close()
    }
}
