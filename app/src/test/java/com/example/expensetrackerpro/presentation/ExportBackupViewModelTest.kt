package com.example.expensetrackerpro.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.ExportDestination
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.usecase.export.BuildTransactionsCsvUseCase
import com.example.expensetrackerpro.domain.usecase.export.ExportTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.export.GetExportFileNameUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.fakes.FakeFileExportRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.KOLKATA
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.exportBackup.ExportBackupViewModel
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupEvent
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupMessage
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportPeriod
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExportBackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val originalTimeZone = TimeZone.getDefault()

    // Sunday 11 Oct 2026, 4:20 pm
    private val time = FakeTimeProvider(millisAt(2026, Calendar.OCTOBER, 11, hour = 16, minute = 20))

    private val lastYear = transaction(id = 1, dateMillis = millisAt(2025, Calendar.DECEMBER, 30), title = "Last year")
    private val march = transaction(id = 2, dateMillis = millisAt(2026, Calendar.MARCH, 14), title = "March")
    private val october1 = transaction(id = 3, dateMillis = millisAt(2026, Calendar.OCTOBER, 1), title = "Oct 1")
    private val october10 = transaction(id = 4, dateMillis = millisAt(2026, Calendar.OCTOBER, 10), title = "Oct 10")

    private val transactions = FakeTransactionRepository(listOf(march, october10, lastYear, october1))
    private val files = FakeFileExportRepository()
    private val buildCsv = BuildTransactionsCsvUseCase()

    private val october = DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val year2026 = DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))

    private val uri = "content://documents/expenses.csv"

    @Before
    fun setUp() {
        // The file name's date is formatted in the default zone
        TimeZone.setDefault(KOLKATA)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): ExportBackupViewModel {
        val getTransactions = GetTransactionsUseCase(transactions)

        return ExportBackupViewModel(
            getTransactions,
            GetCurrentPeriodUseCase(time),
            GetDateRangeUseCase(time),
            GetCustomDateRangeUseCase(time),
            ExportTransactionsUseCase(getTransactions, buildCsv, files),
            GetExportFileNameUseCase(time),
            savedStateHandle
        )
    }

    /** A date as a date picker reports it: midnight UTC. */
    private fun pickerDay(year: Int, month: Int, day: Int): Long =
        millisAt(year, month, day, timeZone = TimeZone.getTimeZone("UTC"))

    private fun writtenCsv(): String = files.written.single().second

    private fun csvOf(vararg rows: Transaction): String = buildCsv(rows.toList())

    @Test
    fun startsOnTheCurrentMonth_withWhatItHolds() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        val state = viewModel.uiState.value
        assertEquals(ExportPeriod.MONTH, state.period)
        assertEquals(october, state.dateRange)
        assertEquals(2, state.transactionCount)
        assertEquals(october1.dateMillis, state.oldestMillis)
        assertEquals(october10.dateMillis, state.newestMillis)
        assertTrue(state.canExport)
        assertFalse(state.isExporting)
        assertNull(state.message)
    }

    @Test
    fun beforeAnythingIsLoaded_thePeriodIsAlreadyTheCurrentMonth() {
        val state = viewModel().uiState.value

        assertEquals(ExportPeriod.MONTH, state.period)
        assertEquals(october, state.dateRange)
        assertFalse(state.canExport)
    }

    @Test
    fun suggestsAFileNamedAfterToday() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals("expenses_2026-10-11.csv", viewModel.uiState.value.suggestedFileName)
    }

    @Test
    fun yearSelected_coversTheCurrentYear() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.YEAR))

        val state = viewModel.uiState.value
        assertEquals(ExportPeriod.YEAR, state.period)
        assertEquals(year2026, state.dateRange)
        assertEquals(3, state.transactionCount)
        assertEquals(march.dateMillis, state.oldestMillis)
        assertEquals(october10.dateMillis, state.newestMillis)
    }

    @Test
    fun allTimeSelected_coversEverything() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.ALL_TIME))

        val state = viewModel.uiState.value
        assertEquals(ExportPeriod.ALL_TIME, state.period)
        assertEquals(DateRange.ALL, state.dateRange)
        assertEquals(4, state.transactionCount)
        assertEquals(lastYear.dateMillis, state.oldestMillis)
        assertEquals(october10.dateMillis, state.newestMillis)
    }

    @Test
    fun monthSelected_goesBackToTheCurrentMonth() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.ALL_TIME))

        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.MONTH))

        assertEquals(ExportPeriod.MONTH, viewModel.uiState.value.period)
        assertEquals(october, viewModel.uiState.value.dateRange)
        assertEquals(2, viewModel.uiState.value.transactionCount)
    }

    @Test
    fun customRangeSelected_coversBothDaysInFull() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(
            ExportBackupEvent.CustomRangeSelected(
                pickerDay(2025, Calendar.DECEMBER, 30),
                pickerDay(2026, Calendar.MARCH, 14)
            )
        )

        val state = viewModel.uiState.value
        assertEquals(ExportPeriod.CUSTOM, state.period)
        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 30), millisAt(2026, Calendar.MARCH, 15)),
            state.dateRange
        )
        assertEquals(2, state.transactionCount)
    }

    @Test
    fun customSelectedWithoutDates_changesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.YEAR))

        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.CUSTOM))

        assertEquals(ExportPeriod.YEAR, viewModel.uiState.value.period)
        assertEquals(year2026, viewModel.uiState.value.dateRange)
    }

    @Test
    fun aPeriodWithNothingInIt_cannotBeExported() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(
            ExportBackupEvent.CustomRangeSelected(
                pickerDay(2026, Calendar.JULY, 1),
                pickerDay(2026, Calendar.JULY, 31)
            )
        )

        val state = viewModel.uiState.value
        assertEquals(0, state.transactionCount)
        assertNull(state.oldestMillis)
        assertNull(state.newestMillis)
        assertFalse(state.canExport)
    }

    @Test
    fun aPickForAPeriodWithNothingInIt_writesTheHeaderAlone() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(
            ExportBackupEvent.CustomRangeSelected(
                pickerDay(2026, Calendar.JULY, 1),
                pickerDay(2026, Calendar.JULY, 31)
            )
        )

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(csvOf(), writtenCsv())
        assertEquals(ExportBackupMessage.Exported(0), viewModel.uiState.value.message)
    }

    /** A new ViewModel on the state [select] left behind, as after the process was killed. */
    private fun restoredAfter(select: (ExportBackupViewModel) -> Unit): ExportBackupViewModel {
        val savedState = SavedStateHandle()
        select(viewModel(savedState))

        return viewModel(savedState)
    }

    @Test
    fun afterProcessDeath_comesBackOnTheSelectedPeriod() = runTest {
        val allTime = restoredAfter { it.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.ALL_TIME)) }
        val year = restoredAfter { it.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.YEAR)) }
        val month = restoredAfter {
            it.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.YEAR))
            it.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.MONTH))
        }

        // Read before anything is loaded, as the screen first draws it
        assertEquals(ExportPeriod.ALL_TIME, allTime.uiState.value.period)
        assertEquals(DateRange.ALL, allTime.uiState.value.dateRange)
        assertEquals(ExportPeriod.YEAR, year.uiState.value.period)
        assertEquals(year2026, year.uiState.value.dateRange)
        assertEquals(ExportPeriod.MONTH, month.uiState.value.period)
        assertEquals(october, month.uiState.value.dateRange)
    }

    @Test
    fun afterProcessDeath_comesBackOnTheCustomRange() = runTest {
        val viewModel = restoredAfter {
            it.onEvent(
                ExportBackupEvent.CustomRangeSelected(
                    pickerDay(2025, Calendar.DECEMBER, 30),
                    pickerDay(2026, Calendar.MARCH, 14)
                )
            )
        }
        subscribe(viewModel.uiState)

        val state = viewModel.uiState.value
        assertEquals(ExportPeriod.CUSTOM, state.period)
        assertEquals(
            DateRange(millisAt(2025, Calendar.DECEMBER, 30), millisAt(2026, Calendar.MARCH, 15)),
            state.dateRange
        )
        assertEquals(2, state.transactionCount)
    }

    @Test
    fun afterProcessDeath_aCustomRangeIsForgotten_onceAnotherPeriodIsChosen() = runTest {
        val viewModel = restoredAfter {
            it.onEvent(
                ExportBackupEvent.CustomRangeSelected(
                    pickerDay(2026, Calendar.MARCH, 1),
                    pickerDay(2026, Calendar.MARCH, 31)
                )
            )
            it.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.ALL_TIME))
        }

        assertEquals(ExportPeriod.ALL_TIME, viewModel.uiState.value.period)
        assertEquals(DateRange.ALL, viewModel.uiState.value.dateRange)
    }

    @Test
    fun afterProcessDeath_aPickThatArrivesBeforeAnythingIsLoaded_exportsTheSelectedPeriod() = runTest {
        val viewModel = restoredAfter { it.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.YEAR)) }
        // Nothing collects the state yet, so the count is still 0
        assertEquals(0, viewModel.uiState.value.transactionCount)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(ExportDestination(uri), files.written.single().first)
        assertEquals(csvOf(october10, october1, march), writtenCsv())
        subscribe(viewModel.uiState)
        assertEquals(ExportBackupMessage.Exported(3), viewModel.uiState.value.message)
    }

    @Test
    fun theCount_followsTransactionsAsTheyChange() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        transactions.deleteTransaction(october1)

        assertEquals(1, viewModel.uiState.value.transactionCount)
        assertEquals(october10.dateMillis, viewModel.uiState.value.oldestMillis)
    }

    @Test
    fun destinationPicked_writesTheSelectedPeriod_andSaysHowMany() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(ExportDestination(uri), files.written.single().first)
        assertEquals(csvOf(october10, october1), writtenCsv())
        assertEquals(ExportBackupMessage.Exported(2), viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun destinationPicked_afterChangingThePeriod_writesThatPeriod() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.ALL_TIME))

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(csvOf(october10, october1, march, lastYear), writtenCsv())
        assertEquals(ExportBackupMessage.Exported(4), viewModel.uiState.value.message)
    }

    @Test
    fun aFailedWrite_showsWhy() = runTest {
        files.fail = true
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(
            ExportBackupMessage.Failed(R.string.error_file_write_failed),
            viewModel.uiState.value.message
        )
        assertFalse(viewModel.uiState.value.isExporting)
        assertTrue(viewModel.uiState.value.canExport)
    }

    @Test
    fun messageShown_clearsTheMessage() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        viewModel.onEvent(ExportBackupEvent.MessageShown)

        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun whileTheFileIsBeingWritten_theScreenIsExporting_andASecondPickIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        files.writeGate = gate
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertTrue(viewModel.uiState.value.isExporting)
        assertFalse(viewModel.uiState.value.canExport)
        assertNull(viewModel.uiState.value.message)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked("content://documents/other.csv"))
        gate.complete(Unit)

        assertEquals(1, files.writeCalls)
        assertEquals(ExportDestination(uri), files.written.single().first)
        assertFalse(viewModel.uiState.value.isExporting)
        assertEquals(ExportBackupMessage.Exported(2), viewModel.uiState.value.message)
    }

    @Test
    fun afterAnExport_anotherOneCanBeMade() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))
        viewModel.onEvent(ExportBackupEvent.MessageShown)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(2, files.written.size)
        assertEquals(ExportBackupMessage.Exported(2), viewModel.uiState.value.message)
    }
}
