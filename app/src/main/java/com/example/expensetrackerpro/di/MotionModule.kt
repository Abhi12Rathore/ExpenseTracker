package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.motion.AndroidMotionPreferences
import com.example.expensetrackerpro.domain.motion.MotionPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MotionModule {

    @Binds
    @Singleton
    abstract fun bindMotionPreferences(impl: AndroidMotionPreferences): MotionPreferences
}
