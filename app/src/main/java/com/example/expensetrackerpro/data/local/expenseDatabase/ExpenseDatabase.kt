package com.example.expensetrackerpro.data.local.expenseDatabase


import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.expensetrackerpro.data.local.dao.SampleDataDao
import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.data.local.utils.Converters


@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    // Pre-release: stays at 1, and a schema change means clearing the app's data
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun sampleDataDao(): SampleDataDao
}

internal class ExpenseDatabaseCallback : RoomDatabase.Callback() {

    /**
     * onCreate is called only the first time the database is created.
     * The defaults are inserted here, as part of creation, so they exist before any query runs.
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        val converters = Converters()
        DefaultCategories.all.forEach { category ->
            val values = ContentValues().apply {
                put("name", category.name)
                put("type", converters.fromTransactionType(category.type))
                put("icon", converters.fromCategoryIcon(category.icon))
                put("isCustom", category.isCustom)
            }
            db.insert("categories", SQLiteDatabase.CONFLICT_REPLACE, values)
        }
    }
}
