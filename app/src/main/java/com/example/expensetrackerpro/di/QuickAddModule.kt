package com.example.expensetrackerpro.di

import com.example.expensetrackerpro.data.preferences.QuickAddPreferencesRepositoryImpl
import com.example.expensetrackerpro.data.repository.CategoryMemoryRepositoryImpl
import com.example.expensetrackerpro.data.speech.AndroidVoiceInputAvailability
import com.example.expensetrackerpro.domain.quickadd.QuickAddParser
import com.example.expensetrackerpro.domain.repository.CategoryMemoryRepository
import com.example.expensetrackerpro.domain.repository.QuickAddPreferencesRepository
import com.example.expensetrackerpro.domain.speech.VoiceInputAvailability
import com.example.expensetrackerpro.domain.usecase.quickadd.RuleBasedQuickAddParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class QuickAddModule {

    @Binds
    @Singleton
    abstract fun bindQuickAddParser(impl: RuleBasedQuickAddParser): QuickAddParser

    @Binds
    @Singleton
    abstract fun bindCategoryMemoryRepository(impl: CategoryMemoryRepositoryImpl): CategoryMemoryRepository

    @Binds
    @Singleton
    abstract fun bindQuickAddPreferencesRepository(
        impl: QuickAddPreferencesRepositoryImpl
    ): QuickAddPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindVoiceInputAvailability(impl: AndroidVoiceInputAvailability): VoiceInputAvailability
}
