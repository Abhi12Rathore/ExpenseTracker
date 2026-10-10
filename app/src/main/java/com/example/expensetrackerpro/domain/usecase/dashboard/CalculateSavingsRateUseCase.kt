package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.Money
import javax.inject.Inject

/** Share of income left after expenses, as a percentage between 0 and 100. */
class CalculateSavingsRateUseCase @Inject constructor() {

    operator fun invoke(income: Money, expense: Money): Float {
        if (!income.isPositive) return 0f
        return ((income - expense).ratioTo(income) * 100).coerceAtLeast(0f)
    }
}
