package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.fakes.sanitizeAmountInput
import com.example.expensetrackerpro.domain.models.AmountValidation
import com.example.expensetrackerpro.domain.models.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatAmountInputUseCaseTest {

    private val format = FormatAmountInputUseCase()

    @Test
    fun wholeAmount_hasNoDecimalPoint() {
        assertEquals("4201", format(Money.ofRupees(4201)))
        assertEquals("100", format(Money.ofRupees(100)))
        assertEquals("10000000", format(Money.ofRupees(10_000_000)))
    }

    @Test
    fun decimals_areShownWithoutTrailingZeros() {
        assertEquals("4201.5", format(Money(4201_50)))
        assertEquals("4201.25", format(Money(4201_25)))
        assertEquals("0.1", format(Money(10)))
        assertEquals("0.01", format(Money(1)))
    }

    @Test
    fun whatItShows_isAValidAmount() {
        val sanitize = sanitizeAmountInput()
        val validate = ValidateAmountUseCase()

        listOf(Money.ofRupees(4201), Money(4201_50), Money(1), Money.ofRupees(10_000_000)).forEach { amount ->
            assertEquals(format(amount), sanitize("", format(amount)))
            assertEquals(AmountValidation.Valid(amount), validate(format(amount)))
        }
    }
}
