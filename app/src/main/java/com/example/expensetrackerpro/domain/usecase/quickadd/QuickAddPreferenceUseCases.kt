package com.example.expensetrackerpro.domain.usecase.quickadd

import com.example.expensetrackerpro.domain.repository.QuickAddPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether quick add has been used. The example sentences under its bar are shown until it has. */
class ObserveQuickAddUsedUseCase @Inject constructor(
    private val repository: QuickAddPreferencesRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.hasUsedQuickAdd()
}

class MarkQuickAddUsedUseCase @Inject constructor(
    private val repository: QuickAddPreferencesRepository
) {
    suspend operator fun invoke() = repository.setUsedQuickAdd()
}
