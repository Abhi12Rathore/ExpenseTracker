package com.example.expensetrackerpro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: TransactionType, // This is the crucial filter
    val icon: CategoryIcon,
    val isCustom: Boolean = true,
    // Set while a built-in still has the app's name for it; cleared when the user renames it
    val builtInKey: BuiltInCategory? = null
)
