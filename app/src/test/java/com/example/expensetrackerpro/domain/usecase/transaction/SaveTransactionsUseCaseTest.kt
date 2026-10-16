package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Several new transactions saved at once: each checked as the form's Save checks one, then all or none written. */
class SaveTransactionsUseCaseTest {

    private val repository = FakeTransactionRepository()
    private val saveAll = SaveTransactionsUseCase(
        repository,
        ValidateTransactionDraftUseCase(ValidateAmountUseCase()),
        FormatAmountInputUseCase()
    )

    private fun new(title: String, amount: Money = rupees(100), note: String = "") =
        transaction(title = title, amount = amount, note = note).copy(id = TransactionId.UNSAVED)

    @Test
    fun completeTransactions_areWrittenInOneBatch() = runTest {
        val result = saveAll(listOf(new("Swiggy"), new("Uber")))

        assertEquals(Outcome.Success(Unit), result)
        assertEquals(listOf("Swiggy", "Uber"), repository.current.map { it.title })
        assertEquals(1, repository.batchSaveCalls)
    }

    @Test
    fun oneIncomplete_writesNone() = runTest {
        val incomplete = DomainError.InvalidInput(InvalidInputReason.TRANSACTION_INCOMPLETE)

        assertEquals(Outcome.Failure(incomplete), saveAll(listOf(new("Swiggy"), new(" "))))
        assertEquals(Outcome.Failure(incomplete), saveAll(listOf(new("Swiggy"), new("Uber", amount = Money.ZERO))))
        assertEquals(Outcome.Failure(incomplete), saveAll(listOf(new("Swiggy"), new("Uber", note = "x".repeat(201)))))
        // Only new transactions: a stored one is changed through the form
        assertEquals(Outcome.Failure(incomplete), saveAll(listOf(new("Swiggy").copy(id = TransactionId(7)))))
        assertEquals(emptyList<Any>(), repository.current)
        assertEquals(0, repository.batchSaveCalls)
    }

    @Test
    fun aStorageFailure_isPassedOn() = runTest {
        repository.failWrites = true

        assertEquals(Outcome.Failure(DomainError.StorageFailure()), saveAll(listOf(new("Swiggy"), new("Uber"))))
    }

    @Test
    fun nothingToSave_succeedsWithoutWriting() = runTest {
        assertEquals(Outcome.Success(Unit), saveAll(emptyList()))
        assertEquals(0, repository.batchSaveCalls)
    }
}
