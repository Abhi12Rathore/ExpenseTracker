package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.category
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReassignAndDeleteCategoryUseCaseTest {

    private val food = category(id = 1, name = "Food")
    private val groceries = category(id = 2, name = "Groceries")
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)

    private val repository = FakeCategoryRepository(listOf(food, groceries, salary)).apply {
        setUsage(food.id, totalCount = 7)
        setUsage(groceries.id, totalCount = 2)
    }

    private val reassignAndDelete = ReassignAndDeleteCategoryUseCase(repository)

    private val incompatibleTarget =
        Outcome.Failure(DomainError.InvalidInput(InvalidInputReason.INCOMPATIBLE_REASSIGN_TARGET))

    @Test
    fun movesTheTransactionsToTheTarget_thenDeletesTheCategory() = runTest {
        assertEquals(Outcome.Success(Unit), reassignAndDelete(food, groceries))

        assertEquals(listOf(groceries, salary), repository.current)
        assertEquals(9, repository.totalCountOf(groceries.id))
    }

    @Test
    fun aTargetOfAnotherType_isRefusedAndNothingChanges() = runTest {
        assertEquals(incompatibleTarget, reassignAndDelete(food, salary))

        assertEquals(listOf(food, groceries, salary), repository.current)
        assertEquals(7, repository.totalCountOf(food.id))
    }

    @Test
    fun theCategoryItself_isNotATarget() = runTest {
        assertEquals(incompatibleTarget, reassignAndDelete(food, food))

        assertEquals(3, repository.current.size)
    }

    @Test
    fun aFailingDatabase_isReportedAndNothingChanges() = runTest {
        repository.failReassign = true

        assertEquals(Outcome.Failure(DomainError.StorageFailure()), reassignAndDelete(food, groceries))

        assertEquals(listOf(food, groceries, salary), repository.current)
        assertEquals(7, repository.totalCountOf(food.id))
    }

    @Test
    fun aTargetThatIsGone_isReportedAsNotFound() = runTest {
        val missing = category(id = 99, name = "Ghost")

        assertEquals(Outcome.Failure(DomainError.NotFound), reassignAndDelete(food, missing))

        assertEquals(listOf(food, groceries, salary), repository.current)
    }
}
