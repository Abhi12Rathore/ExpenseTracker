package com.example.expensetrackerpro.domain.models

/** The categories every new database starts with, in the order they are added. */
object DefaultCategories {

    val all: List<BuiltInCategory> = BuiltInCategory.entries

    val expense: List<BuiltInCategory> = all.filter { it.type == TransactionType.EXPENSE }

    val income: List<BuiltInCategory> = all.filter { it.type == TransactionType.INCOME }
}
