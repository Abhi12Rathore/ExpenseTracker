package com.example.expensetrackerpro.cleanArchPOC.di

import android.content.Context
import androidx.room.Room
import com.example.expensetrackerpro.cleanArchPOC.data.local.CleanArchDatabase
import com.example.expensetrackerpro.cleanArchPOC.data.local.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCleanArchDatabase(
        @ApplicationContext context: Context
    ): CleanArchDatabase {
        return Room.databaseBuilder(
            context,
            CleanArchDatabase::class.java,
            "clean_arch_poc.db"
        ).build()
    }

    @Provides
    fun provideExpenseDao(
        database: CleanArchDatabase
    ): ExpenseDao {
        return database.expenseDao()
    }
}