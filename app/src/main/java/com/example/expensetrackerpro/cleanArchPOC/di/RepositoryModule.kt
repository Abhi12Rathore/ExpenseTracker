package com.example.expensetrackerpro.cleanArchPOC.di

import com.example.expensetrackerpro.cleanArchPOC.data.repository.ExpenseRepositoryImpl
import com.example.expensetrackerpro.cleanArchPOC.domain.repository.ExpenseRepository
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
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository
}