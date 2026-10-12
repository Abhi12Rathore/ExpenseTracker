package com.example.expensetrackerpro.presentation.common.error

import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import org.junit.Assert.assertEquals
import org.junit.Test

class DomainErrorMessagesTest {

    @Test
    fun everyError_hasItsOwnMessage() {
        val expected = mapOf(
            DomainError.NotFound to R.string.error_category_not_found,
            DomainError.DuplicateName to R.string.category_editor_name_error_duplicate,
            DomainError.CategoryInUse to R.string.error_category_in_use,
            DomainError.InvalidInput(InvalidInputReason.NAME_BLANK) to R.string.category_editor_name_error_blank,
            DomainError.InvalidInput(InvalidInputReason.NAME_TOO_LONG) to R.string.error_name_too_long,
            DomainError.InvalidInput(InvalidInputReason.INCOMPATIBLE_REASSIGN_TARGET) to
                R.string.error_incompatible_reassign_target,
            DomainError.StorageFailure("database or disk is full") to R.string.error_storage_failure,
            DomainError.FileWriteFailed to R.string.error_file_write_failed
        )

        expected.forEach { (error, messageRes) -> assertEquals(messageRes, error.toMessageRes()) }
        // No two errors read the same
        assertEquals(expected.size, expected.values.toSet().size)
        assertEquals(InvalidInputReason.entries.size, expected.keys.count { it is DomainError.InvalidInput })
    }

    @Test
    fun aStorageFailure_readsTheSame_whateverItsCause() {
        assertEquals(
            DomainError.StorageFailure("disk I/O error").toMessageRes(),
            DomainError.StorageFailure().toMessageRes()
        )
    }
}
