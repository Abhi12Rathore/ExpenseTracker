package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.category
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateCategoryNameUseCaseTest {

    private val validate = ValidateCategoryNameUseCase()

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE)
    private val salary = category(id = 2, name = "Salary", type = TransactionType.INCOME)
    private val existing = listOf(food, salary)

    private fun expense(name: String, editingId: CategoryId? = null) =
        validate(name, TransactionType.EXPENSE, editingId, existing)

    @Test
    fun aNewName_isValid() {
        assertEquals(CategoryNameValidation.VALID, expense("Gym"))
    }

    @Test
    fun emptyOrOnlySpaces_isBlank() {
        assertEquals(CategoryNameValidation.BLANK, expense(""))
        assertEquals(CategoryNameValidation.BLANK, expense("   "))
    }

    @Test
    fun aNameOfTheSameType_isADuplicate_whateverItsCaseOrSpacing() {
        assertEquals(CategoryNameValidation.DUPLICATE, expense("Food"))
        assertEquals(CategoryNameValidation.DUPLICATE, expense("food"))
        assertEquals(CategoryNameValidation.DUPLICATE, expense("  FOOD "))
    }

    @Test
    fun theSameNameInTheOtherType_isAllowed() {
        assertEquals(CategoryNameValidation.VALID, expense("Salary"))
        assertEquals(
            CategoryNameValidation.VALID,
            validate("Food", TransactionType.INCOME, null, existing)
        )
    }

    @Test
    fun theCategoryBeingEdited_mayKeepItsOwnName_butNotTakeAnothers() {
        val bills = category(id = 3, name = "Bills", type = TransactionType.EXPENSE)
        val all = existing + bills

        assertEquals(CategoryNameValidation.VALID, validate("Food", TransactionType.EXPENSE, food.id, all))
        assertEquals(CategoryNameValidation.VALID, validate("FOOD", TransactionType.EXPENSE, food.id, all))
        assertEquals(CategoryNameValidation.DUPLICATE, validate("Bills", TransactionType.EXPENSE, food.id, all))
    }

    @Test
    fun thirtyCharactersFit_thirtyOneAreTooLong() {
        assertEquals(CategoryNameValidation.VALID, expense("a".repeat(30)))
        assertEquals(CategoryNameValidation.TOO_LONG, expense("a".repeat(31)))
    }

    @Test
    fun theLength_isCountedWithoutTheSpaceAround() {
        assertEquals(CategoryNameValidation.VALID, expense("  " + "a".repeat(30) + "  "))
    }
}
