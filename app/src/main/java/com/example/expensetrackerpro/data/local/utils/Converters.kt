package com.example.expensetrackerpro.data.local.utils

import androidx.room.TypeConverter
import com.example.expensetrackerpro.data.local.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}