package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.CategoryDraft
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.SaveCategoryResult
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveCategoryUseCaseTest {

    // A built-in category and one the user added
    private val food = category(id = 1, name = "Food", icon = CategoryIcon.RESTAURANT).copy(isCustom = false)
    private val gym = category(id = 2, name = "Gym", icon = CategoryIcon.FITNESS_CENTER)
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

    private val repository = FakeCategoryRepository(listOf(food, gym, salary))

    private val saveCategory = SaveCategoryUseCase(
        GetCategoriesUseCase(repository),
        ValidateCategoryNameUseCase(),
        AddCategoryUseCase(repository),
        UpdateCategoryUseCase(repository)
    )

    private fun draft(
        name: String,
        id: Int? = null,
        type: TransactionType = TransactionType.EXPENSE,
        icon: CategoryIcon = CategoryIcon.PETS
    ) = CategoryDraft(id = id, name = name, type = type, icon = icon)

    @Test
    fun aNewCategory_isStoredTrimmedAsCustom_andItsIdReturned() = runTest {
        val result = saveCategory(draft("  Pets  "))

        val stored = repository.current.single { it.name == "Pets" }
        assertEquals(SaveCategoryResult.Saved(stored.id), result)
        assertEquals(TransactionType.EXPENSE, stored.type)
        assertEquals(CategoryIcon.PETS, stored.icon)
        assertTrue(stored.isCustom)
    }

    @Test
    fun aDuplicateOrBlankName_isRefusedAndNothingIsStored() = runTest {
        assertEquals(
            SaveCategoryResult.Invalid(CategoryNameValidation.DUPLICATE),
            saveCategory(draft("food"))
        )
        assertEquals(
            SaveCategoryResult.Invalid(CategoryNameValidation.BLANK),
            saveCategory(draft("  "))
        )
        assertEquals(
            SaveCategoryResult.Invalid(CategoryNameValidation.TOO_LONG),
            saveCategory(draft("a".repeat(31)))
        )
        assertEquals(listOf(food, gym, salary), repository.current)
    }

    @Test
    fun theSameNameInTheOtherType_isStored() = runTest {
        val result = saveCategory(draft("Food", type = TransactionType.INCOME))

        assertTrue(result is SaveCategoryResult.Saved)
        assertEquals(2, repository.current.count { it.name == "Food" })
    }

    @Test
    fun editing_changesNameAndIcon_underTheSameId() = runTest {
        val result = saveCategory(draft("Fitness", id = gym.id, icon = CategoryIcon.SPA))

        assertEquals(SaveCategoryResult.Saved(gym.id), result)
        assertEquals(
            gym.copy(name = "Fitness", icon = CategoryIcon.SPA),
            repository.current.single { it.id == gym.id }
        )
        assertEquals(3, repository.current.size)
    }

    @Test
    fun editingABuiltInCategory_keepsItBuiltIn() = runTest {
        saveCategory(draft("Eating out", id = food.id))

        val stored = repository.current.single { it.id == food.id }
        assertEquals("Eating out", stored.name)
        assertFalse(stored.isCustom)
    }

    @Test
    fun editing_keepsTheStoredType_whateverTheDraftSays() = runTest {
        saveCategory(draft("Gym", id = gym.id, type = TransactionType.INCOME))

        assertEquals(TransactionType.EXPENSE, repository.current.single { it.id == gym.id }.type)
    }

    @Test
    fun editing_mayKeepItsOwnName_butNotTakeAnothers() = runTest {
        assertEquals(SaveCategoryResult.Saved(gym.id), saveCategory(draft("gym", id = gym.id)))
        assertEquals(
            SaveCategoryResult.Invalid(CategoryNameValidation.DUPLICATE),
            saveCategory(draft("Food", id = gym.id))
        )
    }

    @Test
    fun editingACategoryThatIsGone_reportsItAndStoresNothing() = runTest {
        assertEquals(SaveCategoryResult.NotFound, saveCategory(draft("Ghost", id = 99)))
        assertEquals(3, repository.current.size)
    }
}
