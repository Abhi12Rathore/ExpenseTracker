package com.example.expensetrackerpro.domain.repository

import kotlinx.coroutines.flow.Flow

interface QuickAddPreferencesRepository {

    /** True once the user has filled the form from quick add, after which the examples are no longer shown. */
    fun hasUsedQuickAdd(): Flow<Boolean>

    suspend fun setUsedQuickAdd()
}
