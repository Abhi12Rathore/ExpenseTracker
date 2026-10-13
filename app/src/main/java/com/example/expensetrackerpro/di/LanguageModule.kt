package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.category.ResourceDefaultCategoryNames
import com.example.expensetrackerpro.data.language.AndroidAppLocaleProvider
import com.example.expensetrackerpro.data.language.AndroidDeviceLanguages
import com.example.expensetrackerpro.data.language.AppLanguageRepositoryImpl
import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguageModule {

    @Binds
    @Singleton
    abstract fun bindAppLanguageRepository(impl: AppLanguageRepositoryImpl): AppLanguageRepository

    @Binds
    @Singleton
    abstract fun bindDeviceLanguages(impl: AndroidDeviceLanguages): DeviceLanguages

    @Binds
    @Singleton
    abstract fun bindAppLocaleProvider(impl: AndroidAppLocaleProvider): AppLocaleProvider

    @Binds
    @Singleton
    abstract fun bindDefaultCategoryNames(impl: ResourceDefaultCategoryNames): DefaultCategoryNames
}
