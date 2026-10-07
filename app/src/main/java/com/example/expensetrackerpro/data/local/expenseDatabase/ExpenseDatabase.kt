package com.example.expensetrackerpro.data.local.expenseDatabase


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.data.local.entity.Transaction
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.data.local.utils.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Database(
    entities = [Transaction::class, Category::class],
    version = 1, // Increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context,scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_pro_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(ExpenseDatabaseCallback(scope))// Wipes old data for a clean start
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }


    // 3. Create the Callback class inside or outside the companion object
    private class ExpenseDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        /**
         * onCreate is called only the first time the database is created.
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.transactionDao()

                    // Add your default categories
                    val defaultCategories = listOf(
                        Category(name = "Food", iconName = "Food", isCustom = false, type = TransactionType.EXPENSE),
                        Category(name = "Shopping", iconName = "Shopping", isCustom = false,type = TransactionType.EXPENSE),
                        Category(name = "Transport", iconName = "Transport", isCustom = false,type = TransactionType.EXPENSE),
                        Category(name = "Bills", iconName = "Bills", isCustom = false,type = TransactionType.EXPENSE),
                        Category(name = "Home", iconName = "Home", isCustom = false,type = TransactionType.EXPENSE),

                        // --- Default Income Categories ---
                        Category(name = "Salary", iconName = "Payments", isCustom = false, type = TransactionType.INCOME),
                        Category(name = "Freelance", iconName = "Work", isCustom = false, type = TransactionType.INCOME),
                        Category(name = "Investment", iconName = "TrendingUp", isCustom = false, type = TransactionType.INCOME),
                        Category(name = "Rent Income", iconName = "RealEstateAgent", isCustom = false, type = TransactionType.INCOME),
                        Category(name = "Interest", iconName = "AccountBalance", isCustom = false, type = TransactionType.INCOME),
                        Category(name = "Deposit", iconName = "Savings", isCustom = false, type = TransactionType.INCOME),


                        )

                    dao.insertAllCategories(defaultCategories)
                }
            }
        }
    }
}