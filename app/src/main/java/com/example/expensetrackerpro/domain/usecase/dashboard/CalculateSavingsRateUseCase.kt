package com.example.expensetrackerpro.domain.usecase.dashboard

import javax.inject.Inject

/** Share of income left after expenses, as a percentage between 0 and 100. */
class CalculateSavingsRateUseCase @Inject constructor() {

    operator fun invoke(income: Double, expense: Double): Float {
        if (income <= 0.0) return 0f
        return ((income - expense) / income * 100).coerceAtLeast(0.0).toFloat()
    }
}
