package com.example.expensetrackerpro.domain.usecase.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateSavingsRateUseCaseTest {

    private val calculateSavingsRate = CalculateSavingsRateUseCase()

    @Test
    fun returnsShareOfIncomeLeftOver() {
        assertEquals(75f, calculateSavingsRate(income = 1000.0, expense = 250.0), 0.001f)
    }

    @Test
    fun noExpense_isHundredPercent() {
        assertEquals(100f, calculateSavingsRate(income = 500.0, expense = 0.0), 0.001f)
    }

    @Test
    fun expenseEqualToIncome_isZero() {
        assertEquals(0f, calculateSavingsRate(income = 500.0, expense = 500.0), 0.001f)
    }

    @Test
    fun expenseAboveIncome_floorsAtZero() {
        assertEquals(0f, calculateSavingsRate(income = 1000.0, expense = 1250.0), 0.001f)
    }

    @Test
    fun zeroIncome_isZero() {
        assertEquals(0f, calculateSavingsRate(income = 0.0, expense = 300.0), 0.001f)
    }

    @Test
    fun noIncomeAndNoExpense_isZero() {
        assertEquals(0f, calculateSavingsRate(income = 0.0, expense = 0.0), 0.001f)
    }
}
