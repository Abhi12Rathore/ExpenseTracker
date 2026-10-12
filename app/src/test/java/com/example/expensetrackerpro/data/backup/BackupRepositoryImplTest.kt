package com.example.expensetrackerpro.data.backup

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.expensetrackerpro.data.files.DocumentFiles
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.getOrNull
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.backup.ReadBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.RestoreBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.ValidateBackupUseCase
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Backs up and restores a real database through streams standing in for the picked documents. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    private lateinit var database: AppDatabase
    private lateinit var repository: BackupRepositoryImpl

    private val document = DocumentUri("content://com.example.documents/document/backup.json")

    private val dao get() = database.appDataDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().addCallback(ExpenseDatabaseCallback()).build()
        repository = BackupRepositoryImpl(dao, DocumentFiles(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun documentWritesTo(stream: OutputStream) {
        shadowOf(context.contentResolver).registerOutputStream(Uri.parse(document.uri), stream)
    }

    private fun documentHolds(stream: InputStream) {
        shadowOf(context.contentResolver).registerInputStream(Uri.parse(document.uri), stream)
    }

    private fun documentHolds(text: String) = documentHolds(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)))

    /** A custom category and three transactions on top of the seeded categories. */
    private suspend fun fillDatabase() {
        val customId = database.categoryDao().insertCategory(
            CategoryEntity(name = "मिट्टी \"Pottery\"", type = TransactionType.EXPENSE, icon = CategoryIcon.PETS)
        ).toInt()
        val expense = dao.getAllCategories().first { !it.isCustom && it.type == TransactionType.EXPENSE }
        val income = dao.getAllCategories().first { it.type == TransactionType.INCOME }
        listOf(
            TransactionEntity(title = "Clay, 2 kg", amountPaise = 640_50, note = "line one\nline two", dateMillis = 300L, categoryId = customId, transactionType = TransactionType.EXPENSE),
            TransactionEntity(title = "Lunch", amountPaise = 250_00, note = "", dateMillis = 200L, categoryId = expense.id, transactionType = TransactionType.EXPENSE),
            TransactionEntity(title = "Salary", amountPaise = 85_000_00, note = "₹", dateMillis = 100L, categoryId = income.id, transactionType = TransactionType.INCOME)
        ).forEach { database.transactionDao().insertTransaction(it) }
    }

    private suspend fun writtenBackup(): ByteArray {
        val file = ByteArrayOutputStream()
        documentWritesTo(file)
        assertEquals(Outcome.Success(Unit), repository.writeBackup(document, createdAtMillis = 5_000L, appVersion = "1.0"))

        return file.toByteArray()
    }

    @Test
    fun writeBackup_writesEverythingStored_asJsonWithoutAByteOrderMark() = runTest {
        fillDatabase()

        val bytes = writtenBackup()

        assertEquals('{'.code.toByte(), bytes.first())
        val written = BackupJson.decodeFromString<BackupDto>(bytes.toString(Charsets.UTF_8))
        assertEquals(dao.snapshot().toBackupDto(createdAtMillis = 5_000L, appVersion = "1.0"), written)
        assertEquals(3, written.transactions.size)
    }

    @Test
    fun aBackup_restoredOverChangedData_bringsEverythingBackExactly() = runTest {
        fillDatabase()
        val before = dao.snapshot()
        val bytes = writtenBackup()

        // Since the backup: a transaction deleted, one added, a category added and one renamed
        database.transactionDao().deleteTransaction(before.transactions.first())
        database.transactionDao().insertTransaction(before.transactions.last().copy(id = 0, title = "Extra"))
        database.categoryDao().insertCategory(CategoryEntity(name = "Later", type = TransactionType.INCOME, icon = CategoryIcon.SAVINGS))
        database.categoryDao().update(before.categories.first().copy(name = "Renamed"))

        documentHolds(ByteArrayInputStream(bytes))
        val readBackup = ReadBackupUseCase(repository, ValidateBackupUseCase(ValidateCategoryNameUseCase()))
        assertEquals(Outcome.Success(3), RestoreBackupUseCase(readBackup, repository)(document))

        val after = dao.snapshot()
        assertEquals(before.categories, after.categories)
        assertEquals(before.transactions.map { it.copy(id = 0) }, after.transactions.map { it.copy(id = 0) })
    }

    @Test
    fun readBackup_givesTheFileAsItIs_forTheDomainToCheck() = runTest {
        fillDatabase()
        val bytes = writtenBackup()
        documentHolds(ByteArrayInputStream(bytes))

        val file = repository.readBackup(document).getOrNull()

        assertEquals(Backup.FORMAT, file?.document?.format)
        assertEquals(Backup.VERSION, file?.document?.version)
        assertEquals(5_000L, file?.document?.createdAtMillis)
        assertEquals(dao.getAllCategories().map { it.icon.key }, file?.document?.categories?.map { it.icon })
        assertEquals(listOf("Clay, 2 kg", "Lunch", "Salary"), file?.document?.transactions?.map { it.title })
    }

    @Test
    fun readBackup_acceptsAFileThatStartsWithAByteOrderMark() = runTest {
        val bytes = writtenBackup()
        documentHolds(ByteArrayInputStream(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + bytes))

        assertTrue(repository.readBackup(document) is Outcome.Success)
    }

    @Test
    fun writeBackup_toADriveThatIsFull_isAFileWriteFailure() = runTest {
        documentWritesTo(object : OutputStream() {
            override fun write(b: Int) = throw IOException("No space left on device")
        })

        assertEquals(
            Outcome.Failure(DomainError.FileWriteFailed),
            repository.writeBackup(document, createdAtMillis = 5_000L, appVersion = "1.0")
        )
    }

    @Test
    fun readBackup_ofAFileThatCantBeRead_isAFileReadFailure() = runTest {
        documentHolds(object : InputStream() {
            override fun read(): Int = throw IOException("Transport endpoint is not connected")
        })

        assertEquals(Outcome.Failure(DomainError.FileReadFailed), repository.readBackup(document))
    }

    @Test
    fun readBackup_ofACsvFile_isNotABackup() = runTest {
        documentHolds("\uFEFFDate,Type,Category,Title,Note,Amount\r\n2026-10-11,Expense,Food,Lunch,,100.00\r\n")

        assertEquals(Outcome.Failure(DomainError.BackupInvalid), repository.readBackup(document))
    }

    @Test
    fun readBackup_ofAnImage_isNotABackup() = runTest {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 0x49, 0x48, 0x44, 0x52) +
            ByteArray(4_096) { (it * 31).toByte() }
        documentHolds(ByteArrayInputStream(png))

        assertEquals(Outcome.Failure(DomainError.BackupInvalid), repository.readBackup(document))
    }

    @Test
    fun readBackup_ofOtherJson_isNotABackup() = runTest {
        listOf("{}", "[]", "{\"name\":\"package\",\"version\":\"2.0.0\"}", "{\"format\":\"${Backup.FORMAT}\",\"version\":1}")
            .forEach { text ->
                documentHolds(text)
                assertEquals(text, Outcome.Failure(DomainError.BackupInvalid), repository.readBackup(document))
            }
    }

    @Test
    fun readBackup_ofANewerVersion_isTooNew_howeverTheRestIsLaidOut() = runTest {
        documentHolds("{\"format\":\"${Backup.FORMAT}\",\"version\":${Backup.VERSION + 1},\"accounts\":[{\"id\":\"a\"}]}")

        assertEquals(Outcome.Failure(DomainError.BackupTooNew), repository.readBackup(document))
    }

    @Test
    fun readBackup_ofANewerVersionOfSomethingElse_isNotABackup() = runTest {
        documentHolds("{\"format\":\"notes-export\",\"version\":9,\"notes\":[]}")

        assertEquals(Outcome.Failure(DomainError.BackupInvalid), repository.readBackup(document))
    }

    @Test
    fun readBackup_ofAFileFarTooBigToBeABackup_isNotABackup_andIsNotReadToTheEnd() = runTest {
        // 26 MB of what could be the start of a JSON string
        val endless = object : InputStream() {
            var served = 0L
            val size = 26L * 1024 * 1024

            override fun read(): Int = if (served++ < size) 'a'.code else -1

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (served >= size) return -1
                b.fill('a'.code.toByte(), off, off + len)
                served += len
                return len
            }
        }
        documentHolds(endless)

        assertEquals(Outcome.Failure(DomainError.BackupInvalid), repository.readBackup(document))
        assertTrue(endless.served < endless.size)
    }

    @Test
    fun restore_thatTheDatabaseTurnsDown_isAStorageFailure_andLeavesTheDataAsItWas() = runTest {
        fillDatabase()
        val before = dao.snapshot()
        val validated = ValidateBackupUseCase(ValidateCategoryNameUseCase())(dao.snapshot().toBackupDto(1L, "1.0").toDocument()).getOrNull()
        // Not something validation lets through: a transaction under a category that is not restored
        val broken = validated?.let { backup ->
            backup.copy(transactions = backup.transactions + backup.transactions.first().copy(categoryId = CategoryId(9_999)))
        }

        val result = broken?.let { repository.restore(it) }

        assertTrue(result is Outcome.Failure && result.error is DomainError.StorageFailure)
        assertEquals(before, dao.snapshot())
        assertFalse(dao.getAllTransactions().isEmpty())
    }
}
