package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.ExportDestination
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.fakes.FakeFileExportRepository
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportTransactionsUseCaseTest {

    private val old = transaction(id = 1, dateMillis = 100L, title = "Old")
    private val mid = transaction(id = 2, dateMillis = 200L, title = "Mid")
    private val new = transaction(id = 3, dateMillis = 300L, title = "New")

    private val files = FakeFileExportRepository()
    private val buildCsv = BuildTransactionsCsvUseCase()
    private val destination = ExportDestination("content://documents/expenses.csv")

    private fun useCase(transactions: FakeTransactionRepository = FakeTransactionRepository(listOf(old, new, mid))) =
        ExportTransactionsUseCase(GetTransactionsUseCase(transactions), buildCsv, files)

    @Test
    fun writesThePeriodsTransactionsAsCsv_toTheChosenPlace() = runTest {
        val result = useCase()(DateRange.ALL, destination)

        assertEquals(Outcome.Success(3), result)
        assertEquals(listOf(destination to buildCsv(listOf(new, mid, old))), files.written)
    }

    @Test
    fun leavesOutTransactionsOutsideTheRange() = runTest {
        val result = useCase()(DateRange(startMillis = 200L, endMillis = 300L), destination)

        assertEquals(Outcome.Success(1), result)
        assertEquals(buildCsv(listOf(mid)), files.written.single().second)
    }

    @Test
    fun aFailedWrite_isReportedAsItIs() = runTest {
        files.fail = true

        val result = useCase()(DateRange.ALL, destination)

        assertEquals(Outcome.Failure(DomainError.FileWriteFailed), result)
        assertTrue(files.written.isEmpty())
    }
}
