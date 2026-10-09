package com.example.expensetrackerpro.domain.usecase.applock

import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

class GetCurrentTimeMillisUseCase @Inject constructor(
    private val timeProvider: TimeProvider
) {

    operator fun invoke(): Long = timeProvider.now().timeInMillis
}
