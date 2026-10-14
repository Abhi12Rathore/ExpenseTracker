package com.example.expensetrackerpro.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.expensetrackerpro.domain.models.TransactionType

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["dateMillis"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    /**
     * The amount in whole minor units: hundredths of the currency's unit. The column kept the name
     * it had when the app only knew rupees, so the schema didn't change.
     */
    val amountPaise: Long,
    val note: String,
    val dateMillis: Long,
    val categoryId: Int,
    val transactionType: TransactionType
)