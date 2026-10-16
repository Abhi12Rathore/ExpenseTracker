package com.example.expensetrackerpro.domain.error

/** Every way an operation is expected to fail. Anything else is a bug and is left to crash. */
sealed interface DomainError {

    /** What the operation works on, or something it refers to, is no longer stored. */
    data object NotFound : DomainError

    /** Another category of the same type already has the name. */
    data object DuplicateName : DomainError

    /** The category still has transactions, so it can't be deleted. */
    data object CategoryInUse : DomainError

    data class InvalidInput(val reason: InvalidInputReason) : DomainError

    /** The database could not carry out the write. [cause] is its own description, for logs only. */
    data class StorageFailure(val cause: String? = null) : DomainError

    /** A file could not be written where the user chose, e.g. the drive is full or was removed. */
    data object FileWriteFailed : DomainError

    /** A file the user chose could not be read, e.g. it was moved or its drive removed. */
    data object FileReadFailed : DomainError

    /** The file is not a backup of this app, or what it holds does not add up. */
    data object BackupInvalid : DomainError

    /** The backup was written by a newer version of the app, in a format this one can't read. */
    data object BackupTooNew : DomainError

    /** The photo could not be read, or nothing on it looks like a bill. */
    data object BillUnreadable : DomainError
}

enum class InvalidInputReason {
    NAME_BLANK,
    NAME_TOO_LONG,

    /** Transactions can only move to a different category of the same type. */
    INCOMPATIBLE_REASSIGN_TARGET,

    /** A transaction of several saved at once lacks an amount, a title or a category, or its note is too long. */
    TRANSACTION_INCOMPLETE
}
