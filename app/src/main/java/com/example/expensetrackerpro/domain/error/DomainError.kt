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
}

enum class InvalidInputReason {
    NAME_BLANK,
    NAME_TOO_LONG,

    /** Transactions can only move to a different category of the same type. */
    INCOMPATIBLE_REASSIGN_TARGET
}
