package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.BuildConfig
import com.example.expensetrackerpro.domain.models.BuildInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object BuildModule {

    @Provides
    fun provideBuildInfo(): BuildInfo = BuildInfo(isDebug = BuildConfig.DEBUG)
}
