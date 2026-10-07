package com.example.expensetrackerpro.cleanArchPOC.di

import com.example.expensetrackerpro.cleanArchPOC.data.remote.ExpenseApi
import com.example.expensetrackerpro.cleanArchPOC.data.remote.FakeExpenseApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @Singleton
    abstract fun bindExpenseApi(
        impl: FakeExpenseApi
    ): ExpenseApi
}