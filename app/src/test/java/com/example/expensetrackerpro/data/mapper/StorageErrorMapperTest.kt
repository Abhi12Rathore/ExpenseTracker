package com.example.expensetrackerpro.data.mapper

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class StorageErrorMapperTest {

    private lateinit var database: SQLiteDatabase

    @Before
    fun setUp() {
        database = SQLiteDatabase.create(null)
        database.execSQL("PRAGMA foreign_keys = ON")
        database.execSQL("CREATE TABLE parent (id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE)")
        database.execSQL(
            "CREATE TABLE child (id INTEGER PRIMARY KEY, parentId INTEGER NOT NULL " +
                "REFERENCES parent(id) ON DELETE RESTRICT)"
        )
        database.execSQL("INSERT INTO parent (id, name) VALUES (1, 'Food')")
        database.execSQL("INSERT INTO child (id, parentId) VALUES (1, 1)")
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** What SQLite itself throws for [sql], so the mapping is checked against its real wording. */
    private fun rejectionOf(sql: String): SQLiteException {
        try {
            database.execSQL(sql)
        } catch (e: SQLiteException) {
            return e
        }
        fail("SQLite accepted: $sql")
        error("unreachable")
    }

    // --- Constraints, as SQLite words them

    @Test
    fun deletingARowThatIsStillReferenced_isCategoryInUse() {
        val rejection = rejectionOf("DELETE FROM parent WHERE id = 1")

        assertEquals(DomainError.CategoryInUse, rejection.toDomainError())
    }

    @Test
    fun aBrokenForeignKey_isWhateverTheWriteSaysItMeans() {
        val rejection = rejectionOf("INSERT INTO child (id, parentId) VALUES (2, 99)")

        assertEquals(DomainError.NotFound, rejection.toDomainError(foreignKeyError = DomainError.NotFound))
        assertEquals(DomainError.CategoryInUse, rejection.toDomainError())
    }

    @Test
    fun aRepeatedUniqueValue_isDuplicateName() {
        val rejection = rejectionOf("INSERT INTO parent (id, name) VALUES (2, 'Food')")

        assertEquals(DomainError.DuplicateName, rejection.toDomainError())
        // What a foreign key means has no say in it
        assertEquals(DomainError.DuplicateName, rejection.toDomainError(foreignKeyError = DomainError.NotFound))
    }

    @Test
    fun anyOtherConstraint_isAStorageFailure() {
        val rejection = rejectionOf("INSERT INTO parent (id, name) VALUES (2, NULL)")

        assertEquals(DomainError.StorageFailure(rejection.message), rejection.toDomainError())
    }

    @Test
    fun aConstraintExceptionWithoutAMessage_isAStorageFailure() {
        assertEquals(DomainError.StorageFailure(null), SQLiteConstraintException().toDomainError())
    }

    // --- Everything else SQLite throws

    @Test
    fun aFullDisk_anIoError_andAnyOtherSqliteException_areStorageFailuresCarryingTheirMessage() {
        val exceptions = listOf(
            SQLiteFullException("database or disk is full (code 13 SQLITE_FULL)"),
            SQLiteDiskIOException("disk I/O error (code 1802 SQLITE_IOERR_FSTAT)"),
            SQLiteException("no such table: transactions")
        )

        exceptions.forEach { exception ->
            assertEquals(DomainError.StorageFailure(exception.message), exception.toDomainError())
        }
    }

    @Test
    fun aMessageThatOnlyMentionsAConstraint_doesNotMakeAnotherExceptionOne() {
        val exception = SQLiteException("FOREIGN KEY constraint failed")

        assertEquals(DomainError.StorageFailure(exception.message), exception.toDomainError())
    }

    // --- storageOutcome

    @Test
    fun storageOutcome_returnsTheBlocksValue() {
        assertEquals(Outcome.Success(3), storageOutcome { 3 })
    }

    @Test
    fun storageOutcome_turnsASqliteExceptionIntoAFailure() {
        val full = SQLiteFullException("database or disk is full")

        assertEquals(
            Outcome.Failure(DomainError.StorageFailure("database or disk is full")),
            storageOutcome<Unit> { throw full }
        )
        assertEquals(
            Outcome.Failure(DomainError.NotFound),
            storageOutcome<Unit>(foreignKeyError = DomainError.NotFound) {
                throw rejectionOf("INSERT INTO child (id, parentId) VALUES (2, 99)")
            }
        )
    }

    @Test
    fun storageOutcome_letsAnythingElseThrough() {
        assertThrows(IllegalStateException::class.java) {
            storageOutcome<Unit> { throw IllegalStateException("a bug") }
        }
    }
}
