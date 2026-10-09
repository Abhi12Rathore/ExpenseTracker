package com.example.expensetrackerpro.data.local.utils

import androidx.room.TypeConverter
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    // Stored by key, so entries can be renamed or reordered without touching saved data
    @TypeConverter
    fun fromCategoryIcon(value: CategoryIcon): String = value.key

    @TypeConverter
    fun toCategoryIcon(value: String): CategoryIcon = CategoryIcon.fromKey(value)
}