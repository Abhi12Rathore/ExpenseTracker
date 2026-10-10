package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateSavingsRateUseCaseTest {

    private val calculateSavingsRate = CalculateSavingsRateUseCase()

    @Test
    fun returnsShareOfIncomeLeftOver() {
        assertEquals(75f, calculateSavingsRate(income = rupees(1000), expense = rupees(250)), 0.001f)
    }

    @Test
    fun noExpense_isHundredPercent() {
        assertEquals(100f, calculateSavingsRate(income = rupees(500), expense = Money.ZERO), 0.001f)
    }

    @Test
    fun expenseEqualToIncome_isZero() {
        assertEquals(0f, calculateSavingsRate(income = rupees(500), expense = rupees(500)), 0.001f)
    }

    @Test
    fun expenseAboveIncome_floorsAtZero() {
        assertEquals(0f, calculateSavingsRate(income = rupees(1000), expense = rupees(1250)), 0.001f)
    }

    @Test
    fun zeroIncome_isZero() {
        assertEquals(0f, calculateSavingsRate(income = Money.ZERO, expense = rupees(300)), 0.001f)
    }

    @Test
    fun noIncomeAndNoExpense_isZero() {
        assertEquals(0f, calculateSavingsRate(income = Money.ZERO, expense = Money.ZERO), 0.001f)
    }
}
