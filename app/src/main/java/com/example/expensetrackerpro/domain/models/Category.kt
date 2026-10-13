package com.example.expensetrackerpro.domain.models

data class Category(
    val id: CategoryId = CategoryId.UNSAVED,
    val name: String,
    val type: TransactionType,
    val icon: CategoryIcon,
    val isCustom: Boolean = true,
    // Set while a built-in still carries the app's own name for it, which follows the app's
    // language. Null for a custom category and for a built-in the user has renamed.
    val builtInKey: BuiltInCategory? = null
) {
    /** A category the app came with, under a name the user gave it. */
    val isRenamedBuiltIn: Boolean get() = !isCustom && builtInKey == null
}
