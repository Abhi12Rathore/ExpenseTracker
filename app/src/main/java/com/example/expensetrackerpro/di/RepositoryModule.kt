package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.preferences.AppLockRepositoryImpl
import com.example.expensetrackerpro.data.preferences.ThemePreferenceRepositoryImpl
import com.example.expensetrackerpro.data.repository.CategoryRepositoryImpl
import com.example.expensetrackerpro.data.repository.DashboardRepositoryImpl
import com.example.expensetrackerpro.data.repository.TransactionRepositoryImpl
import com.example.expensetrackerpro.domain.repository.AppLockRepository
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import com.example.expensetrackerpro.domain.repository.ThemePreferenceRepository
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import com.example.expensetrackerpro.data.sample.SampleDataSeeder
import com.example.expensetrackerpro.domain.repository.SampleDataRepository
import com.example.expensetrackerpro.data.preferences.UserProfileRepositoryImpl
import com.example.expensetrackerpro.data.repository.AppDataRepositoryImpl
import com.example.expensetrackerpro.data.backup.BackupRepositoryImpl
import com.example.expensetrackerpro.data.export.FileExportRepositoryImpl
import com.example.expensetrackerpro.data.preferences.BackupInfoRepositoryImpl
import com.example.expensetrackerpro.domain.repository.BackupInfoRepository
import com.example.expensetrackerpro.domain.repository.BackupRepository
import com.example.expensetrackerpro.domain.repository.FileExportRepository
import com.example.expensetrackerpro.domain.repository.AppDataRepository
import com.example.expensetrackerpro.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindThemePreferenceRepository(
        impl: ThemePreferenceRepositoryImpl
    ): ThemePreferenceRepository

    @Binds
    @Singleton
    abstract fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository

    @Binds
    @Singleton
    abstract fun bindSampleDataRepository(impl: SampleDataSeeder): SampleDataRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindAppDataRepository(impl: AppDataRepositoryImpl): AppDataRepository

    @Binds
    @Singleton
    abstract fun bindFileExportRepository(impl: FileExportRepositoryImpl): FileExportRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindBackupInfoRepository(impl: BackupInfoRepositoryImpl): BackupInfoRepository
}
