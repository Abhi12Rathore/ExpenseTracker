package com.example.expensetrackerpro.di

import android.content.Context
import androidx.room.Room
import com.example.expensetrackerpro.data.local.dao.AppDataDao
import com.example.expensetrackerpro.data.local.dao.CategoryDao
import com.example.expensetrackerpro.data.local.dao.CategoryMemoryDao
import com.example.expensetrackerpro.data.local.dao.DashboardDao
import com.example.expensetrackerpro.data.local.dao.SampleDataDao
import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.local.expenseDatabase.ExpenseDatabaseCallback
import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
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
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        defaultCategoryNames: DefaultCategoryNames,
        appLocaleProvider: AppLocaleProvider
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "finance_pro_db"
        )
            .fallbackToDestructiveMigration()
            .addCallback(ExpenseDatabaseCallback(defaultCategoryNames, appLocaleProvider))
            .build()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideDashboardDao(database: AppDatabase): DashboardDao =
        database.dashboardDao()

    @Provides
    fun provideAppDataDao(database: AppDatabase): AppDataDao =
        database.appDataDao()

    @Provides
    fun provideSampleDataDao(database: AppDatabase): SampleDataDao =
        database.sampleDataDao()

    @Provides
    fun provideCategoryMemoryDao(database: AppDatabase): CategoryMemoryDao =
        database.categoryMemoryDao()
}
