package com.example.expensetrackerpro.data.local.entity

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: TransactionType, // This is the crucial filter
    val iconName: String,
    val isCustom: Boolean = true,
)
