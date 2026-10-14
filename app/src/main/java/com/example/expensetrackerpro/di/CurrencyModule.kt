package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.currency.AndroidNumberSymbolsProvider
import com.example.expensetrackerpro.data.currency.AndroidRegionCurrency
import com.example.expensetrackerpro.data.currency.ResourceCurrencyNames
import com.example.expensetrackerpro.data.preferences.CurrencyRepositoryImpl
import com.example.expensetrackerpro.domain.currency.CurrencyNames
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.currency.RegionCurrency
import com.example.expensetrackerpro.domain.locale.NumberSymbolsProvider
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CurrencyModule {

    // One instance behind both: the provider answers with what the repository has read
    @Binds
    @Singleton
    abstract fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository

    @Binds
    @Singleton
    abstract fun bindCurrencyProvider(impl: CurrencyRepositoryImpl): CurrencyProvider

    @Binds
    @Singleton
    abstract fun bindRegionCurrency(impl: AndroidRegionCurrency): RegionCurrency

    @Binds
    @Singleton
    abstract fun bindNumberSymbolsProvider(impl: AndroidNumberSymbolsProvider): NumberSymbolsProvider

    @Binds
    @Singleton
    abstract fun bindCurrencyNames(impl: ResourceCurrencyNames): CurrencyNames
}
