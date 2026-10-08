package com.example.expensetrackerpro.data.local.expenseDatabase


import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.data.local.utils.Converters


@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 1, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
}

internal class ExpenseDatabaseCallback : RoomDatabase.Callback() {

    /**
     * onCreate is called only the first time the database is created.
     * The defaults are inserted here, as part of creation, so they exist before any query runs.
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        // Add your default categories
        val defaultCategories = listOf(
            CategoryEntity(name = "Food", iconName = "Food", isCustom = false, type = TransactionType.EXPENSE),
            CategoryEntity(name = "Shopping", iconName = "Shopping", isCustom = false,type = TransactionType.EXPENSE),
            CategoryEntity(name = "Transport", iconName = "Transport", isCustom = false,type = TransactionType.EXPENSE),
            CategoryEntity(name = "Bills", iconName = "Bills", isCustom = false,type = TransactionType.EXPENSE),
            CategoryEntity(name = "Home", iconName = "Home", isCustom = false,type = TransactionType.EXPENSE),

            // --- Default Income Categories ---
            CategoryEntity(name = "Salary", iconName = "Payments", isCustom = false, type = TransactionType.INCOME),
            CategoryEntity(name = "Freelance", iconName = "Work", isCustom = false, type = TransactionType.INCOME),
            CategoryEntity(name = "Investment", iconName = "TrendingUp", isCustom = false, type = TransactionType.INCOME),
            CategoryEntity(name = "Rent Income", iconName = "RealEstateAgent", isCustom = false, type = TransactionType.INCOME),
            CategoryEntity(name = "Interest", iconName = "AccountBalance", isCustom = false, type = TransactionType.INCOME),
            CategoryEntity(name = "Deposit", iconName = "Savings", isCustom = false, type = TransactionType.INCOME),
        )

        val converters = Converters()
        defaultCategories.forEach { category ->
            val values = ContentValues().apply {
                put("name", category.name)
                put("type", converters.fromTransactionType(category.type))
                put("iconName", category.iconName)
                put("isCustom", category.isCustom)
            }
            db.insert("categories", SQLiteDatabase.CONFLICT_REPLACE, values)
        }
    }
}
