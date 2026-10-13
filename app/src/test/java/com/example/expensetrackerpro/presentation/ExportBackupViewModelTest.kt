package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.fakes.dateFormatter
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.localizeDefaultCategories
import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.getOrNull
import com.example.expensetrackerpro.domain.models.BackupPreview
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.LastBackup
import com.example.expensetrackerpro.domain.usecase.backup.CreateBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.ObserveLastBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.PreviewBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.ReadBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.RestoreBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.ValidateBackupUseCase
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.ObserveTodayUseCase
import com.example.expensetrackerpro.fakes.FakeBackupInfoRepository
import com.example.expensetrackerpro.fakes.FakeBackupRepository
import com.example.expensetrackerpro.fakes.backupDocument
import com.example.expensetrackerpro.fakes.transactionRecord
import com.example.expensetrackerpro.presentation.exportBackup.BackupController
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DocumentUri
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
    private val buildCsv = BuildTransactionsCsvUseCase(dateFormatter())

    private val october = DateRange(millisAt(2026, Calendar.OCTOBER, 1), millisAt(2026, Calendar.NOVEMBER, 1))
    private val year2026 = DateRange(millisAt(2026, Calendar.JANUARY, 1), millisAt(2027, Calendar.JANUARY, 1))

    private val uri = "content://documents/expenses.csv"

    private val backups = FakeBackupRepository()
    private val backupInfo = FakeBackupInfoRepository()
    private val backupUri = "content://documents/expensetracker-backup.json"

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
            GetExportFileNameUseCase(time, dateFormatter()),
            backupController(getTransactions),
            savedStateHandle
        )
    }

    private fun backupController(getTransactions: GetTransactionsUseCase): BackupController {
        val readBackup = ReadBackupUseCase(backups, ValidateBackupUseCase(ValidateCategoryNameUseCase()))

        return BackupController(
            CreateBackupUseCase(backups, backupInfo, time, BuildInfo(isDebug = false, versionName = "1.0")),
            PreviewBackupUseCase(readBackup, getTransactions),
            RestoreBackupUseCase(readBackup, backups, localizeDefaultCategories(FakeCategoryRepository())),
            ObserveLastBackupUseCase(backupInfo, ObserveTodayUseCase(time), time)
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
        assertEquals(ExportBackupMessage.Exported(0, uri), viewModel.uiState.value.message)
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

        assertEquals(DocumentUri(uri), files.written.single().first)
        assertEquals(csvOf(october10, october1, march), writtenCsv())
        subscribe(viewModel.uiState)
        assertEquals(ExportBackupMessage.Exported(3, uri), viewModel.uiState.value.message)
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

        assertEquals(DocumentUri(uri), files.written.single().first)
        assertEquals(csvOf(october10, october1), writtenCsv())
        assertEquals(ExportBackupMessage.Exported(2, uri), viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isExporting)
    }

    @Test
    fun destinationPicked_afterChangingThePeriod_writesThatPeriod() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.PeriodSelected(ExportPeriod.ALL_TIME))

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(csvOf(october10, october1, march, lastYear), writtenCsv())
        assertEquals(ExportBackupMessage.Exported(4, uri), viewModel.uiState.value.message)
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
        assertEquals(DocumentUri(uri), files.written.single().first)
        assertFalse(viewModel.uiState.value.isExporting)
        assertEquals(ExportBackupMessage.Exported(2, uri), viewModel.uiState.value.message)
    }

    @Test
    fun afterAnExport_anotherOneCanBeMade() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))
        viewModel.onEvent(ExportBackupEvent.MessageShown)

        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        assertEquals(2, files.written.size)
        assertEquals(ExportBackupMessage.Exported(2, uri), viewModel.uiState.value.message)
    }

    // --- Backup ---

    @Test
    fun suggestsABackupFileNamedAfterToday() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals("expensetracker-backup-2026-10-11.json", viewModel.uiState.value.suggestedBackupFileName)
    }

    @Test
    fun beforeAnyBackup_thereIsNoLastBackup() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertNull(viewModel.uiState.value.backup.lastBackup)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun backupDestinationPicked_savesTheBackup_andShowsItAsTheLastOne() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked(backupUri))

        val written = backups.written.single()
        assertEquals(DocumentUri(backupUri), written.destination)
        assertEquals(time.nowMillis, written.createdAtMillis)
        assertEquals("1.0", written.appVersion)
        val state = viewModel.uiState.value
        assertEquals(ExportBackupMessage.BackupSaved(backupUri), state.pendingMessage)
        assertEquals(LastBackup(time.nowMillis, DayLabel.TODAY), state.backup.lastBackup)
        assertFalse(state.isBusy)
    }

    @Test
    fun aBackupMadeEarlier_isShownWithItsDay() = runTest {
        backupInfo.setLastBackupMillis(millisAt(2026, Calendar.OCTOBER, 10, hour = 9))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(DayLabel.YESTERDAY, viewModel.uiState.value.backup.lastBackup?.day)
    }

    @Test
    fun aBackupThatCantBeWritten_showsWhy_andIsNotTheLastBackup() = runTest {
        backups.failWrites = true
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked(backupUri))

        val state = viewModel.uiState.value
        assertEquals(ExportBackupMessage.Failed(R.string.error_file_write_failed), state.pendingMessage)
        assertNull(state.backup.lastBackup)
        assertFalse(state.isBusy)
    }

    @Test
    fun whileABackupIsBeingWritten_nothingElseStarts() = runTest {
        val gate = CompletableDeferred<Unit>()
        backups.gate = gate
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked(backupUri))

        assertTrue(viewModel.uiState.value.backup.isCreating)
        assertTrue(viewModel.uiState.value.isBusy)
        assertFalse(viewModel.uiState.value.canExport)

        viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked("content://documents/second.json"))
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))
        gate.complete(Unit)

        assertEquals(DocumentUri(backupUri), backups.written.single().destination)
        assertEquals(0, backups.readCalls)
        assertEquals(0, files.writeCalls)
        assertNull(viewModel.uiState.value.backup.restorePreview)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun whileAnExportIsBeingWritten_noBackupOrRestoreStarts() = runTest {
        val gate = CompletableDeferred<Unit>()
        files.writeGate = gate
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))

        viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked(backupUri))
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
        gate.complete(Unit)

        assertTrue(backups.written.isEmpty())
        assertEquals(0, backups.readCalls)
        assertEquals(ExportBackupMessage.Exported(2, uri), viewModel.uiState.value.pendingMessage)
    }

    @Test
    fun messageShown_clearsOneMessageAtATime_theExportsFirst() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked(backupUri))
        viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri))
        assertEquals(ExportBackupMessage.Exported(2, uri), viewModel.uiState.value.pendingMessage)

        viewModel.onEvent(ExportBackupEvent.MessageShown)
        assertEquals(ExportBackupMessage.BackupSaved(backupUri), viewModel.uiState.value.pendingMessage)

        viewModel.onEvent(ExportBackupEvent.MessageShown)
        assertNull(viewModel.uiState.value.pendingMessage)
    }

    // --- Restore ---

    private val expectedPreview = BackupPreview(
        fileName = "backup.json",
        createdAtMillis = 1_000L,
        transactionCount = 3,
        customCategoryCount = 1,
        oldestMillis = 100L,
        newestMillis = 300L,
        // Everything in the app, whatever period the export card is on
        currentTransactionCount = 4
    )

    private val expectedBackup = ValidateBackupUseCase(ValidateCategoryNameUseCase())(backupDocument()).getOrNull()

    @Test
    fun restoreFilePicked_showsWhatTheFileHolds_andChangesNothing() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))

        val state = viewModel.uiState.value
        assertEquals(expectedPreview, state.backup.restorePreview)
        assertNull(state.pendingMessage)
        assertFalse(state.isBusy)
        assertTrue(backups.restored.isEmpty())
    }

    @Test
    fun whileThePickedFileIsBeingRead_theScreenIsBusy() = runTest {
        val gate = CompletableDeferred<Unit>()
        backups.gate = gate
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))

        assertTrue(viewModel.uiState.value.backup.isReading)
        assertNull(viewModel.uiState.value.backup.restorePreview)

        gate.complete(Unit)

        assertFalse(viewModel.uiState.value.backup.isReading)
        assertEquals(expectedPreview, viewModel.uiState.value.backup.restorePreview)
    }

    @Test
    fun aPickedFileThatCantBeRestored_showsWhy_andNoSheet() = runTest {
        val unreadable = "content://documents/gone.json"
        val tooNew = "content://documents/newer.json"
        val notABackup = "content://documents/expenses.csv"
        val inconsistent = "content://documents/edited.json"
        backups.files[tooNew] = Outcome.Failure(DomainError.BackupTooNew)
        backups.files[notABackup] = Outcome.Failure(DomainError.BackupInvalid)
        backups.putFile(inconsistent, backupDocument(transactions = listOf(transactionRecord(categoryId = 99))))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        mapOf(
            unreadable to R.string.error_file_read_failed,
            tooNew to R.string.error_backup_too_new,
            notABackup to R.string.error_backup_invalid,
            inconsistent to R.string.error_backup_invalid
        ).forEach { (picked, messageRes) ->
            viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(picked))

            val state = viewModel.uiState.value
            assertEquals(picked, ExportBackupMessage.Failed(messageRes), state.pendingMessage)
            assertNull(picked, state.backup.restorePreview)
            assertFalse(picked, state.isBusy)

            // The file is forgotten: confirming now restores nothing
            viewModel.onEvent(ExportBackupEvent.MessageShown)
            viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)
        }
        assertTrue(backups.restored.isEmpty())
    }

    @Test
    fun restoreConfirmed_readsTheFileAgain_restoresIt_andSaysHowMuch() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertEquals(2, backups.readCalls)
        assertEquals(expectedBackup, backups.restored.single())
        val state = viewModel.uiState.value
        assertEquals(ExportBackupMessage.Restored(3), state.pendingMessage)
        assertNull(state.backup.restorePreview)
        assertFalse(state.isBusy)
    }

    @Test
    fun restoreConfirmed_whenTheFileHasChangedSinceThePreview_restoresNothing() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
        backups.putFile(backupUri, backupDocument(transactions = listOf(transactionRecord(amountPaise = 0))))

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertTrue(backups.restored.isEmpty())
        val state = viewModel.uiState.value
        assertEquals(ExportBackupMessage.Failed(R.string.error_backup_invalid), state.pendingMessage)
        assertNull(state.backup.restorePreview)
    }

    @Test
    fun restoreConfirmed_whenTheFileIsGone_restoresNothing() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
        backups.files.remove(backupUri)

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertTrue(backups.restored.isEmpty())
        assertEquals(
            ExportBackupMessage.Failed(R.string.error_file_read_failed),
            viewModel.uiState.value.pendingMessage
        )
    }

    @Test
    fun aRestoreTheDatabaseTurnsDown_showsWhy() = runTest {
        backups.putFile(backupUri, backupDocument())
        backups.failRestores = true
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        val state = viewModel.uiState.value
        assertEquals(ExportBackupMessage.Failed(R.string.error_storage_failure), state.pendingMessage)
        assertNull(state.backup.restorePreview)
        assertFalse(state.isBusy)
    }

    @Test
    fun restoreDismissed_closesTheSheet_andForgetsTheFile() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))

        viewModel.onEvent(ExportBackupEvent.RestoreDismissed)
        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertNull(viewModel.uiState.value.backup.restorePreview)
        assertNull(viewModel.uiState.value.pendingMessage)
        assertTrue(backups.restored.isEmpty())
        assertEquals(1, backups.readCalls)
    }

    @Test
    fun restoreConfirmed_withoutAFile_doesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertEquals(0, backups.readCalls)
        assertNull(viewModel.uiState.value.pendingMessage)
    }

    @Test
    fun whileARestoreIsRunning_itCantBeDismissedOrConfirmedAgain() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
        val gate = CompletableDeferred<Unit>()
        backups.gate = gate

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertTrue(viewModel.uiState.value.backup.isRestoring)
        assertEquals(expectedPreview, viewModel.uiState.value.backup.restorePreview)

        viewModel.onEvent(ExportBackupEvent.RestoreDismissed)
        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)
        assertEquals(expectedPreview, viewModel.uiState.value.backup.restorePreview)

        gate.complete(Unit)

        assertEquals(expectedBackup, backups.restored.single())
        assertEquals(ExportBackupMessage.Restored(3), viewModel.uiState.value.pendingMessage)
    }

    @Test
    fun afterProcessDeath_theSheetComesBackFromTheFile_andConfirmingRestoresIt() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = restoredAfter { it.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri)) }
        subscribe(viewModel.uiState)

        assertEquals(expectedPreview, viewModel.uiState.value.backup.restorePreview)

        viewModel.onEvent(ExportBackupEvent.RestoreConfirmed)

        assertEquals(expectedBackup, backups.restored.single())
        assertEquals(ExportBackupMessage.Restored(3), viewModel.uiState.value.pendingMessage)
    }

    @Test
    fun afterProcessDeath_aFileThatHasGone_isReportedAndForgotten() = runTest {
        backups.putFile(backupUri, backupDocument())
        val viewModel = restoredAfter {
            it.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
            backups.files.remove(backupUri)
        }
        subscribe(viewModel.uiState)

        assertNull(viewModel.uiState.value.backup.restorePreview)
        assertEquals(
            ExportBackupMessage.Failed(R.string.error_file_read_failed),
            viewModel.uiState.value.pendingMessage
        )
    }

    @Test
    fun afterProcessDeath_aRestoreThatWasDismissedOrDone_isNotOfferedAgain() = runTest {
        backups.putFile(backupUri, backupDocument())
        val dismissed = restoredAfter {
            it.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
            it.onEvent(ExportBackupEvent.RestoreDismissed)
        }
        val done = restoredAfter {
            it.onEvent(ExportBackupEvent.RestoreFilePicked(backupUri))
            it.onEvent(ExportBackupEvent.RestoreConfirmed)
        }
        subscribe(dismissed.uiState)
        subscribe(done.uiState)

        assertNull(dismissed.uiState.value.backup.restorePreview)
        assertNull(done.uiState.value.backup.restorePreview)
        assertEquals(1, backups.restored.size)
    }
}
