package com.example.expensetrackerpro.presentation.common.error

import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason

/** What to tell the user when an operation fails with this error. */
@StringRes
fun DomainError.toMessageRes(): Int = when (this) {
    // Only a category is ever reported missing: the one being edited, or a transaction's
    DomainError.NotFound -> R.string.error_category_not_found
    DomainError.DuplicateName -> R.string.category_editor_name_error_duplicate
    DomainError.CategoryInUse -> R.string.error_category_in_use
    is DomainError.InvalidInput -> when (reason) {
        InvalidInputReason.NAME_BLANK -> R.string.category_editor_name_error_blank
        InvalidInputReason.NAME_TOO_LONG -> R.string.error_name_too_long
        InvalidInputReason.INCOMPATIBLE_REASSIGN_TARGET -> R.string.error_incompatible_reassign_target
    }
    is DomainError.StorageFailure -> R.string.error_storage_failure
}
