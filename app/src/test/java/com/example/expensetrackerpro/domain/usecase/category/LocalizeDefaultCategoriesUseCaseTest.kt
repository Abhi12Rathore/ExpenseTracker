package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryDraft
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeDefaultCategoryNames
import com.example.expensetrackerpro.fakes.HINDI
import com.example.expensetrackerpro.fakes.category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class LocalizeDefaultCategoriesUseCaseTest {

    private fun builtIn(id: Int, key: BuiltInCategory, name: String = FakeDefaultCategoryNames.english.getValue(key)) =
        category(id = id, name = name, type = key.type, icon = key.icon).copy(isCustom = false, builtInKey = key)

    private val food = builtIn(1, BuiltInCategory.FOOD_DINING)
    private val fuel = builtIn(2, BuiltInCategory.FUEL)
    private val salary = builtIn(3, BuiltInCategory.SALARY)
    private val pets = category(id = 4, name = "Pets")

    private val localeProvider = FakeAppLocaleProvider()

    private fun localize(repository: FakeCategoryRepository) =
        LocalizeDefaultCategoriesUseCase(repository, FakeDefaultCategoryNames(), localeProvider)

    private fun FakeCategoryRepository.names() = current.sortedBy { it.id.value }.map { it.name }

    private fun FakeCategoryRepository.category(id: Int): Category = current.single { it.id == CategoryId(id) }

    @Test
    fun builtIns_takeTheNamesOfTheAppsLanguage_andCustomOnesKeepTheirs() = runTest {
        val repository = FakeCategoryRepository(listOf(food, fuel, salary, pets))
        localeProvider.locale = HINDI

        assertEquals(Outcome.Success(Unit), localize(repository)())

        assertEquals(listOf("खाना-पीना", "ईंधन", "सैलरी", "Pets"), repository.names())
        // Still built-in, so the next language is followed too
        assertEquals(BuiltInCategory.FOOD_DINING, repository.category(1).builtInKey)
    }

    @Test
    fun switchingLanguages_backAndForth_followsEachTime() = runTest {
        val repository = FakeCategoryRepository(listOf(food, fuel, salary, pets))
        val localize = localize(repository)

        localeProvider.locale = HINDI
        localize()
        localeProvider.locale = Locale.ENGLISH
        localize()

        assertEquals(listOf("Food & Dining", "Fuel", "Salary", "Pets"), repository.names())

        localeProvider.locale = HINDI
        localize()

        assertEquals(listOf("खाना-पीना", "ईंधन", "सैलरी", "Pets"), repository.names())
    }

    @Test
    fun aLanguageTheAppLacks_namesThemInEnglish() = runTest {
        val repository = FakeCategoryRepository(listOf(builtIn(1, BuiltInCategory.FOOD_DINING, name = "खाना-पीना")))
        localeProvider.locale = Locale.FRENCH

        localize(repository)()

        assertEquals(listOf("Food & Dining"), repository.names())
    }

    @Test
    fun namesThatAreAlreadyRight_areNotWrittenAgain() = runTest {
        val repository = FakeCategoryRepository(listOf(food, fuel, salary, pets))

        assertEquals(Outcome.Success(Unit), localize(repository)())
        assertEquals(0, repository.renameCount)

        localeProvider.locale = HINDI
        localize(repository)()
        localize(repository)()

        assertEquals(1, repository.renameCount)
    }

    @Test
    fun aBuiltInTheUserRenamed_keepsTheirName_inEveryLanguage() = runTest {
        val repository = FakeCategoryRepository(listOf(food, fuel, pets))
        val saveCategory = SaveCategoryUseCase(
            GetCategoriesUseCase(repository),
            ValidateCategoryNameUseCase(),
            AddCategoryUseCase(repository),
            UpdateCategoryUseCase(repository)
        )
        saveCategory(CategoryDraft(id = food.id, name = "Eating out", type = food.type, icon = food.icon))
        assertNull(repository.category(1).builtInKey)

        localeProvider.locale = HINDI
        localize(repository)()

        assertEquals(listOf("Eating out", "ईंधन", "Pets"), repository.names())

        localeProvider.locale = Locale.ENGLISH
        localize(repository)()

        assertEquals(listOf("Eating out", "Fuel", "Pets"), repository.names())
    }

    @Test
    fun aNameACustomCategoryOfTheSameTypeAlreadyHas_leavesThatBuiltInAsItIs() = runTest {
        // Differs from the Hindi name only in case and surrounding space, which is the same name
        val custom = category(id = 9, name = " ईंधन ")
        val repository = FakeCategoryRepository(listOf(food, fuel, custom))
        localeProvider.locale = HINDI

        assertEquals(Outcome.Success(Unit), localize(repository)())

        // Only the colliding one is left; it keeps its key, so it follows once the name is free
        assertEquals(listOf("खाना-पीना", "Fuel", " ईंधन "), repository.names())
        assertEquals(BuiltInCategory.FUEL, repository.category(2).builtInKey)

        repository.deleteCategory(custom)
        localize(repository)()

        assertEquals(listOf("खाना-पीना", "ईंधन"), repository.names())
    }

    @Test
    fun theSameNameInTheOtherType_isNoCollision() = runTest {
        val incomeCustom = category(id = 9, name = "ईंधन", type = TransactionType.INCOME)
        val repository = FakeCategoryRepository(listOf(fuel, incomeCustom))
        localeProvider.locale = HINDI

        localize(repository)()

        assertEquals(listOf("ईंधन", "ईंधन"), repository.names())
    }

    @Test
    fun aBuiltInThatKeepsItsName_stillBlocksAnotherFromTakingIt() = runTest {
        // "Other" can't become "अन्य" (a custom has it), so it stays "Other"; a second built-in
        // whose Hindi name would be "Other" must then stay as it is too
        val other = builtIn(1, BuiltInCategory.OTHER)
        val custom = category(id = 5, name = "अन्य")
        val names = object : DefaultCategoryNames {
            override fun nameOf(category: BuiltInCategory, locale: Locale): String = when (category) {
                BuiltInCategory.OTHER -> "अन्य"
                BuiltInCategory.FUEL -> "Other"
                else -> category.key
            }
        }
        val repository = FakeCategoryRepository(listOf(other, fuel, custom))

        LocalizeDefaultCategoriesUseCase(repository, names, localeProvider)()

        assertEquals(listOf("Other", "Fuel", "अन्य"), repository.names())
    }

    @Test
    fun aStorageFailure_isReported_andNothingIsRenamed() = runTest {
        val repository = FakeCategoryRepository(listOf(food)).apply { saveFailure = DomainError.StorageFailure() }
        localeProvider.locale = HINDI

        assertEquals(Outcome.Failure(DomainError.StorageFailure()), localize(repository)())
        assertEquals(listOf("Food & Dining"), repository.names())
    }
}
