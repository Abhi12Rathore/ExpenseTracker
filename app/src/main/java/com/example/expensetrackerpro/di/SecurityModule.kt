package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.security.AndroidBiometricAvailability
import com.example.expensetrackerpro.domain.security.BiometricAvailability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindBiometricAvailability(impl: AndroidBiometricAvailability): BiometricAvailability
}
