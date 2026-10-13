package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.time.SystemHourFormatProvider
import com.example.expensetrackerpro.data.time.SystemTimeProvider
import com.example.expensetrackerpro.domain.time.HourFormatProvider
import com.example.expensetrackerpro.domain.time.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    @Binds
    @Singleton
    abstract fun bindHourFormatProvider(impl: SystemHourFormatProvider): HourFormatProvider
}
