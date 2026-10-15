package com.example.expensetrackerpro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.expensetrackerpro.domain.models.TransactionType

/**
 * A category the user picked for a title. Goes with its category, since it is only a cache of the
 * user's choices.
 */
@Entity(
    tableName = "category_memory",
    primaryKeys = ["normalizedTitle", "type"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class CategoryMemoryEntity(
    val normalizedTitle: String,
    val type: TransactionType,
    val categoryId: Int
)
