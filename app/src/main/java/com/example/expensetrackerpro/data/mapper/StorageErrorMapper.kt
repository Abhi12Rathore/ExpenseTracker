package com.example.expensetrackerpro.data.mapper

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome

/**
 * Runs a database write, turning a rejection by SQLite into the [DomainError] it stands for.
 * Anything else that is thrown is a bug and carries on up.
 *
 * [foreignKeyError] is what a broken foreign key means for this write: deleting a category
 * breaks one when transactions still use it, which is the default; writing a transaction
 * breaks one when its category is gone.
 */
inline fun <T> storageOutcome(
    foreignKeyError: DomainError = DomainError.CategoryInUse,
    block: () -> T
): Outcome<T> =
    try {
        Outcome.Success(block())
    } catch (e: SQLiteException) {
        Outcome.Failure(e.toDomainError(foreignKeyError))
    }

fun SQLiteException.toDomainError(
    foreignKeyError: DomainError = DomainError.CategoryInUse
): DomainError {
    // SQLite only says which constraint failed in the message
    val description = message.orEmpty()

    return when {
        this !is SQLiteConstraintException -> DomainError.StorageFailure(message)
        description.contains("FOREIGN KEY", ignoreCase = true) -> foreignKeyError
        description.contains("UNIQUE", ignoreCase = true) -> DomainError.DuplicateName
        // NOT NULL, CHECK and the like are not something the user can put right
        else -> DomainError.StorageFailure(message)
    }
}
