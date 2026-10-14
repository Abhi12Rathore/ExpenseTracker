package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.RestoredBackup
import com.example.expensetrackerpro.domain.usecase.currency.SetCurrencyUseCase
import com.example.expensetrackerpro.fakes.FakeCurrencyRepository
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.localizeDefaultCategories
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.getOrNull
import com.example.expensetrackerpro.domain.models.BackupPreview
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.fakes.FakeBackupInfoRepository
import com.example.expensetrackerpro.fakes.FakeBackupRepository
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.backupDocument
import com.example.expensetrackerpro.fakes.millisAt
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.fakes.transactionRecord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BackupUseCasesTest {

    private val now = millisAt(2026, Calendar.OCTOBER, 11, hour = 16, minute = 20)
    private val time = FakeTimeProvider(now)

    private val backups = FakeBackupRepository()
    private val categories = FakeCategoryRepository()
    private val backupInfo = FakeBackupInfoRepository()
    private val transactions = FakeTransactionRepository(
        listOf(transaction(id = 1), transaction(id = 2), transaction(id = 3), transaction(id = 4))
    )

    private val file = DocumentUri("content://documents/backup.json")

    private val currency = FakeCurrencyRepository()
    private val createBackup =
        CreateBackupUseCase(backups, backupInfo, time, BuildInfo(isDebug = false, versionName = "1.4"), currency)
    private val readBackup = ReadBackupUseCase(backups, ValidateBackupUseCase(ValidateCategoryNameUseCase()))
    private val previewBackup = PreviewBackupUseCase(readBackup, GetTransactionsUseCase(transactions), currency)
    private val restoreBackup =
        RestoreBackupUseCase(readBackup, backups, localizeDefaultCategories(categories), currency, SetCurrencyUseCase(currency))

    @Test
    fun createBackup_writesItStampedWithNowAndTheAppVersion_andRemembersWhen() = runTest {
        assertEquals(Outcome.Success(Unit), createBackup(file))

        assertEquals(listOf(FakeBackupRepository.WrittenBackup(file, now, "1.4")), backups.written)
        assertEquals(now, backupInfo.current)
    }

    @Test
    fun createBackup_thatFails_isNotRememberedAsABackup() = runTest {
        backups.failWrites = true

        assertEquals(Outcome.Failure(DomainError.FileWriteFailed), createBackup(file))
        assertNull(backupInfo.current)
    }

    @Test
    fun previewBackup_saysWhatTheFileHolds_andWhatItWouldReplace() = runTest {
        backups.putFile(file.uri, backupDocument(), name = "my-backup.json")

        assertEquals(
            Outcome.Success(
                BackupPreview(
                    fileName = "my-backup.json",
                    createdAtMillis = 1_000L,
                    transactionCount = 3,
                    customCategoryCount = 1,
                    oldestMillis = 100L,
                    newestMillis = 300L,
                    currentTransactionCount = 4
                )
            ),
            previewBackup(file)
        )
        assertTrue(backups.restored.isEmpty())
    }

    @Test
    fun previewBackup_ofAFileWithoutTransactions_coversNoPeriod() = runTest {
        backups.putFile(file.uri, backupDocument(transactions = emptyList()))

        val preview = previewBackup(file).getOrNull()

        assertEquals(0, preview?.transactionCount)
        assertNull(preview?.oldestMillis)
        assertNull(preview?.newestMillis)
    }

    @Test
    fun previewBackup_passesOnWhyTheFileCantBeUsed() = runTest {
        assertEquals(Outcome.Failure(DomainError.FileReadFailed), previewBackup(file))

        backups.files[file.uri] = Outcome.Failure(DomainError.BackupTooNew)
        assertEquals(Outcome.Failure(DomainError.BackupTooNew), previewBackup(file))

        backups.putFile(file.uri, backupDocument(format = "something-else"))
        assertEquals(Outcome.Failure(DomainError.BackupInvalid), previewBackup(file))
    }

    @Test
    fun restoreBackup_readsTheFileAgain_andRestoresWhatItHolds() = runTest {
        backups.putFile(file.uri, backupDocument())

        assertEquals(Outcome.Success(RestoredBackup(3)), restoreBackup(file, switchCurrency = true))

        assertEquals(1, backups.readCalls)
        assertEquals(ValidateBackupUseCase(ValidateCategoryNameUseCase())(backupDocument()).getOrNull(), backups.restored.single())
    }

    @Test
    fun restoreBackup_ofAFileThatIsNoLongerValid_restoresNothing() = runTest {
        backups.putFile(file.uri, backupDocument(transactions = listOf(transactionRecord(categoryId = 99))))

        assertEquals(Outcome.Failure(DomainError.BackupInvalid), restoreBackup(file, switchCurrency = true))
        assertTrue(backups.restored.isEmpty())
    }

    @Test
    fun restoreBackup_ofAFileThatCantBeRead_restoresNothing() = runTest {
        assertEquals(Outcome.Failure(DomainError.FileReadFailed), restoreBackup(file, switchCurrency = true))
        assertTrue(backups.restored.isEmpty())
    }

    @Test
    fun restoreBackup_passesOnAStorageFailure() = runTest {
        backups.putFile(file.uri, backupDocument())
        backups.failRestores = true

        assertEquals(Outcome.Failure(DomainError.StorageFailure()), restoreBackup(file, switchCurrency = true))
    }

    @Test
    fun createBackup_notesTheCurrencyAmountsAreShownIn() = runTest {
        currency.setCurrency(AppCurrency.EUR)

        createBackup(file)

        assertEquals(AppCurrency.EUR, backups.written.single().currency)
    }

    @Test
    fun previewBackup_saysWhenTheBackupWasMadeInAnotherCurrency() = runTest {
        backups.putFile(file.uri, backupDocument(currency = "USD"))
        assertEquals(AppCurrency.USD, previewBackup(file).getOrNull()?.differentCurrency)
        assertEquals(AppCurrency.INR, previewBackup(file).getOrNull()?.currentCurrency)

        // The same currency, none at all, or one this app doesn't offer: nothing to say
        for (code in listOf("INR", null, "KWD")) {
            backups.putFile(file.uri, backupDocument(currency = code))
            assertNull("for $code", previewBackup(file).getOrNull()?.differentCurrency)
        }
    }

    @Test
    fun restoreBackup_inAnotherCurrency_switchesToIt_whenAsked() = runTest {
        backups.putFile(file.uri, backupDocument(currency = "USD"))

        assertEquals(Outcome.Success(RestoredBackup(3, switchedTo = AppCurrency.USD)), restoreBackup(file, switchCurrency = true))
        assertEquals(AppCurrency.USD, currency.currency.value)
    }

    @Test
    fun restoreBackup_inAnotherCurrency_keepsTheCurrentOne_whenNotAsked() = runTest {
        backups.putFile(file.uri, backupDocument(currency = "USD"))

        assertEquals(Outcome.Success(RestoredBackup(3)), restoreBackup(file, switchCurrency = false))
        assertEquals(AppCurrency.INR, currency.currency.value)
    }

    @Test
    fun restoreBackup_whoseCurrencyCantBeStored_stillRestores_withoutSwitching() = runTest {
        backups.putFile(file.uri, backupDocument(currency = "USD"))
        currency.failWrites = true

        assertEquals(Outcome.Success(RestoredBackup(3)), restoreBackup(file, switchCurrency = true))
        assertEquals(AppCurrency.INR, currency.currency.value)
    }

    @Test
    fun restoreBackup_thatFails_neverSwitchesTheCurrency() = runTest {
        backups.putFile(file.uri, backupDocument(currency = "USD"))
        backups.failRestores = true

        restoreBackup(file, switchCurrency = true)

        assertEquals(AppCurrency.INR, currency.currency.value)
    }

    @Test
    fun restoreBackup_ofAnOlderFileWithoutACurrency_keepsTheCurrentOne() = runTest {
        backups.putFile(file.uri, backupDocument(currency = null))

        assertEquals(Outcome.Success(RestoredBackup(3)), restoreBackup(file, switchCurrency = true))
        assertEquals(AppCurrency.INR, currency.currency.value)
    }
}
