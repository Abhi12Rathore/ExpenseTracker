package com.example.expensetrackerpro.data.local.expenseDatabase


import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.expensetrackerpro.data.local.dao.AppDataDao
import com.example.expensetrackerpro.data.local.dao.CategoryDao
import com.example.expensetrackerpro.data.local.dao.CategoryMemoryDao
import com.example.expensetrackerpro.data.local.dao.DashboardDao
import com.example.expensetrackerpro.data.local.dao.SampleDataDao
import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.CategoryMemoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.data.local.utils.Converters


@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, CategoryMemoryEntity::class],
    // Pre-release: every schema change bumps this, and the destructive fallback resets the data
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    abstract fun dashboardDao(): DashboardDao

    abstract fun appDataDao(): AppDataDao

    abstract fun sampleDataDao(): SampleDataDao

    abstract fun categoryMemoryDao(): CategoryMemoryDao
}

internal class ExpenseDatabaseCallback(
    private val names: DefaultCategoryNames,
    private val localeProvider: AppLocaleProvider
) : RoomDatabase.Callback() {

    /**
     * onCreate is called only the first time the database is created.
     * The defaults are inserted here, as part of creation, so they exist before any query runs.
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedDefaultCategories(db)
    }

    // A schema change before release empties the database without calling onCreate, which would
    // leave the app with no categories at all. Room calls this from inside dropping the tables,
    // before they are made again, so the defaults go in once the database has opened
    @Volatile
    private var seedOnOpen = false

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        seedOnOpen = true
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        if (seedOnOpen) {
            seedOnOpen = false
            seedDefaultCategories(db)
        }
    }

    private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
        val converters = Converters()
        // In the app's language now; LocalizeDefaultCategoriesUseCase renames them when it changes
        val locale = localeProvider.current()
        DefaultCategories.all.forEach { category ->
            val values = ContentValues().apply {
                put("name", names.nameOf(category, locale))
                put("type", converters.fromTransactionType(category.type))
                put("icon", converters.fromCategoryIcon(category.icon))
                put("isCustom", false)
                put("builtInKey", converters.fromBuiltInCategory(category))
            }
            db.insert("categories", SQLiteDatabase.CONFLICT_REPLACE, values)
        }
    }
}
