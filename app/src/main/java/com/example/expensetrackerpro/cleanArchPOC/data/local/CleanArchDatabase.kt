package com.example.expensetrackerpro.cleanArchPOC.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CleanArchDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
}