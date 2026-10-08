package com.example.expensetrackerpro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.expensetrackerpro.domain.models.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: TransactionType, // This is the crucial filter
    val iconName: String,
    val isCustom: Boolean = true,
)
