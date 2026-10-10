package com.example.expensetrackerpro.domain.models

/** A category as the editor hands it over. [id] is null for one that is not stored yet. */
data class CategoryDraft(
    val id: Int? = null,
    val name: String,
    val type: TransactionType,
    val icon: CategoryIcon
)

enum class CategoryNameValidation { VALID, BLANK, TOO_LONG, DUPLICATE }
