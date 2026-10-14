package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.currency.DefaultCurrencyProvider
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CurrencyModule {

    @Binds
    @Singleton
    abstract fun bindCurrencyProvider(impl: DefaultCurrencyProvider): CurrencyProvider
}
